package app.salat.domain

/** What the Qibla screen is allowed to show for the current compass reading. */
sealed interface QiblaDisplay {
    /** Trustworthy: [deviationDegrees] is the signed turn needed, in (-180, 180]. */
    data class Direction(val deviationDegrees: Float) : QiblaDisplay

    /** Not trustworthy: the direction and every derived degree must be withheld. */
    data object Hidden : QiblaDisplay
}

/**
 * Decides whether a compass reading may be shown as a Qibla direction.
 *
 * The approved design states the rule plainly: below the accuracy threshold the
 * needle and the degrees are hidden entirely, because a wrong Qibla is worse than
 * no Qibla. This lives in the shared module so the rule is pinned by tests rather
 * than by whichever platform screen happens to implement it.
 */
object QiblaDirectionPolicy {
    fun evaluate(
        bearingDegrees: Float,
        headingDegrees: Float?,
        accuracyDegrees: Int?,
        thresholdDegrees: Int
    ): QiblaDisplay {
        if (headingDegrees == null) return QiblaDisplay.Hidden
        // An unreported accuracy is not an optimistic one.
        if (accuracyDegrees == null || accuracyDegrees > thresholdDegrees) return QiblaDisplay.Hidden
        return QiblaDisplay.Direction(normalizedDelta(bearingDegrees - headingDegrees))
    }

    /** Wraps a signed angle into (-180, 180] so "turn left" never becomes a 350° turn. */
    fun normalizedDelta(value: Float): Float {
        var delta = value % 360f
        if (delta > 180f) delta -= 360f
        if (delta <= -180f) delta += 360f
        return delta
    }
}
