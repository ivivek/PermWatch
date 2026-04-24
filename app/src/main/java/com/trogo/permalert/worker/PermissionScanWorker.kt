package com.trogo.permalert.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trogo.permalert.data.AlertDiff
import com.trogo.permalert.data.PermsStore
import com.trogo.permalert.notify.AlertNotifier
import com.trogo.permalert.scanner.PermissionScanner

class PermissionScanWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val store = PermsStore(ctx)

        if (!store.isOnboarded()) {
            ScanScheduler.scheduleNext(ctx)
            return Result.success()
        }

        val scanner = PermissionScanner(ctx)
        val apps = scanner.scanAll()

        val baseline = store.currentBaseline()
        val ignored = store.currentIgnored()
        val flagged = AlertDiff.compute(apps, baseline, ignored)

        val notifier = AlertNotifier(ctx)
        notifier.ensureChannel()
        notifier.updateSummary(flagged)

        ScanScheduler.scheduleNext(ctx)
        return Result.success()
    }
}
