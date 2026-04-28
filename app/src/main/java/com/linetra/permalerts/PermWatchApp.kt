package com.linetra.permalerts

import android.app.Application
import com.linetra.permalerts.data.PermsStore
import com.linetra.permalerts.notify.AlertNotifier
import com.linetra.permalerts.worker.ScanScheduler
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
