package com.sohan.diutransportschedule.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sohan.diutransportschedule.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale



@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(vm: HomeViewModel, pad: PaddingValues) {
    val items by vm.items.collectAsState()
    val showUpdate by vm.showUpdate.collectAsState()
    val msg by vm.updateMessage.collectAsState()
    val selectedRoute by vm.selectedRoute.collectAsState()
    val isSyncing by vm.isSyncing.collectAsState()
    val compactMode by vm.compactMode.collectAsState()
    val notificationsEnabled by vm.notificationsEnabled.collectAsState()
    val notifyLeadMinutes by vm.notifyLeadMinutes.collectAsState()
    val ctx = LocalContext.current
    val appDark by vm.darkMode.collectAsState()

    var q by rememberSaveable { mutableStateOf("") }
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.sync() }

    LaunchedEffect(items) {
        if (expandedKey != null && items.none { it.stableId() == expandedKey }) expandedKey = null
        if (items.size == 1) expandedKey = items.first().stableId()
    }

    // Schedule/cancel notifications when settings or data change
    // NOTE: Temporarily disabled while fixing crash. Re-enable after verifying Logcat.
//    LaunchedEffect(selectedRoute, notificationsEnabled, notifyLeadMinutes, items) {
//        vm.updateRouteNotifications(
//            context = ctx,
//            selectedRoute = selectedRoute,
//            currentItems = items,
//            enabled = notificationsEnabled,
//            leadMinutes = notifyLeadMinutes
//        )
//    }

    val pullState = rememberPullRefreshState(
        refreshing = isSyncing,
        onRefresh = { vm.refresh(showBannerIfUpdated = true) }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            PremiumHeader(selectedRoute = selectedRoute, syncing = isSyncing)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-14).dp)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PremiumSearchBar(
                    value = q,
                    onChange = { q = it; vm.setQuery(it) },
                    onClear = { q = ""; vm.setQuery("") }
                )

                if (showUpdate) {
                    UpdateBanner(message = msg, onOk = { vm.dismissUpdate() }, appDark = appDark)
                }

                if (items.isEmpty()) {
                    EmptyStateCard(isSyncing = isSyncing, appDark = appDark)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 130.dp)
                    ) {
                        items(
                            items = items,
                            key = { it.stableId() },
                            contentType = { "schedule_card" }) { item ->
                            val key = item.stableId()
                            val expanded = expandedKey == key
                            // Removed unused dark/theme color logic
                            PremiumAccordionScheduleCard(
                                item = item,
                                expanded = expanded,
                                onToggle = { expandedKey = if (expanded) null else key },
                                defaultCollapsedWhenMany = items.size > 1,
                                compact = compactMode,
                                appDark = appDark
                            )
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isSyncing,
            state = pullState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/* ------------------ HEADER ------------------ */

@Composable
private fun PremiumHeader(selectedRoute: String, syncing: Boolean) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        )
    )

    val today = remember {
        val fmt = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.ENGLISH)
        LocalDate.now().format(fmt)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(horizontal = 14.dp)
            .padding(top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "DIU Transport Schedule",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            DatePill(text = today, syncing = syncing)
        }

        val sub = if (selectedRoute == "ALL") "Daily Schedule • All Routes"
        else "Daily Schedule • Route: $selectedRoute"

        Text(
            text = sub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DatePill(text: String, syncing: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (syncing) "Syncing…" else text,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ------------------ SEARCH ------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumSearchBar(
    value: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            singleLine = true,
            placeholder = { Text("Search route / stop / route no") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (value.isNotBlank()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.70f),
                cursorColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

/* ------------------ UPDATE BANNER ------------------ */

@Composable
private fun UpdateBanner(message: String, onOk: () -> Unit, appDark: Boolean) {
    val dark = appDark

    // More visible in dark mode, still clean in light mode
    val container = if (dark) {
        // a slightly elevated surface reads better on dark backgrounds
        MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val onContainer = if (dark) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // In dark mode, secondary pops better than primary on many palettes
    val accent = if (dark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

    val gradient = Brush.horizontalGradient(
        colors = listOf(
            container,
            container.copy(alpha = if (dark) 0.98f else 0.92f)
        )
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (dark) 0.65f else 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .background(gradient)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(accent.copy(alpha = 0.85f))
            )

            Spacer(Modifier.width(10.dp))

            // Icon pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = accent
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Update",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.90f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(10.dp))

            FilledTonalButton(
                onClick = onOk,
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = accent.copy(alpha = 0.14f),
                    contentColor = accent
                )
            ) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* ------------------ EMPTY ------------------ */

@Composable
private fun EmptyStateCard(isSyncing: Boolean, appDark: Boolean) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("No schedule found", style = MaterialTheme.typography.titleMedium)
            Text(
                if (isSyncing) "Syncing… please wait"
                else "Pull down to refresh or change filter in Profile.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)
            )
        }
    }
}

/* ------------------ CARD ------------------ */

@Composable
private fun PremiumAccordionScheduleCard(
    item: UiSchedule,
    expanded: Boolean,
    onToggle: () -> Unit,
    defaultCollapsedWhenMany: Boolean,
    compact: Boolean,
    appDark: Boolean
) {
    val dark = appDark
    val cardColor = if (dark) MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp) else MaterialTheme.colorScheme.surface
    val cardBorder = if (dark) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    val lightText = Color.Black
    val lightSubText = Color.Black
    val actionGreen = Color(0xFF00C853) // green for dark mode action

    val key = remember(item) { item.stableId() }

    val stops = remember(key) { extractStops(item.routeDetails) }
    val parts = remember(key) { parseRouteParts(item.routeDetails) }

    val startOne = remember(item.startTimes) { item.startTimes.firstOrNull()?.trim().orEmpty() }
    val depOne =
        remember(item.departureTimes) { item.departureTimes.firstOrNull()?.trim().orEmpty() }

    val rightLabel by remember(startOne, depOne) {
        derivedStateOf {
            when {
                startOne.isNotBlank() -> "Start • $startOne"
                depOne.isNotBlank() -> "Dep • $depOne"
                else -> ""
            }
        }
    }

    val summaryTimes by remember(startOne, depOne) {
        derivedStateOf {
            buildString {
                if (startOne.isNotBlank()) append("Start: $startOne")
                if (depOne.isNotBlank()) {
                    if (isNotEmpty()) append("  •  ")
                    append("Dep: $depOne")
                }
            }
        }
    }

    val viaPreview by remember(parts.via) {
        derivedStateOf {
            if (parts.via.isBlank()) "" else {
                val list = parts.via.split(" • ").filter { it.isNotBlank() }
                val show = list.take(3)
                val extra = list.size - show.size
                if (extra > 0) show.joinToString(" • ") + "  (+$extra more)" else show.joinToString(" • ")
            }
        }
    }

    val viaText by remember(parts.via, expanded, defaultCollapsedWhenMany) {
        derivedStateOf {
            if (parts.via.isBlank()) "" else {
                // expanded হলে পুরো via দেখাবে, collapsed হলে preview
                if (expanded || !defaultCollapsedWhenMany) parts.via else viaPreview
            }
        }
    }

    val interaction = remember { MutableInteractionSource() }

    // Hide route/stops by default; user can tap to show
    var showRoute by rememberSaveable(key) { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (cardBorder != null) Modifier.border(cardBorder, RoundedCornerShape(22.dp)) else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = {
                    // if user is collapsing, hide route details next time
                    if (expanded) showRoute = false
                    onToggle()
                }
            ),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (compact) 4.dp else 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                RouteBadgeSmall(item.routeNo, appDark = dark)
                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    val fromText = if (parts.from.isNotBlank()) parts.from else item.routeName
                    val toText = parts.to

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📍", modifier = Modifier.padding(end = 6.dp))
                        Text(
                            text = fromText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (dark) MaterialTheme.colorScheme.onSurface else lightText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (toText.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏁", modifier = Modifier.padding(end = 6.dp))
                            Text(
                                text = toText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dark) MaterialTheme.colorScheme.onSurface else lightText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = item.routeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f) else lightSubText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (rightLabel.isNotBlank()) TimePill(text = rightLabel, appDark = dark)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (expanded || !defaultCollapsedWhenMany) "Hide ▲" else "Show ▼",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (dark) MaterialTheme.colorScheme.primary else Color.Black
                    )
                }
            }

                if (viaText.isNotBlank()) {
                    Text(
                        text = "Via: $viaText",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f) else lightSubText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

            if (!expanded && defaultCollapsedWhenMany) {
                val small = stops.take(2)
                if (small.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        small.forEach { s -> MiniStopRow(title = s) }
                        if (stops.size > 2) {
                            Text(
                                text = "+ ${stops.size - 2} more stops (tap to expand)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (dark) Color.White.copy(alpha = 0.88f) else lightSubText,
                            )
                        }
                        if (summaryTimes.isNotBlank()) SummaryTimesPill(summaryTimes)
                    }
                }
                return@ElevatedCard
            }

            AnimatedVisibility(
                visible = expanded || !defaultCollapsedWhenMany,
                enter = fadeIn(tween(110)) + expandVertically(tween(140)),
                exit = fadeOut(tween(90)) + shrinkVertically(tween(120))
            ) {
                Column {
                    // ✅ Main focus: show Times first
                    TimesSection(
                        startTimes = item.startTimes,
                        departureTimes = item.departureTimes,
                        compact = compact,
                        appDark = dark
                    )

                    Spacer(Modifier.height(10.dp))

                    // Toggle for route details / stops
                    TextButton(
                        onClick = { showRoute = !showRoute },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (showRoute) "Hide route details ▲" else "Show route details ▼",
                            color = if (dark) actionGreen else Color.Black
                        )
                    }

                    AnimatedVisibility(
                        visible = showRoute,
                        enter = fadeIn(tween(120)) + expandVertically(tween(160)),
                        exit = fadeOut(tween(90)) + shrinkVertically(tween(140))
                    ) {
                        Column {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
                            )
                            Spacer(Modifier.height(8.dp))

                            if (stops.isEmpty()) {
                                Text(
                                    item.routeDetails,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (dark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f) else lightSubText,
                                    maxLines = 12,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    stops.forEachIndexed { i, stop ->
                                        TimelineRowPolished(
                                            title = stop,
                                            isFirst = i == 0,
                                            isLast = i == stops.lastIndex
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------ Timeline Row ------------------ */

@Composable
private fun TimelineRowPolished(title: String, isFirst: Boolean, isLast: Boolean) {
    val dotColor = when {
        isFirst -> Color(0xFFE53935)
        isLast -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.secondary
    }
    val lineColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)
    val textColor = if (isLast) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.width(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(12.dp)
                        .background(lineColor)
                )
            } else Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dotColor)
            )

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(12.dp)
                        .background(lineColor)
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ------------------ Mini + Summary ------------------ */

@Composable
private fun MiniStopRow(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SummaryTimesPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ------------------ Times (FIXED) ------------------ */

@Composable
private fun TimesSection(
    startTimes: List<String>,
    departureTimes: List<String>,
    compact: Boolean,
    appDark: Boolean
) {
    val dark = appDark
    val timesCardColor =
        if (dark) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        else MaterialTheme.colorScheme.surface

    val timesBorder =
        if (dark) null
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = timesCardColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (timesBorder != null) Modifier.border(timesBorder, RoundedCornerShape(18.dp)) else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 10.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
        ) {
            Text(
                text = "Times",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (dark) MaterialTheme.colorScheme.onSurface else Color.Black
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

            TimesBlock(
                title = "Start Times",
                times = startTimes,
                multiline = false,
                appDark = dark
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f))

            TimesBlock(
                title = "Departure Times",
                times = departureTimes,
                multiline = true,
                appDark = dark
            )
        }
    }
}

@Composable
private fun TimesBlock(
    title: String,
    times: List<String>,
    multiline: Boolean,
    appDark: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = if (appDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
        )

        if (times.isEmpty()) {
            Text(
                text = "—",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
            )
        } else {
            TimeChipsRow(times, multiline = multiline, appDark = appDark)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeChipsRow(times: List<String>, multiline: Boolean, appDark: Boolean) {
    val clean = remember(times) {
        times.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }
    if (clean.isEmpty()) return

    // ✅ light = Black, dark = White (app toggle অনুযায়ী)
    val chipTextColor = if (appDark) Color.White else Color.Black

    fun splitTimeAndNote(raw: String): Pair<String, String?> {
        // Prefer explicit newline split first (your data often has time + note on next line)
        val lines = raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size >= 2) return lines.first() to lines.drop(1).joinToString(" ")

        // Fallback: try to extract a time like 7:20 am / 7:20AM / 7:20:00 pm
        val r = raw.trim()
        val regex = Regex("(\\d{1,2}:\\d{2}(?::\\d{2})?\\s*[APap][Mm])")
        val m = regex.find(r)
        return if (m != null) {
            val time = m.value.trim().replace(Regex("\\s+"), " ")
            val note = r.replace(m.value, "").trim().trimStart('-', '—').trim()
            if (note.isBlank()) time to null else time to note
        } else {
            r to null
        }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        clean.forEach { t ->
            AssistChip(
                onClick = {},
                modifier = (if (multiline) Modifier.fillMaxWidth() else Modifier)
                    .heightIn(min = if (multiline) 72.dp else 54.dp),
                label = {
                    Box(
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = if (multiline) 12.dp else 10.dp
                        )
                    ) {
                        val (timeText, noteText) = splitTimeAndNote(t)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = chipTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (noteText != null && noteText.isNotBlank()) {
                                Text(
                                    text = noteText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = chipTextColor.copy(alpha = 0.65f),
                                    maxLines = if (multiline) 3 else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(999.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (appDark) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    labelColor = chipTextColor
                ),
                border = BorderStroke(
                    1.dp,
                    if (appDark)
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                )
            )
        }
    }
}

/* ------------------ Helpers ------------------ */

@Composable
private fun RouteBadgeSmall(routeNo: String, appDark: Boolean) {
    val dark = appDark

    val bg = if (dark) MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)
    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)

    val fg = if (dark) Color.White else Color.Black

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(routeNo, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

@Composable
private fun TimePill(text: String, appDark: Boolean) {
    val dark = appDark

    val bg = if (dark) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
    else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.90f)

    val fg = if (dark) Color.White else Color.Black

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ------------------ Extract + Parts ------------------ */

private fun extractStops(details: String): List<String> {
    return details
        .replace("<>", " > ")
        .replace("→", " > ")
        .replace("—", " > ")
        .replace("-", " > ")
        .replace(">", " > ")
        .replace("<", " > ")
        .split(" > ")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private data class RouteParts(val from: String, val to: String, val via: String)

private fun parseRouteParts(details: String): RouteParts {
    val stops = extractStops(details)
    return when {
        stops.size >= 2 -> {
            val from = stops.first()
            val to = stops.last()
            val mid = stops.drop(1).dropLast(1)
            val via = if (mid.isEmpty()) "" else mid.joinToString(" • ")
            RouteParts(from, to, via)
        }

        stops.size == 1 -> RouteParts(stops[0], "", "")
        else -> RouteParts("", "", "")
    }
}

/* ------------------ Stable ID ------------------ */

private fun UiSchedule.stableId(): String {
    return "${routeNo}__${routeName}__${routeDetails.hashCode()}"
}