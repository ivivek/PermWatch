package com.linetra.permissionalerts

import android.app.Application
import com.linetra.permissionalerts.data.PermsStore
import com.linetra.permissionalerts.notify.AlertNotifier
import com.linetra.permissionalerts.worker.ScanScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PermissionAlertsApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AlertNotifier(this).ensureChannel()
        appScope.launch {
            ScanScheduler.ensureScheduled(this@PermissionAlertsApp, PermsStore(this@PermissionAlertsApp).currentIntervalSeconds())
        }
    }
}
