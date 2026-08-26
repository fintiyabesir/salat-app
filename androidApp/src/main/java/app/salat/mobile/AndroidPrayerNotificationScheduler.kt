package app.salat.mobile

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import app.salat.notification.NotificationSoundMode
import app.salat.notification.ScheduledPrayerAlert

/**
 * Native Android delivery for alerts already planned by the shared domain.
 *
 * Permission policy is intentionally explicit:
 * - this class never requests POST_NOTIFICATIONS by itself;
 * - exact-alarm special access is exposed as an Intent for a user-initiated settings flow;
 * - when exact access is unavailable, alerts fall back to setAndAllowWhileIdle().
 */
class AndroidPrayerNotificationScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    fun exactAlarmAccessIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarms.canScheduleExactAlarms()) {
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            null
        }

    /**
     * Replace every Salat-owned pending alarm with a newly calculated plan. This is
     * important when a prayer is disabled: cancelling only the new IDs would leave
     * the previously enabled prayer alarm alive on the device.
     */
    fun reschedule(alerts: List<ScheduledPrayerAlert>) {
        cancel(storedIds())
        val scheduled = alerts.filter(::schedule).mapTo(mutableSetOf()) { it.stableId }
        preferences.edit().putStringSet(KEY_SCHEDULED_IDS, scheduled).apply()
    }

    /** Returns true only when a future alarm was actually registered. */
    fun schedule(alert: ScheduledPrayerAlert): Boolean {
        val triggerAt = alert.triggerAt.toEpochMilliseconds()
        if (triggerAt <= System.currentTimeMillis()) return false

        val pendingIntent = pendingIntent(alert, PendingIntent.FLAG_UPDATE_CURRENT)
        if (canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
        return true
    }

    fun cancel(stableIds: Collection<String>) {
        stableIds.forEach { id ->
            val pending = PendingIntent.getBroadcast(
                context,
                requestCode(id),
                Intent(context, PrayerNotificationReceiver::class.java).setAction(ACTION_PRAYER_ALERT),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return@forEach
            alarms.cancel(pending)
            pending.cancel()
        }
    }

    fun cancelAll() {
        cancel(storedIds())
        preferences.edit().remove(KEY_SCHEDULED_IDS).apply()
    }

    private fun storedIds(): Set<String> =
        preferences.getStringSet(KEY_SCHEDULED_IDS, emptySet())?.toSet().orEmpty()

    private fun pendingIntent(alert: ScheduledPrayerAlert, updateFlag: Int): PendingIntent {
        val intent = Intent(context, PrayerNotificationReceiver::class.java)
            .setAction(ACTION_PRAYER_ALERT)
            .putExtra(EXTRA_STABLE_ID, alert.stableId)
            .putExtra(EXTRA_PRAYER_NAME, alert.prayer.name)
            .putExtra(EXTRA_PRAYER_AT, alert.prayerAt.toEpochMilliseconds())
            .putExtra(EXTRA_SOUND_MODE, alert.soundMode.name)

        return PendingIntent.getBroadcast(
            context,
            requestCode(alert.stableId),
            intent,
            updateFlag or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(stableId: String): Int = stableId.hashCode()

    companion object {
        const val ACTION_PRAYER_ALERT = "app.salat.mobile.PRAYER_ALERT"
        const val EXTRA_STABLE_ID = "stable_id"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_PRAYER_AT = "prayer_at"
        const val EXTRA_SOUND_MODE = "sound_mode"

        private const val PREFERENCES = "salat-prayer-notifications"
        private const val KEY_SCHEDULED_IDS = "scheduled-ids"
    }
}

internal object PrayerNotificationChannels {
    private const val SILENT = "prayer-silent"
    private const val SYSTEM = "prayer-system"
    private const val ADHAN = "prayer-short-adhan"

    fun ensure(context: Context, mode: NotificationSoundMode): String {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelId = when (mode) {
            NotificationSoundMode.SILENT -> SILENT
            NotificationSoundMode.SYSTEM -> SYSTEM
            NotificationSoundMode.SHORT_ADHAN -> ADHAN
        }
        if (manager.getNotificationChannel(channelId) != null) return channelId

        val channel = NotificationChannel(
            channelId,
            when (mode) {
                NotificationSoundMode.SILENT -> "Prayer reminders · silent"
                NotificationSoundMode.SYSTEM -> "Prayer reminders"
                NotificationSoundMode.SHORT_ADHAN -> "Prayer reminders · short adhan"
            },
            NotificationManager.IMPORTANCE_HIGH
        )

        when (mode) {
            NotificationSoundMode.SILENT -> channel.setSound(null, null)
            NotificationSoundMode.SYSTEM -> Unit
            NotificationSoundMode.SHORT_ADHAN -> {
                val rawId = context.resources.getIdentifier("adhan_short", "raw", context.packageName)
                if (rawId != 0) {
                    val soundUri = Uri.parse("android.resource://${context.packageName}/$rawId")
                    val audio = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                    channel.setSound(soundUri, audio)
                }
            }
        }
        manager.createNotificationChannel(channel)
        return channelId
    }
}
