package app.salat.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class QiblaDirectionPolicyTest {
    private fun evaluate(
        bearing: Float = 151f,
        heading: Float? = 151f,
        accuracy: Int? = 5,
        threshold: Int = 10
    ) = QiblaDirectionPolicy.evaluate(bearing, heading, accuracy, threshold)

    @Test
    fun a_reading_within_the_threshold_is_shown() {
        val display = evaluate(bearing = 151f, heading = 120f)
        assertIs<QiblaDisplay.Direction>(display)
        assertEquals(31f, display.deviationDegrees, 0.001f)
    }

    @Test
    fun accuracy_exactly_at_the_threshold_is_still_trusted() {
        assertIs<QiblaDisplay.Direction>(evaluate(accuracy = 10, threshold = 10))
    }

    @Test
    fun accuracy_worse_than_the_threshold_hides_the_direction() {
        assertEquals(QiblaDisplay.Hidden, evaluate(accuracy = 11, threshold = 10))
    }

    @Test
    fun an_unreported_accuracy_is_never_treated_as_good() {
        // The sensor not having spoken yet must not read as "fine so far".
        assertEquals(QiblaDisplay.Hidden, evaluate(accuracy = null))
    }

    @Test
    fun no_heading_hides_the_direction() {
        assertEquals(QiblaDisplay.Hidden, evaluate(heading = null))
    }

    @Test
    fun a_loosened_threshold_admits_a_reading_a_tight_one_rejects() {
        assertEquals(QiblaDisplay.Hidden, evaluate(accuracy = 15, threshold = 10))
        assertIs<QiblaDisplay.Direction>(evaluate(accuracy = 15, threshold = 20))
    }

    @Test
    fun turning_the_short_way_never_exceeds_180_degrees() {
        // Facing 350 with the Qibla at 10 is a 20 degree turn right, not 340 left.
        val display = evaluate(bearing = 10f, heading = 350f)
        assertIs<QiblaDisplay.Direction>(display)
        assertEquals(20f, display.deviationDegrees, 0.001f)
    }

    @Test
    fun the_opposite_bearing_resolves_to_a_single_signed_value() {
        val display = evaluate(bearing = 0f, heading = 180f)
        assertIs<QiblaDisplay.Direction>(display)
        assertEquals(180f, display.deviationDegrees, 0.001f)
    }

    @Test
    fun normalization_covers_several_full_turns() {
        listOf(-720f, -360f, 0f, 360f, 720f).forEach { angle ->
            assertEquals(0f, QiblaDirectionPolicy.normalizedDelta(angle), 0.001f, "angle=$angle")
        }
        assertTrue(QiblaDirectionPolicy.normalizedDelta(1000f) in -180f..180f)
    }
}
