package com.linetra.permissionalerts.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.linetra.permissionalerts.data.AlertDiff
import com.linetra.permissionalerts.data.PermsStore
import com.linetra.permissionalerts.data.SensitivePermissions
import com.linetra.permissionalerts.notify.AlertNotifier
import com.linetra.permissionalerts.scanner.PermissionScanner

class PermissionScanWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val store = PermsStore(ctx)

        if (!store.isOnboarded()) {
            ScanScheduler.scheduleNext(ctx, store.currentIntervalSeconds())
            return Result.success()
        }

        val scanner = PermissionScanner(ctx)
        val apps = scanner.scanAll()

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
        val flagged = AlertDiff.compute(apps, baseline, ignored, watched)

        val notifier = AlertNotifier(ctx)
        notifier.ensureChannel()
        val previousCount = store.currentLastAlertCount()
        notifier.updateSummary(flagged, previousCount)
        store.setLastAlertCount(flagged.size)

        ScanScheduler.scheduleNext(ctx, store.currentIntervalSeconds())
        return Result.success()
    }
}
