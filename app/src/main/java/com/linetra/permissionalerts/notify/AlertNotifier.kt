package com.linetra.permissionalerts.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.linetra.permissionalerts.MainActivity
import com.linetra.permissionalerts.R
import com.linetra.permissionalerts.data.FlaggedApp
import com.linetra.permissionalerts.data.SensitivePermissions

class AlertNotifier(private val context: Context) {

    private val nm: NotificationManagerCompat = NotificationManagerCompat.from(context)

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sys = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // The HIGH-importance channel allows heads-up. We only fire heads-up on count
            // escalation; ordinary updates are silenced via setOnlyAlertOnce(true).
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notif_channel_alerts_desc)
                setShowBadge(true)
            }
            sys.createNotificationChannel(channel)
            // Tidy up the legacy DEFAULT-importance channel from older builds.
            sys.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
    }

    /**
     * Re-render the persistent summary. Fires a heads-up only when [flagged].size is greater
     * than [previousCount] — calm refreshes don't interrupt the user.
     */
    fun updateSummary(flagged: List<FlaggedApp>, previousCount: Int) {
        if (!hasPostPermission()) return
        if (flagged.isEmpty()) {
            nm.cancel(NOTIF_ID_SUMMARY)
            return
        }

        val isEscalation = flagged.size > previousCount
        if (isEscalation) {
            // Cancelling first makes the next notify a "new" post, so the channel's HIGH
            // importance triggers heads-up. Plain updates would be suppressed silently.
            nm.cancel(NOTIF_ID_SUMMARY)
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
            putExtra(MainActivity.EXTRA_FROM_ALERT, true)
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
            .setLargeIcon(rasterizeIris())
            .setColor(ACCENT_COLOR)
            .setColorized(false)
            .setContentTitle(title)
            .setContentText(firstLine + moreSuffix)
            .setSubText("Signal · since baseline")
            .setStyle(inbox)
            .setOngoing(true)
            .setOnlyAlertOnce(!isEscalation)
            .setContentIntent(pi)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(NOTIF_ID_SUMMARY, notif)
    }

    fun cancelSummary() {
        nm.cancel(NOTIF_ID_SUMMARY)
    }

    private fun rasterizeIris(): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_iris_mark) ?: return null
        val sizePx = (LARGE_ICON_DP * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(Canvas(bitmap))
        return bitmap
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
        const val CHANNEL_ID = "perm_alerts_v2"
        private const val LEGACY_CHANNEL_ID = "perm_alerts"
        const val NOTIF_ID_SUMMARY = 1001
        private const val MAX_LINES = 6
        private const val LARGE_ICON_DP = 64
        // accentC from HoloDark — iridescent violet, used for setColor tint.
        private const val ACCENT_COLOR = 0xFF8F8CFF.toInt()
    }
}
