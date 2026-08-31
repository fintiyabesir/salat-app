package app.salat.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Swift reaches the Qibla rule through this facade, so the bridge itself needs to
 * hold the rule rather than merely pass numbers along.
 */
class SalatApiQiblaTest {
    private val istanbulLatitude = 41.0082
    private val istanbulLongitude = 28.9784

    @Test
    fun distance_matches_the_calculator() {
        val distance = SalatApi.qiblaDistanceKilometres(istanbulLatitude, istanbulLongitude)
        assertEquals(2404.9, distance, 1.0)
    }

    @Test
    fun a_trusted_reading_comes_back_as_a_signed_turn() {
        val deviation = SalatApi.qiblaDeviationDegrees(
            bearingDegrees = 151.0,
            headingDegrees = 120.0,
            accuracyDegrees = 5.0,
            thresholdDegrees = 10
        )
        assertNotNull(deviation)
        assertEquals(31.0, deviation, 0.001)
    }

    @Test
    fun an_accuracy_worse_than_the_threshold_is_withheld() {
        assertNull(
            SalatApi.qiblaDeviationDegrees(
                bearingDegrees = 151.0,
                headingDegrees = 120.0,
                accuracyDegrees = 25.0,
                thresholdDegrees = 20
            )
        )
    }

    @Test
    fun a_negative_accuracy_is_invalid_not_excellent() {
        // CLHeading uses a negative headingAccuracy to mean "unusable".
        assertNull(
            SalatApi.qiblaDeviationDegrees(
                bearingDegrees = 151.0,
                headingDegrees = 120.0,
                accuracyDegrees = -1.0,
                thresholdDegrees = 10
            )
        )
    }

    @Test
    fun a_fractional_accuracy_rounds_against_the_reading() {
        // 10.4 degrees of error is not within a 10 degree threshold.
        assertNull(
            SalatApi.qiblaDeviationDegrees(
                bearingDegrees = 151.0,
                headingDegrees = 120.0,
                accuracyDegrees = 10.4,
                thresholdDegrees = 10
            )
        )
    }

    @Test
    fun no_heading_is_withheld() {
        assertNull(
            SalatApi.qiblaDeviationDegrees(
                bearingDegrees = 151.0,
                headingDegrees = null,
                accuracyDegrees = 5.0,
                thresholdDegrees = 10
            )
        )
    }
}
