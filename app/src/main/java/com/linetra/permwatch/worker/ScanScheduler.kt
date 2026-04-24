package com.linetra.permwatch.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Self-chaining one-time worker. WorkManager's PeriodicWorkRequest floors at 15 min — for testing
 * we want faster cadence, so the worker re-enqueues itself via [scheduleNext] after each run.
 */
object ScanScheduler {

    const val INTERVAL_SECONDS: Long = 60L

    private const val CHAIN_NAME = "perm_scan_chain"
    private const val ONESHOT_NAME = "perm_scan_oneshot"
    private const val LEGACY_PERIODIC_NAME = "perm_scan_periodic"

    fun ensureScheduled(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(LEGACY_PERIODIC_NAME)
        enqueue(wm, policy = ExistingWorkPolicy.KEEP)
    }

    fun scheduleNext(context: Context) {
        enqueue(WorkManager.getInstance(context), policy = ExistingWorkPolicy.REPLACE)
    }

    fun runOnce(context: Context) {
        val request = OneTimeWorkRequestBuilder<PermissionScanWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONESHOT_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun enqueue(wm: WorkManager, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<PermissionScanWorker>()
            .setInitialDelay(INTERVAL_SECONDS, TimeUnit.SECONDS)
            .build()
        wm.enqueueUniqueWork(CHAIN_NAME, policy, request)
    }
}
