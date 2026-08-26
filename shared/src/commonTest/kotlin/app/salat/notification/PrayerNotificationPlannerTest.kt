package app.salat.notification

import app.salat.model.PrayerDay
import app.salat.model.PrayerName
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

class PrayerNotificationPlannerTest {
    @Test
    fun default_plan_excludes_sunrise_and_never_schedules_past_triggers() {
        val day = prayerDay()
        val alerts = PrayerNotificationPlanner.plan(
            days = listOf(day),
            settings = PrayerNotificationSettings(),
            now = Instant.parse("2026-08-26T02:30:00Z")
        )

        assertFalse(alerts.any { it.prayer == PrayerName.SUNRISE })
        assertFalse(alerts.any { it.triggerAt <= Instant.parse("2026-08-26T02:30:00Z") })
    }

    @Test
    fun before_time_offset_changes_trigger_but_not_prayer_time() {
        val settings = PrayerNotificationSettings(
            rules = mapOf(
                PrayerName.MAGHRIB to PrayerAlertRule(
                    enabled = true,
                    minutesBefore = 15,
                    soundMode = NotificationSoundMode.SHORT_ADHAN
                )
            )
        )
        val alert = PrayerNotificationPlanner.plan(
            listOf(prayerDay()), settings, Instant.parse("2026-08-26T00:00:00Z")
        ).single()

        assertEquals(PrayerName.MAGHRIB, alert.prayer)
        assertEquals(Instant.parse("2026-08-26T16:45:00Z"), alert.triggerAt)
        assertEquals(Instant.parse("2026-08-26T17:00:00Z"), alert.prayerAt)
        assertEquals("2026-08-26:maghrib", alert.stableId)
    }

    private fun prayerDay(): PrayerDay = PrayerDay(
        date = LocalDate(2026, 8, 26),
        fajr = Instant.parse("2026-08-26T02:00:00Z"),
        sunrise = Instant.parse("2026-08-26T03:30:00Z"),
        dhuhr = Instant.parse("2026-08-26T10:00:00Z"),
        asr = Instant.parse("2026-08-26T14:00:00Z"),
        maghrib = Instant.parse("2026-08-26T17:00:00Z"),
        isha = Instant.parse("2026-08-26T18:30:00Z"),
        calculationProfile = "test"
    )
}
