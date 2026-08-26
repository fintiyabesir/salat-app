package app.salat.domain

import app.salat.model.CalculationMethodId
import app.salat.model.CalculationProfile
import app.salat.model.GeoPoint
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class AdhanPrayerCalculatorTest {
    @Test
    fun istanbul_matches_diyanet_fixture_used_by_adhan() {
        val day = AdhanPrayerCalculator().calculate(
            date = LocalDate(2020, 4, 16),
            point = GeoPoint(41.005616, 28.976380),
            timeZoneId = "Europe/Istanbul",
            profile = CalculationProfile("tr-diyanet", CalculationMethodId.TURKEY)
        )
        val zone = TimeZone.of("Europe/Istanbul")
        fun hm(instant: kotlin.time.Instant): String {
            val t = instant.toLocalDateTime(zone).time
            return t.hour.toString().padStart(2, '0') + ":" + t.minute.toString().padStart(2, '0')
        }
        assertEquals("04:44", hm(day.fajr))
        assertEquals("06:16", hm(day.sunrise))
        assertEquals("13:09", hm(day.dhuhr))
        assertEquals("16:53", hm(day.asr))
        assertEquals("19:52", hm(day.maghrib))
        assertEquals("21:19", hm(day.isha))
    }
}
