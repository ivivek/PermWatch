package com.linetra.permwatch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linetra.permwatch.data.AlertDiff
import com.linetra.permwatch.data.InstalledAppPerms
import com.linetra.permwatch.data.PermsStore
import com.linetra.permwatch.notify.AlertNotifier
import com.linetra.permwatch.scanner.PermissionScanner
import com.linetra.permwatch.worker.ScanScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val onboarded: Boolean = false,
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

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val apps = withContext(Dispatchers.IO) { scanner.scanAll() }
            val onboarded = store.isOnboarded()
            val baseline = store.currentBaseline()
            val ignored = store.currentIgnored()
            _state.value = UiState(
                loading = false,
                onboarded = onboarded,
                rows = toRows(apps, baseline, ignored),
            )
            if (onboarded) updateNotification(apps, baseline, ignored)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { scanner.scanAll() }
            val current = AlertDiff.currentGrantsMap(apps)
            store.acceptCurrentAsBaseline(current)
            store.setOnboarded(true)
            ScanScheduler.ensureScheduled(getApplication())
            _state.value = _state.value.copy(
                onboarded = true,
                rows = toRows(apps, current, store.currentIgnored()),
            )
            notifier.cancelSummary()
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

    private suspend fun updateNotification(
        apps: List<InstalledAppPerms>,
        baseline: Map<String, Set<String>>,
        ignored: Set<String>,
    ) = withContext(Dispatchers.IO) {
        val flagged = AlertDiff.compute(apps, baseline, ignored)
        notifier.ensureChannel()
        notifier.updateSummary(flagged)
    }

    private fun toRows(
        apps: List<InstalledAppPerms>,
        baseline: Map<String, Set<String>>,
        ignored: Set<String>,
    ): List<AppRow> {
        return apps
            .filter { it.grantedSensitive.isNotEmpty() }
            .map { app ->
                val known = baseline[app.packageName] ?: emptySet()
                AppRow(
                    packageName = app.packageName,
                    label = app.label,
                    isSystem = app.isSystem,
                    granted = app.grantedSensitive,
                    newPerms = app.grantedSensitive - known,
                    isIgnored = app.packageName in ignored,
                )
            }
            .sortedWith(compareByDescending<AppRow> { it.hasAlert }.thenBy { it.label.lowercase() })
    }
}
