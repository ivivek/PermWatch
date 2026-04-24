package com.trogo.permalert

import android.app.Application
import com.trogo.permalert.notify.AlertNotifier
import com.trogo.permalert.worker.ScanScheduler

class PermalertApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AlertNotifier(this).ensureChannel()
        ScanScheduler.ensureScheduled(this)
    }
}
