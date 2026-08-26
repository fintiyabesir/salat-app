package app.salat.repository

import app.salat.model.PrayerDay
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class PrayerCacheTest {
    @Test
    fun same_source_and_date_are_isolated_by_location_key() = kotlinx.coroutines.test.runTest {
        val cache = InMemoryPrayerCache()
        val date = LocalDate(2026, 8, 26)
        val istanbul = day(date, "istanbul")
        val ankara = day(date, "ankara")

        cache.putOfficial("diyanet", "istanbul", listOf(istanbul))
        cache.putOfficial("diyanet", "ankara", listOf(ankara))

        assertEquals("istanbul", cache.official("diyanet", "istanbul", date)?.calculationProfile)
        assertEquals("ankara", cache.official("diyanet", "ankara", date)?.calculationProfile)
        assertNull(cache.official("diyanet", "izmir", date))
    }

    private fun day(date: LocalDate, profile: String): PrayerDay {
        val t = Instant.parse("2026-08-26T00:00:00Z")
        return PrayerDay(date, t, t, t, t, t, t, profile)
    }
}
