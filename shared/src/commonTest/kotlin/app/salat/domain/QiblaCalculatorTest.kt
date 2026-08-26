package app.salat.domain

import app.salat.model.GeoPoint
import kotlin.test.Test
import kotlin.test.assertTrue

class QiblaCalculatorTest {
    @Test
    fun istanbul_qibla_is_about_151_point_6_degrees() {
        val bearing = QiblaCalculator.bearingDegrees(GeoPoint(41.005616, 28.976380))
        assertTrue(bearing in 151.5..151.8, "bearing=$bearing")
    }
}
