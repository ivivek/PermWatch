package com.linetra.permwatch

import android.app.Application
import com.linetra.permwatch.notify.AlertNotifier
import com.linetra.permwatch.worker.ScanScheduler

class PermWatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AlertNotifier(this).ensureChannel()
        ScanScheduler.ensureScheduled(this)
    }
}
