package app.salat.domain

import app.salat.model.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QiblaCalculatorTest {
    @Test
    fun istanbul_qibla_is_about_151_point_6_degrees() {
        val bearing = QiblaCalculator.bearingDegrees(GeoPoint(41.005616, 28.976380))
        assertTrue(bearing in 151.5..151.8, "bearing=$bearing")
    }

    @Test
    fun istanbul_is_about_2405_kilometres_from_the_kaaba() {
        // The Qibla design mock prints 2.399 km for this location, but that figure is
        // illustrative rather than computed; the great-circle distance is ~2405 km.
        val km = QiblaCalculator.distanceKilometres(GeoPoint(41.005616, 28.976380))
        assertTrue(km in 2400.0..2410.0, "km=$km")
    }

    @Test
    fun the_kaaba_is_zero_distance_from_itself() {
        assertEquals(0.0, QiblaCalculator.distanceKilometres(GeoPoint(21.422487, 39.826206)), 0.001)
    }

    @Test
    fun antipode_is_about_half_the_earths_circumference() {
        // Guards the haversine against the small-angle formulation that degrades
        // exactly where a great-circle distance is hardest.
        val km = QiblaCalculator.distanceKilometres(GeoPoint(-21.422487, -140.173794))
        assertTrue(km in 20010.0..20040.0, "km=$km")
    }
}
