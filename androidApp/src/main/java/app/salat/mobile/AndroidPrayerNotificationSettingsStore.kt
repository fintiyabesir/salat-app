package app.salat.mobile

import android.content.Context
import app.salat.model.PrayerName
import app.salat.notification.NotificationSoundMode
import app.salat.notification.PrayerAlertRule
import app.salat.notification.PrayerNotificationSettings

/** Device-only persistence for notification preferences. No account or backend. */
class AndroidPrayerNotificationSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): PrayerNotificationSettings {
        val rules = PrayerName.entries.associateWith { prayer ->
            val prefix = keyPrefix(prayer)
            val enabled = prefs.getBoolean("$prefix.enabled", false)
            val minutesBefore = prefs.getInt("$prefix.minutesBefore", 0).coerceIn(0, 120)
            val sound = runCatching {
                NotificationSoundMode.valueOf(
                    prefs.getString("$prefix.sound", NotificationSoundMode.SYSTEM.name)
                        ?: NotificationSoundMode.SYSTEM.name
                )
            }.getOrDefault(NotificationSoundMode.SYSTEM)
            PrayerAlertRule(enabled, minutesBefore, sound)
        }
        return PrayerNotificationSettings(rules)
    }

    fun save(settings: PrayerNotificationSettings) {
        prefs.edit().apply {
            PrayerName.entries.forEach { prayer ->
                val rule = settings.rule(prayer)
                val prefix = keyPrefix(prayer)
                putBoolean("$prefix.enabled", rule.enabled)
                putInt("$prefix.minutesBefore", rule.minutesBefore)
                putString("$prefix.sound", rule.soundMode.name)
            }
        }.apply()
    }

    fun hasAnyEnabled(): Boolean = load().rules.values.any { it.enabled }

    private fun keyPrefix(prayer: PrayerName): String = "rule.${prayer.name.lowercase()}"

    companion object {
        private const val PREFS_NAME = "salat-prayer-notifications-v1"
    }
}
