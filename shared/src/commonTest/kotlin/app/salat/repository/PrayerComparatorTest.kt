package app.salat.repository

import app.salat.model.PrayerDay
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class PrayerComparatorTest {
    @Test
    fun returns_largest_prayer_delta() {
        fun i(h: Int, m: Int): Instant {
            val hh = h.toString().padStart(2, '0')
            val mm = m.toString().padStart(2, '0')
            return Instant.parse("2026-08-25T${hh}:${mm}:00Z")
        }
        val a = PrayerDay(LocalDate(2026,8,25), i(1,0), i(2,0), i(3,0), i(4,0), i(5,0), i(6,0), "a")
        val b = PrayerDay(LocalDate(2026,8,25), i(1,1), i(2,0), i(3,3), i(4,0), i(5,2), i(6,0), "b")
        assertEquals(3, PrayerComparator.maxDeltaMinutes(a, b))
    }
}
