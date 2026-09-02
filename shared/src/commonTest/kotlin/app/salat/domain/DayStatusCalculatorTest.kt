package app.salat.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val HOUR = 3_600_000L
private const val MINUTE = 60_000L

/** A plain day: Fajr 05:00, sunrise 06:30, Dhuhr 13:00, Asr 16:30, Maghrib 19:30, Isha 21:00. */
private fun day(offsetDays: Long = 0): DayTimes {
    val midnight = offsetDays * 24 * HOUR
    return DayTimes(
        fajr = midnight + 5 * HOUR,
        sunrise = midnight + 6 * HOUR + 30 * MINUTE,
        dhuhr = midnight + 13 * HOUR,
        asr = midnight + 16 * HOUR + 30 * MINUTE,
        maghrib = midnight + 19 * HOUR + 30 * MINUTE,
        isha = midnight + 21 * HOUR
    )
}

private fun statusAt(millis: Long, kerahatMinutes: Int? = 45) = DayStatusCalculator.evaluate(
    nowMillis = millis,
    today = day(0),
    yesterday = day(-1),
    tomorrow = day(1),
    kerahatMinutes = kerahatMinutes
)

class DayStatusCalculatorTest {
    @Test
    fun each_window_is_named_by_the_prayer_that_opens_it() {
        assertEquals(DayPeriodId.FAJR, statusAt(5 * HOUR + 30 * MINUTE).period.id)
        assertEquals(DayPeriodId.DHUHR, statusAt(14 * HOUR).period.id)
        assertEquals(DayPeriodId.ASR, statusAt(17 * HOUR).period.id)
        assertEquals(DayPeriodId.MAGHRIB, statusAt(20 * HOUR).period.id)
        assertEquals(DayPeriodId.ISHA, statusAt(22 * HOUR).period.id)
    }

    @Test
    fun sunrise_closes_fajr_rather_than_opening_a_window() {
        // Nothing obligatory occupies the stretch between sunrise and Dhuhr.
        assertEquals(DayPeriodId.DUHA, statusAt(8 * HOUR).period.id)
        val fajr = statusAt(6 * HOUR).period
        assertEquals(DayPeriodId.FAJR, fajr.id)
        assertEquals(day().sunrise, fajr.endMillis)
    }

    @Test
    fun the_small_hours_still_belong_to_yesterdays_isha() {
        val period = statusAt(2 * HOUR).period
        assertEquals(DayPeriodId.ISHA, period.id)
        assertEquals(day(-1).isha, period.startMillis)
        assertEquals(day(0).fajr, period.endMillis)
    }

    @Test
    fun isha_runs_to_the_next_fajr_not_to_midnight() {
        val period = statusAt(23 * HOUR).period
        assertEquals(DayPeriodId.ISHA, period.id)
        assertEquals(day(1).fajr, period.endMillis)
    }

    @Test
    fun remaining_time_counts_to_the_end_of_the_window() {
        val period = statusAt(14 * HOUR).period
        assertEquals(2 * HOUR + 30 * MINUTE, period.remainingMillis(14 * HOUR))
        // Never negative, so a late tick cannot render a minus sign.
        assertEquals(0L, period.remainingMillis(99 * HOUR))
    }

    @Test
    fun the_three_kerahat_windows_hang_off_the_sun_not_the_prayers() {
        val windows = DayStatusCalculator.windows(day(), 45)
        assertEquals(listOf(KerahatId.SUNRISE, KerahatId.ZENITH, KerahatId.SUNSET), windows.map { it.id })
        assertEquals(day().sunrise, windows[0].startMillis)
        assertEquals(day().sunrise + 45 * MINUTE, windows[0].endMillis)
        assertEquals(day().dhuhr - 45 * MINUTE, windows[1].startMillis)
        assertEquals(day().dhuhr, windows[1].endMillis)
        assertEquals(day().maghrib - 45 * MINUTE, windows[2].startMillis)
        assertEquals(day().maghrib, windows[2].endMillis)
    }

    @Test
    fun being_inside_a_window_is_reported_with_its_end() {
        val status = statusAt(6 * HOUR + 45 * MINUTE)
        val kerahat = assertNotNull(status.kerahat)
        assertEquals(KerahatId.SUNRISE, kerahat.id)
        assertEquals(day().sunrise + 45 * MINUTE, kerahat.endMillis)
    }

    @Test
    fun a_window_ends_exclusively_so_dhuhr_is_not_still_kerahat() {
        // At exactly Dhuhr the zenith window has closed and the prayer has opened.
        assertNull(statusAt(day().dhuhr).kerahat)
        assertNotNull(statusAt(day().dhuhr - MINUTE).kerahat)
    }

    @Test
    fun kerahat_overlays_the_period_rather_than_replacing_it() {
        // The sunset window sits inside the Asr period; both facts stay true.
        val status = statusAt(19 * HOUR)
        assertEquals(DayPeriodId.ASR, status.period.id)
        assertEquals(KerahatId.SUNSET, assertNotNull(status.kerahat).id)
    }

    @Test
    fun turning_kerahat_off_reports_no_window() {
        assertNull(statusAt(6 * HOUR + 45 * MINUTE, kerahatMinutes = null).kerahat)
    }

    @Test
    fun a_custom_duration_moves_both_edges_that_depend_on_it() {
        val windows = DayStatusCalculator.windows(day(), 20)
        assertEquals(day().sunrise + 20 * MINUTE, windows[0].endMillis)
        assertEquals(day().dhuhr - 20 * MINUTE, windows[1].startMillis)
    }

    @Test
    fun an_out_of_range_duration_is_clamped_rather_than_trusted() {
        val windows = DayStatusCalculator.windows(day(), 600)
        assertEquals(day().sunrise + 60 * MINUTE, windows[0].endMillis)
        assertTrue(windows[1].startMillis < day().dhuhr)
    }
}
