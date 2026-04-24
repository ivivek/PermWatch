package com.linetra.permwatch.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.linetra.permwatch.MainActivity
import com.linetra.permwatch.R
import com.linetra.permwatch.data.FlaggedApp
import com.linetra.permwatch.data.SensitivePermissions

class AlertNotifier(private val context: Context) {

    private val nm: NotificationManagerCompat = NotificationManagerCompat.from(context)

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_alerts_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_channel_alerts_desc)
                setShowBadge(true)
            }
            val sys = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            sys.createNotificationChannel(channel)
        }
    }

    fun updateSummary(flagged: List<FlaggedApp>) {
        if (!hasPostPermission()) return
        if (flagged.isEmpty()) {
            nm.cancel(NOTIF_ID_SUMMARY)
            return
        }

        val title = context.resources.getQuantityStringCompat(
            flagged.size,
            "%d app has new access",
            "%d apps have new access",
        )

        val inbox = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText("Signal · since baseline")
        flagged.take(MAX_LINES).forEach { app ->
            val perms = app.newPerms.joinToString(", ") { SensitivePermissions.labelFor(it) }
            inbox.addLine("${app.label}: $perms")
        }
        val moreSuffix = if (flagged.size > MAX_LINES) "  · +${flagged.size - MAX_LINES} more" else ""

        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pi = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val firstLine = flagged.firstOrNull()?.let {
            "${it.label}: ${it.newPerms.joinToString(", ") { p -> SensitivePermissions.labelFor(p) }}"
        } ?: ""

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_iris)
            .setContentTitle(title)
            .setContentText(firstLine + moreSuffix)
            .setSubText("Signal · since baseline")
            .setStyle(inbox)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(NOTIF_ID_SUMMARY, notif)
    }

    fun cancelSummary() {
        nm.cancel(NOTIF_ID_SUMMARY)
    }

    private fun hasPostPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            nm.areNotificationsEnabled()
        }

    private fun android.content.res.Resources.getQuantityStringCompat(
        count: Int,
        singular: String,
        plural: String,
    ): String = if (count == 1) singular.format(count) else plural.format(count)

    companion object {
        const val CHANNEL_ID = "perm_alerts"
        const val NOTIF_ID_SUMMARY = 1001
        private const val MAX_LINES = 6
    }
}
