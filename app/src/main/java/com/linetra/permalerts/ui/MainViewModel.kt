package com.linetra.permalerts.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linetra.permalerts.data.AlertDiff
import com.linetra.permalerts.data.InstalledAppPerms
import com.linetra.permalerts.data.PermEvent
import com.linetra.permalerts.data.PermsStore
import com.linetra.permalerts.data.SensitivePermissions
import com.linetra.permalerts.notify.AlertNotifier
import com.linetra.permalerts.scanner.PermissionScanner
import com.linetra.permalerts.worker.ScanScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppRow(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val granted: Set<String>,
    val newPerms: Set<String>,
    val isIgnored: Boolean,
) {
    val hasAlert: Boolean get() = !isIgnored && newPerms.isNotEmpty()
}

data class UiState(
    val loading: Boolean = true,
    val rows: List<AppRow> = emptyList(),
) {
    val alertCount: Int get() = rows.count { it.hasAlert }
}

data class IgnoredApp(val packageName: String, val label: String)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val scanner = PermissionScanner(app)
    private val store = PermsStore(app)
    private val notifier = AlertNotifier(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** null until the DataStore has emitted at least once, so the UI can render a splash
     *  rather than briefly flashing the wrong screen on cold start. */
    val onboarded: StateFlow<Boolean?> = store.onboarded
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Manifest names the user has opted out of watching. Drives both the Settings UI and
     *  the diff filter — empty by default so first-run watches the full catalog. */
    val unwatched: StateFlow<Set<String>> = store.unwatched
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** User-chosen scan cadence in seconds. Defaults to PermsStore.DEFAULT_INTERVAL_SECONDS. */
    val intervalSeconds: StateFlow<Long> = store.intervalSeconds
        .stateIn(viewModelScope, SharingStarted.Eagerly, PermsStore.DEFAULT_INTERVAL_SECONDS)

    /** Newest-first stream of permission state changes recorded across scans. */
    val events: StateFlow<List<PermEvent>> = store.events
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Number of events newer than the user's last History visit — drives the bell badge. */
    val unreadEventCount: StateFlow<Int> = combine(store.events, store.lastSeenEventTs) { evs, seen ->
        evs.count { it.tsMillis > seen }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** Bump the last-seen pointer to the newest event so the badge clears. Called when History
     *  opens. No-op when there are no events. */
    fun markEventsRead() {
        viewModelScope.launch {
            val newest = store.currentEvents().firstOrNull()?.tsMillis ?: return@launch
            store.setLastSeenEventTs(newest)
        }
    }

    /** Apps the user has muted via the per-card toggle. Drives the Settings management sheet —
     *  only includes apps still present in the latest scan (orphans drop out naturally). */
    val ignoredApps: StateFlow<List<IgnoredApp>> = state
        .map { ui ->
            ui.rows
                .asSequence()
                .filter { it.isIgnored }
                .map { IgnoredApp(it.packageName, it.label) }
                .sortedBy { it.label.lowercase() }
                .toList()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Emitted after a notification-tap-driven scan completes, so the UI can pick the right tab
     *  and scroll to the alerted card *with the post-scan rows*. Replay 0 + buffer 1 so an emit
     *  during a config change is held until the new collector attaches; DROP_OLDEST keeps only
     *  the latest pending jump if multiple taps land before the UI catches up. */
    private val _scrollToAlert = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val scrollToAlert: SharedFlow<Unit> = _scrollToAlert.asSharedFlow()

    /** Packages whose alert the user just acknowledged via "Accept" (or "Accept all"). They
     *  keep their position in the sort as if still alerted, so the card visually calms down in
     *  place rather than re-shuffling alphabetically into the non-alert region. Cleared on any
     *  refresh that isn't an accept — onResume, manual rescan, notification tap, ignore/watch
     *  toggles — so the next "round" starts fresh. In-memory only; cold start clears via VM
     *  recreation. */
    private var recentlyAccepted: Set<String> = emptySet()

    fun refresh() {
        viewModelScope.launch {
            recentlyAccepted = emptySet()
            runScan()
        }
    }

    /** Called by [MainActivity] when the activity is opened via a notification tap. Runs a scan
     *  inline and emits [scrollToAlert] only after the new rows are visible to the UI, so the
     *  collector can read post-refresh data when picking a tab and scrolling. */
    fun onAlertTap() {
        viewModelScope.launch {
            recentlyAccepted = emptySet()
            runScan()
            _scrollToAlert.emit(Unit)
        }
    }

    private suspend fun runScan() {
        if (!store.isOnboarded()) return
        _state.value = _state.value.copy(loading = true)
        val apps = withContext(Dispatchers.IO) { scanner.scanAll() }

        val grantsMap = AlertDiff.currentGrantsMap(apps)
        store.recordEventsForScan(
            current = grantsMap,
            labelLookup = apps.associate { it.packageName to it.label },
            systemLookup = apps.associate { it.packageName to it.isSystem },
        )
        store.pruneBaselineToCurrent(grantsMap)

        val baseline = store.currentBaseline()
        val ignored = store.currentIgnored()
        val watched = SensitivePermissions.watchedSet(store.currentUnwatched())
        _state.value = UiState(
            loading = false,
            rows = toRows(apps, baseline, ignored, watched, recentlyAccepted),
        )
        updateNotification(apps, baseline, ignored, watched)
    }

    /** Called from the Intro screen's "Activate" button. Snapshots current grants as the
     *  baseline, schedules the worker, flips the onboarded flag, and renders the first scan. */
    fun activate() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { scanner.scanAll() }
            store.acceptCurrentAsBaseline(AlertDiff.currentGrantsMap(apps))
            ScanScheduler.ensureScheduled(getApplication(), store.currentIntervalSeconds())
            store.setOnboarded(true)
            refresh()
        }
    }

    /** Persist a new scan cadence and reschedule the chain so the next tick uses it. */
    fun setIntervalSeconds(seconds: Long) {
        viewModelScope.launch {
            store.setIntervalSeconds(seconds)
            ScanScheduler.scheduleNext(getApplication(), seconds)
        }
    }

    fun acceptApp(packageName: String) {
        viewModelScope.launch {
            val row = _state.value.rows.firstOrNull { it.packageName == packageName } ?: return@launch
            recentlyAccepted = recentlyAccepted + packageName
            store.acceptForPackage(packageName, row.granted)
            runScan()
        }
    }

    fun acceptAll() {
        viewModelScope.launch {
            val nowAlerted = _state.value.rows
                .asSequence()
                .filter { it.hasAlert }
                .map { it.packageName }
                .toSet()
            recentlyAccepted = recentlyAccepted + nowAlerted
            val apps = withContext(Dispatchers.IO) { scanner.scanAll() }
            store.acceptCurrentAsBaseline(AlertDiff.currentGrantsMap(apps))
            runScan()
        }
    }

    fun toggleIgnore(packageName: String, ignored: Boolean) {
        viewModelScope.launch {
            store.setIgnored(packageName, ignored)
            refresh()
        }
    }

    /** Toggle whether a sensitive permission is watched. Re-enabling silently merges current
     *  grants of that perm into the baseline, so existing grants don't fire as new alerts. */
    fun setWatched(perm: String, watched: Boolean) {
        viewModelScope.launch {
            val wasUnwatched = perm in store.currentUnwatched()
            store.setUnwatched(perm, !watched)
            if (watched && wasUnwatched) {
                val apps = withContext(Dispatchers.IO) { scanner.scanAll() }
                store.mergeIntoBaseline(perm, AlertDiff.currentGrantsMap(apps))
            }
            refresh()
        }
    }

    private suspend fun updateNotification(
        apps: List<InstalledAppPerms>,
        baseline: Map<String, Set<String>>,
        ignored: Set<String>,
        watched: Set<String>,
    ) = withContext(Dispatchers.IO) {
        val flagged = AlertDiff.compute(apps, baseline, ignored, watched)
        val previousCount = store.currentLastAlertCount()
        notifier.ensureChannel()
        notifier.updateSummary(flagged, previousCount)
        store.setLastAlertCount(flagged.size)
    }

    private fun toRows(
        apps: List<InstalledAppPerms>,
        baseline: Map<String, Set<String>>,
        ignored: Set<String>,
        watched: Set<String>,
        recentlyAccepted: Set<String>,
    ): List<AppRow> {
        return apps
            .map { app ->
                val watchedGranted = app.grantedSensitive intersect watched
                val known = baseline[app.packageName] ?: emptySet()
                AppRow(
                    packageName = app.packageName,
                    label = app.label,
                    isSystem = app.isSystem,
                    granted = watchedGranted,
                    newPerms = watchedGranted - known,
                    isIgnored = app.packageName in ignored,
                )
            }
            .filter { it.granted.isNotEmpty() }
            .sortedWith(
                compareByDescending<AppRow> { it.hasAlert || it.packageName in recentlyAccepted }
                    .thenBy { it.label.lowercase() },
            )
    }

}
