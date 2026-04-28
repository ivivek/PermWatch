package com.linetra.permwatch

import android.app.Application
import com.linetra.permwatch.data.PermsStore
import com.linetra.permwatch.notify.AlertNotifier
import com.linetra.permwatch.worker.ScanScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PermWatchApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AlertNotifier(this).ensureChannel()
        appScope.launch {
            ScanScheduler.ensureScheduled(this@PermWatchApp, PermsStore(this@PermWatchApp).currentIntervalSeconds())
        }
    }
}
