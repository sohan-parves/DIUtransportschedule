package com.sohan.diutransportschedule

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sohan.diutransportschedule.ui.HomeViewModel
import com.sohan.diutransportschedule.ui.MainNav
import com.sohan.diutransportschedule.ui.theme.DIUTransportScheduleTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sohan.diutransportschedule.App
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        const val NOTIF_CHANNEL_ID = "diu_schedule"
        const val NOTIF_CHANNEL_NAME = "DIU Transport Alerts"
        const val NOTIF_CHANNEL_DESC = "Bus schedule and reminder notifications"

        // Used by AlarmManager PendingIntent requestCode + notification id
        const val ALARM_REQ_CODE = 9001

        // Intent extras used by ScheduleAlarmReceiver
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TEXT = "extra_text"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Always create notification channel early (required for Android 8+)
        ensureNotificationChannel(this, NOTIF_CHANNEL_ID, NOTIF_CHANNEL_NAME, NOTIF_CHANNEL_DESC)

        val app = application as App

        setContent {
            val vm: HomeViewModel = viewModel(factory = HomeVmFactory(app))
            val notificationsEnabled by vm.notificationsEnabled.collectAsState()
            val notifyLeadMinutes by vm.notifyLeadMinutes.collectAsState()
            val selectedRoute by vm.selectedRoute.collectAsState()

            // ✅ FIRST TIME ENTER = permission ask
            RequestStartupPermissions()

            val dark by vm.darkMode.collectAsState()

            DIUTransportScheduleTheme(darkTheme = dark) {
                val items by vm.items.collectAsState()
                val syncing by vm.isSyncing.collectAsState()

                // Always render the app, but blur + show full-screen overlay while syncing
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (syncing) Modifier.blur(16.dp) else Modifier)
                    ) {
                        // Keep app running underneath so user sees it's loading
                        MainNav(vm)
                    }

                    if (syncing) {
                        FullScreenLoading(
                            title = "DIU Transport Schedule",
                            subtitle = if (items.isEmpty()) "Loading schedule…" else "Syncing…",
                            logoResId = com.sohan.diutransportschedule.R.drawable.diu_logo
                        )
                    }
                }
                val ctx = LocalContext.current

                LaunchedEffect(notificationsEnabled, notifyLeadMinutes, selectedRoute, items) {
                    try {
                        if (!notificationsEnabled) {
                            cancelNextAlarm(ctx)
                        } else {
                            scheduleNextAlarmFromData(
                                context = ctx,
                                selectedRoute = selectedRoute,
                                leadMinutes = notifyLeadMinutes,
                                items = items
                            )
                        }
                    } catch (t: Throwable) {
                        Log.e("RouteNotificationScheduler", "Failed to schedule notifications", t)
                    }
                }
            }
        }
    }
}
@Composable
private fun FullScreenLoading(
    title: String,
    subtitle: String,
    logoResId: Int
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.78f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val logoPainter = painterResource(id = logoResId)

            Image(
                painter = logoPainter,
                contentDescription = "Logo",
                modifier = Modifier.size(160.dp) // ✅ logo একটু বড়
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(0.65f), // ✅ opacity কম
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Please wait…",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.alpha(0.70f),
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
private fun RequestStartupPermissions() {
    val ctx = LocalContext.current

    // ✅ Ask only once (first app entry), but do it step-by-step
    val prefs = remember {
        ctx.getSharedPreferences("startup_permissions", Context.MODE_PRIVATE)
    }

    // asked=true মানে startup flow একবার complete হয়েছে
    var alreadyAsked by remember { mutableStateOf(prefs.getBoolean("asked", false)) }

    // step: 0 = start, 1 = notif done, 2 = exact-alarm done, 3 = miui done
    var step by remember { mutableStateOf(prefs.getInt("step", 0)) }

    // ✅ Notification permission (Android 13+)
    fun hasNotifPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // ✅ Exact alarm (Android 12+)
    val alarmManager = remember {
        ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    val needsExactAlarm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun canExactAlarm(): Boolean {
        return !needsExactAlarm || alarmManager.canScheduleExactAlarms()
    }

    fun isMiui(): Boolean = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

    fun openMiuiBatterySettings(context: Context) {
        try {
            val intent = Intent().apply {
                component = android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    // local states
    var notifGranted by remember { mutableStateOf(hasNotifPermission()) }
    var exactAlarmGranted by remember { mutableStateOf(canExactAlarm()) }

    // Re-check permissions when user returns from Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifGranted = hasNotifPermission()
                exactAlarmGranted = canExactAlarm()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        notifGranted = granted
        if (granted) {
            step = maxOf(step, 1)
            prefs.edit().putInt("step", step).apply()
        }
    }

    var showExactAlarmDialog by remember { mutableStateOf(false) }
    var showMiuiDialog by remember { mutableStateOf(false) }

    // ✅ Startup flow
    LaunchedEffect(alreadyAsked, step, notifGranted, exactAlarmGranted) {
        if (alreadyAsked) return@LaunchedEffect

        // STEP 0: Notifications (Android 13+)
        if (step < 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@LaunchedEffect
            }
            // notif not needed / already granted
            step = 1
            prefs.edit().putInt("step", step).apply()
        }

        // STEP 1: Exact Alarm (Android 12+)
        if (step < 2) {
            if (needsExactAlarm && !exactAlarmGranted) {
                showExactAlarmDialog = true
                return@LaunchedEffect
            }
            // exact alarm not needed / already granted
            step = 2
            prefs.edit().putInt("step", step).apply()
        }

        // STEP 2: MIUI battery/autostart guidance (Xiaomi)
        if (step < 3) {
            if (isMiui()) {
                showMiuiDialog = true
                return@LaunchedEffect
            }
            step = 3
            prefs.edit().putInt("step", step).apply()
        }

        // ✅ Finished the startup permission flow
        prefs.edit().putBoolean("asked", true).apply()
        alreadyAsked = true

        // Test notification dialog disabled (user requested)
    }

    if (showExactAlarmDialog && !alreadyAsked) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text("Allow exact alarms") },
            text = {
                Text(
                    "To show bus time notifications reliably, please allow Exact Alarms. " +
                        "This helps the app trigger notifications at the correct time."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExactAlarmDialog = false
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                            }
                        } catch (_: Throwable) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", ctx.packageName, null)
                            }
                            ctx.startActivity(intent)
                        }
                        // Step will advance automatically on resume if permission becomes granted
                    }
                ) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExactAlarmDialog = false
                        // user skipped -> advance step anyway
                        step = 2
                        prefs.edit().putInt("step", step).apply()
                    }
                ) { Text("Not now") }
            }
        )
    }

    if (showMiuiDialog && !alreadyAsked) {
        AlertDialog(
            onDismissRequest = { showMiuiDialog = false },
            title = { Text("Allow background running") },
            text = {
                Text(
                    "To receive bus notifications on Xiaomi devices, please allow:\n\n" +
                        "• Autostart\n" +
                        "• No battery restrictions"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMiuiDialog = false
                        openMiuiBatterySettings(ctx)
                        // user went to settings -> mark step done so we don't block the app
                        step = 3
                        prefs.edit().putInt("step", step).apply()
                    }
                ) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMiuiDialog = false
                        step = 3
                        prefs.edit().putInt("step", step).apply()
                    }
                ) { Text("Not now") }
            }
        )
    }

    // Test notification dialog permanently disabled
}

private class HomeVmFactory(
    private val app: App
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(app.repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

// --- Notification helpers ---
private fun ensureNotificationChannel(
    context: Context,
    channelId: String,
    channelName: String,
    channelDesc: String
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val existing = nm.getNotificationChannel(channelId)
    if (existing != null) return

    val channel = NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = channelDesc
        enableVibration(true)
        setShowBadge(true)
    }
    nm.createNotificationChannel(channel)
}

private fun sendTestNotification(
    context: Context,
    channelId: String,
    channelName: String,
    channelDesc: String
) {
    ensureNotificationChannel(context, channelId, channelName, channelDesc)

    // If user disabled notifications for the app, we can't show anything.
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        return
    }

    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pending = PendingIntent.getActivity(
        context,
        1001,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
    )

    val n = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("DIU Transport Schedule")
        .setContentText("Test notification — if you can see this, notifications are working ✅")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pending)
        .build()

    NotificationManagerCompat.from(context).notify(1001, n)
}
// ---------------- Real alarm scheduling (single next notification) ----------------

private fun cancelNextAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ScheduleAlarmReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context,
        MainActivity.ALARM_REQ_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
    )
    alarmManager.cancel(pi)
    Log.d("RouteNotificationScheduler", "Canceled next alarm")
}

/**
 * Schedules ONLY the next upcoming time (start/departure) across the selected route.
 * leadMinutes আগে notify করবে।
 */
private fun scheduleNextAlarmFromData(
    context: Context,
    selectedRoute: String,
    leadMinutes: Int,
    items: List<Any>
) {
    // 1) Permission checks
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            Log.w("RouteNotificationScheduler", "POST_NOTIFICATIONS not granted")
            return
        }
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        Log.w("RouteNotificationScheduler", "Exact alarm not allowed")
        return
    }

    if (items.isEmpty()) return

    // 2) Filter by route
    val filtered = if (selectedRoute == "ALL") items else items.filter { any ->
        val routeNo = any.readStringProp("routeNo")
        val routeName = any.readStringProp("routeName")
        val sel = selectedRoute.trim()
        routeNo.equals(sel, ignoreCase = true) || routeName.equals(sel, ignoreCase = true)
    }

    if (filtered.isEmpty()) return

    // 3) Find next upcoming time
    val now = LocalDateTime.now()
    val zone = ZoneId.systemDefault()

    var bestFireAt: LocalDateTime? = null
    var bestTitle = "DIU Transport Schedule"
    var bestText = "Bus reminder"

    for (any in filtered) {
        val routeNo = any.readStringProp("routeNo").ifBlank { selectedRoute }
        val routeName = any.readStringProp("routeName").ifBlank { "DIU Route" }
        val startTimes = any.readStringListProp("startTimes")
        val departureTimes = any.readStringListProp("departureTimes")

        val allTimes = (startTimes + departureTimes)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .flatMap { extractClockTimes(it) }

        for (t in allTimes) {
            val next = nextOccurrence(now, t) ?: continue
            val fireAt = next.minusMinutes(leadMinutes.toLong())
            if (fireAt.isAfter(now)) {
                if (bestFireAt == null || fireAt.isBefore(bestFireAt)) {
                    bestFireAt = fireAt
                    bestTitle = "DIU Bus Reminder • $routeNo"
                    bestText = "$routeName at ${formatTime(next.toLocalTime())} (lead ${leadMinutes}m)"
                }
            }
        }
    }

    if (bestFireAt == null) {
        Log.w("RouteNotificationScheduler", "No upcoming time found")
        return
    }

    ensureNotificationChannel(
        context,
        MainActivity.NOTIF_CHANNEL_ID,
        MainActivity.NOTIF_CHANNEL_NAME,
        MainActivity.NOTIF_CHANNEL_DESC
    )

    val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
        putExtra(MainActivity.EXTRA_TITLE, bestTitle)
        putExtra(MainActivity.EXTRA_TEXT, bestText)
    }

    val pi = PendingIntent.getBroadcast(
        context,
        MainActivity.ALARM_REQ_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
    )

    val triggerAtMillis = bestFireAt!!.atZone(zone).toInstant().toEpochMilli()

    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        pi
    )

    Log.d("RouteNotificationScheduler", "Scheduled next alarm at=$bestFireAt")
}

private fun formatTime(t: LocalTime): String =
    t.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))

private fun nextOccurrence(now: LocalDateTime, t: LocalTime): LocalDateTime? {
    val today = LocalDate.now()
    val todayDt = LocalDateTime.of(today, t)
    return if (todayDt.isAfter(now)) todayDt else LocalDateTime.of(today.plusDays(1), t)
}

private fun extractClockTimes(raw: String): List<LocalTime> {
    val regex = Regex("(\\b\\d{1,2}:\\d{2}\\s*[AaPp][Mm]\\b)")
    val matches = regex.findAll(raw).map { it.value }.toList()
    if (matches.isEmpty()) return emptyList()

    val fmt1 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    val fmt2 = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)

    return matches.mapNotNull { s0 ->
        val s = s0.trim().replace(Regex("\\s+"), " ").uppercase(Locale.ENGLISH)
        try {
            LocalTime.parse(s, fmt1)
        } catch (_: DateTimeParseException) {
            try {
                LocalTime.parse(s.replace(" ", ""), fmt2)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}

private fun Any.readStringProp(name: String): String {
    return try {
        val f = this::class.java.getDeclaredField(name)
        f.isAccessible = true
        (f.get(this) as? String).orEmpty()
    } catch (_: Throwable) {
        try {
            val getter = "get" + name.replaceFirstChar { it.uppercase() }
            val m = this::class.java.methods.firstOrNull { it.name == getter }
            (m?.invoke(this) as? String).orEmpty()
        } catch (_: Throwable) { "" }
    }
}

@Suppress("UNCHECKED_CAST")
private fun Any.readStringListProp(name: String): List<String> {
    return try {
        val f = this::class.java.getDeclaredField(name)
        f.isAccessible = true
        (f.get(this) as? List<String>) ?: emptyList()
    } catch (_: Throwable) {
        try {
            val getter = "get" + name.replaceFirstChar { it.uppercase() }
            val m = this::class.java.methods.firstOrNull { it.name == getter }
            (m?.invoke(this) as? List<String>) ?: emptyList()
        } catch (_: Throwable) { emptyList() }
    }
}

// Receiver that shows notification when alarm fires
class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ensureNotificationChannel(
            context,
            MainActivity.NOTIF_CHANNEL_ID,
            MainActivity.NOTIF_CHANNEL_NAME,
            MainActivity.NOTIF_CHANNEL_DESC
        )

        val title = intent.getStringExtra(MainActivity.EXTRA_TITLE) ?: "DIU Transport Schedule"
        val text = intent.getStringExtra(MainActivity.EXTRA_TEXT) ?: "Bus reminder"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pending = PendingIntent.getActivity(
            context,
            2001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val n = NotificationCompat.Builder(context, MainActivity.NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        NotificationManagerCompat.from(context).notify(MainActivity.ALARM_REQ_CODE, n)
        Log.d("RouteNotificationScheduler", "Alarm fired -> notification shown")
    }
}