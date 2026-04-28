package com.linetra.permwatch.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Self-chaining one-time worker. WorkManager's PeriodicWorkRequest floors at 15 min, and we want
 * sub-15m cadences available, so the worker re-enqueues itself via [scheduleNext] after each run.
 * Interval comes from PermsStore — see [ScanCadence] for the user-facing presets.
 */
object ScanScheduler {

    private const val CHAIN_NAME = "perm_scan_chain"
    private const val ONESHOT_NAME = "perm_scan_oneshot"
    private const val LEGACY_PERIODIC_NAME = "perm_scan_periodic"

    fun ensureScheduled(context: Context, intervalSeconds: Long) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(LEGACY_PERIODIC_NAME)
        enqueue(wm, intervalSeconds, policy = ExistingWorkPolicy.KEEP)
    }

    fun scheduleNext(context: Context, intervalSeconds: Long) {
        enqueue(WorkManager.getInstance(context), intervalSeconds, policy = ExistingWorkPolicy.REPLACE)
    }

    fun runOnce(context: Context) {
        val request = OneTimeWorkRequestBuilder<PermissionScanWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONESHOT_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun enqueue(wm: WorkManager, intervalSeconds: Long, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<PermissionScanWorker>()
            .setInitialDelay(intervalSeconds, TimeUnit.SECONDS)
            .build()
        wm.enqueueUniqueWork(CHAIN_NAME, policy, request)
    }
}

object ScanCadence {
    data class Preset(val seconds: Long, val label: String)

    val presets: List<Preset> = listOf(
        Preset(60L, "1 minute"),
        Preset(300L, "5 minutes"),
        Preset(900L, "15 minutes"),
        Preset(1800L, "30 minutes"),
        Preset(3600L, "1 hour"),
        Preset(21600L, "6 hours"),
        Preset(43200L, "12 hours"),
        Preset(86400L, "Daily"),
    )
}
