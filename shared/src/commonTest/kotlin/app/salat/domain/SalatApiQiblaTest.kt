package app.salat.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import app.salat.model.AppPreferences

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
    fun the_seed_thresholds_are_inside_the_offered_range() {
        // The settings screen offers these as chips, so a seed outside the range
        // would leave the picker with nothing selected.
        listOf(SalatApi.qiblaThresholdModernDevice, SalatApi.qiblaThresholdOlderDevice)
            .forEach { assertTrue(it in AppPreferences.QIBLA_THRESHOLD_RANGE, "seed=$it") }
        assertTrue(SalatApi.qiblaThresholdModernDevice < SalatApi.qiblaThresholdOlderDevice)
    }

    @Test
    fun iphone_12_and_newer_seed_the_tight_threshold() {
        // iPhone 12 is iPhone13,x — the marketing name runs a year behind.
        listOf("iPhone13,1", "iPhone13,2", "iPhone14,6", "iPhone15,3", "iPhone17,3", "iPhone20,1")
            .forEach {
                assertEquals(SalatApi.qiblaThresholdModernDevice, SalatApi.qiblaSeedThresholdForAppleDevice(it), it)
            }
    }

    @Test
    fun older_iphones_seed_the_looser_threshold() {
        // iPhone12,8 is the 2nd-generation SE — older hardware despite the "12".
        listOf("iPhone12,8", "iPhone12,1", "iPhone11,8", "iPhone10,6", "iPhone8,4")
            .forEach {
                assertEquals(SalatApi.qiblaThresholdOlderDevice, SalatApi.qiblaSeedThresholdForAppleDevice(it), it)
            }
    }

    @Test
    fun anything_unrecognised_errs_towards_showing_a_reading() {
        listOf("iPad14,3", "arm64", "", "iPhone", "iPhoneX", "Watch7,1")
            .forEach {
                assertEquals(SalatApi.qiblaThresholdOlderDevice, SalatApi.qiblaSeedThresholdForAppleDevice(it), "id=$it")
            }
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
