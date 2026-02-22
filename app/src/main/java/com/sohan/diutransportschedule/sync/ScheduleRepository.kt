package com.sohan.diutransportschedule.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.sohan.diutransportschedule.db.DbScheduleItem
import com.sohan.diutransportschedule.db.JsonConverters
import com.sohan.diutransportschedule.db.ScheduleDao
import com.sohan.diutransportschedule.prefs.UserPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

data class SyncResult(
    val updated: Boolean,
    val version: Int,
    val message: String
)

class ScheduleRepository(
    private val dao: ScheduleDao,
    private val fs: FirebaseFirestore,
    private val store: VersionStore,
    private val prefs: UserPrefs
) {

    fun observeLocal() = dao.observeAll()

    // ---------------- Preferences (Expose from UserPrefs) ----------------

    val selectedRouteFlow: Flow<String> = prefs.selectedRouteFlow
    suspend fun setSelectedRoute(routeNo: String) = prefs.setSelectedRoute(routeNo)

    val darkModeFlow: Flow<Boolean> = prefs.darkModeFlow
    suspend fun ensureDefaultPrefs() = prefs.ensureDefaults()
    suspend fun setDarkMode(enabled: Boolean) = prefs.setDarkMode(enabled)

    // ✅ NEW: Update banner preference
    val showUpdateBannerFlow: Flow<Boolean> = prefs.showUpdateBannerFlow
    suspend fun setShowUpdateBanner(enabled: Boolean) = prefs.setShowUpdateBanner(enabled)

    // ✅ NEW: Compact mode preference
    val compactModeFlow: Flow<Boolean> = prefs.compactModeFlow
    suspend fun setCompactMode(enabled: Boolean) = prefs.setCompactMode(enabled)

    // ✅ NEW: Notifications preference
    val notificationsEnabledFlow: Flow<Boolean> = prefs.notificationsEnabledFlow
    suspend fun setNotificationsEnabled(enabled: Boolean) = prefs.setNotificationsEnabled(enabled)

    val notifyLeadMinutesFlow: Flow<Int> = prefs.notifyLeadMinutesFlow
    suspend fun setNotifyLeadMinutes(minutes: Int) = prefs.setNotifyLeadMinutes(minutes)

    // ---------------- Sync ----------------

    suspend fun syncIfNeeded(): SyncResult {
        val meta = fs.collection("meta").document("app").get().await()
        val remoteVersion = (meta.getLong("version") ?: 0L).toInt()
        val message = meta.getString("message") ?: ""

        val localVersion = store.getLocalVersion()
        if (remoteVersion <= localVersion) {
            return SyncResult(false, remoteVersion, message)
        }

        val doc = fs.collection("schedules")
            .document("current")
            .collection("data")
            .document("items")
            .get().await()

        val raw = doc.get("items") as? List<Map<String, Any?>> ?: emptyList()

        val dbItems = raw.map { m ->
            val routeNo = m["routeNo"] as? String ?: ""
            val routeName = m["routeName"] as? String ?: ""
            val routeDetails = m["routeDetails"] as? String ?: ""
            val startTimes =
                (m["startTimes"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val depTimes =
                (m["departureTimes"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            DbScheduleItem(
                id = "${routeNo}_${routeName}".trim(),
                routeNo = routeNo,
                routeName = routeName,
                startTimesJson = JsonConverters.listToJson(startTimes),
                departureTimesJson = JsonConverters.listToJson(depTimes),
                routeDetails = routeDetails
            )
        }

        dao.clearAll()
        dao.upsertAll(dbItems)
        store.setLocalVersion(remoteVersion)

        return SyncResult(true, remoteVersion, message)
    }

    suspend fun shouldShowUpdate(version: Int): Boolean {
        val seen = store.getSeenVersion()
        return version > seen
    }

    suspend fun markSeen(version: Int) {
        store.setSeenVersion(version)
    }
}