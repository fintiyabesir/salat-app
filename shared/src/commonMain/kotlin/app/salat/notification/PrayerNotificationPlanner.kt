package app.salat.notification

import app.salat.model.PrayerDay
import app.salat.model.PrayerName
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

enum class NotificationSoundMode { SILENT, SYSTEM, SHORT_ADHAN }

data class PrayerAlertRule(
    val enabled: Boolean = false,
    /** 0 = at prayer time; positive values mean before the prayer. */
    val minutesBefore: Int = 0,
    val soundMode: NotificationSoundMode = NotificationSoundMode.SYSTEM
) {
    init { require(minutesBefore in 0..120) { "minutesBefore must be between 0 and 120" } }
}

data class PrayerNotificationSettings(
    val rules: Map<PrayerName, PrayerAlertRule> = defaultRules()
) {
    fun rule(prayer: PrayerName): PrayerAlertRule = rules[prayer] ?: PrayerAlertRule()

    companion object {
        fun defaultRules(): Map<PrayerName, PrayerAlertRule> = PrayerName.entries.associateWith { prayer ->
            PrayerAlertRule(enabled = prayer != PrayerName.SUNRISE)
        }
    }
}

data class ScheduledPrayerAlert(
    val prayer: PrayerName,
    val prayerAt: Instant,
    val triggerAt: Instant,
    val soundMode: NotificationSoundMode,
    val stableId: String
)

object PrayerNotificationPlanner {
    fun plan(
        days: List<PrayerDay>,
        settings: PrayerNotificationSettings,
        now: Instant
    ): List<ScheduledPrayerAlert> = buildList {
        days.sortedBy { it.date }.forEach { day ->
            PrayerName.entries.forEach { prayer ->
                val rule = settings.rule(prayer)
                if (!rule.enabled) return@forEach
                val prayerAt = day.time(prayer)
                val triggerAt = prayerAt - rule.minutesBefore.minutes
                if (triggerAt > now) {
                    add(
                        ScheduledPrayerAlert(
                            prayer = prayer,
                            prayerAt = prayerAt,
                            triggerAt = triggerAt,
                            soundMode = rule.soundMode,
                            stableId = "${day.date}:${prayer.name.lowercase()}"
                        )
                    )
                }
            }
        }
    }.sortedBy { it.triggerAt }
}
