package app.salat.mobile

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import app.salat.notification.NotificationSoundMode
import kotlin.math.max

class PrayerNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidPrayerNotificationScheduler.ACTION_PRAYER_ALERT) return

        if (
            Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val stableId = intent.getStringExtra(AndroidPrayerNotificationScheduler.EXTRA_STABLE_ID) ?: return
        val rawPrayer = intent.getStringExtra(AndroidPrayerNotificationScheduler.EXTRA_PRAYER_NAME) ?: return
        val prayerAt = intent.getLongExtra(AndroidPrayerNotificationScheduler.EXTRA_PRAYER_AT, 0L)
        val soundMode = runCatching {
            NotificationSoundMode.valueOf(
                intent.getStringExtra(AndroidPrayerNotificationScheduler.EXTRA_SOUND_MODE)
                    ?: NotificationSoundMode.SYSTEM.name
            )
        }.getOrDefault(NotificationSoundMode.SYSTEM)

        val prayerName = localizedPrayerName(context, rawPrayer)
        val remainingMinutes = max(0L, (prayerAt - System.currentTimeMillis()) / 60_000L)
        val body = if (remainingMinutes > 0) {
            context.getString(R.string.prayer_in_minutes, prayerName, remainingMinutes)
        } else {
            context.getString(R.string.prayer_time_now, prayerName)
        }

        val channelId = PrayerNotificationChannels.ensure(context, soundMode)
        val openApp = PendingIntent.getActivity(
            context,
            stableId.hashCode(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(prayerName)
            .setContentText(body)
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setWhen(if (prayerAt > 0L) prayerAt else System.currentTimeMillis())
            .setShowWhen(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(stableId.hashCode(), notification)
    }

    private fun localizedPrayerName(context: Context, rawPrayer: String): String {
        val resource = when (rawPrayer.uppercase()) {
            "FAJR" -> R.string.prayer_fajr
            "SUNRISE" -> R.string.prayer_sunrise
            "DHUHR" -> R.string.prayer_dhuhr
            "ASR" -> R.string.prayer_asr
            "MAGHRIB" -> R.string.prayer_maghrib
            "ISHA" -> R.string.prayer_isha
            else -> return rawPrayer
        }
        return context.getString(resource)
    }
}
