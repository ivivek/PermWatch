package com.linetra.permwatch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linetra.permwatch.data.AlertDiff
import com.linetra.permwatch.data.InstalledAppPerms
import com.linetra.permwatch.data.PermsStore
import com.linetra.permwatch.data.SensitivePermissions
import com.linetra.permwatch.notify.AlertNotifier
import com.linetra.permwatch.scanner.PermissionScanner
import com.linetra.permwatch.worker.ScanScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun refresh() {
        viewModelScope.launch {
            if (!store.isOnboarded()) return@launch
            _state.value = _state.value.copy(loading = true)
            val apps = withContext(Dispatchers.IO) { scanner.scanAll() }

            store.pruneBaselineToCurrent(AlertDiff.currentGrantsMap(apps))

            val baseline = store.currentBaseline()
            val ignored = store.currentIgnored()
            val watched = SensitivePermissions.watchedSet(store.currentUnwatched())
            _state.value = UiState(
                loading = false,
                rows = toRows(apps, baseline, ignored, watched),
            )
            updateNotification(apps, baseline, ignored, watched)
        }
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
            store.acceptForPackage(packageName, row.granted)
            refresh()
        }
    }

    fun acceptAll() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { scanner.scanAll() }
            store.acceptCurrentAsBaseline(AlertDiff.currentGrantsMap(apps))
            refresh()
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
            .sortedWith(compareByDescending<AppRow> { it.hasAlert }.thenBy { it.label.lowercase() })
    }

}
