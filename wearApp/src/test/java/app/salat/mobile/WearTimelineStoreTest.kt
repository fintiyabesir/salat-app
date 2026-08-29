package app.salat.mobile

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class WearTimelineStoreTest {
    @Test
    fun `selects the next prayer from persisted rolling timeline`() {
        val timeline = WearPrayerTimeline(
            generatedAtMillis = 1722500000000,
            locationName = "Istanbul",
            timeZoneId = "Europe/Istanbul",
            events = listOf(
                WearPrayerEvent("FAJR", 1722505000000),
                WearPrayerEvent("DHUHR", 1722510000000)
            )
        )

        assertEquals("DHUHR", timeline.next(1722507000000)?.prayerId)
    }

    @Test
    fun `events on uses the synced location timezone`() {
        val firstDay = Instant.parse("2026-08-29T20:30:00Z").toEpochMilli()
        val nextDay = Instant.parse("2026-08-29T22:30:00Z").toEpochMilli()
        val timeline = WearPrayerTimeline(
            generatedAtMillis = firstDay,
            locationName = "Istanbul",
            timeZoneId = "Europe/Istanbul",
            events = listOf(
                WearPrayerEvent("ISHA", firstDay),
                WearPrayerEvent("FAJR", nextDay)
            )
        )

        assertEquals(listOf("ISHA"), timeline.eventsOn(firstDay).map { it.prayerId })
        assertEquals(listOf("FAJR"), timeline.eventsOn(nextDay).map { it.prayerId })
    }
}
