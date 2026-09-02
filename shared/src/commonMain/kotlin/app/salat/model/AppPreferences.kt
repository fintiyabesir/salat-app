package app.salat.model

import app.salat.domain.DayStatusCalculator

enum class AppearanceMode { SYSTEM, LIGHT, DARK }

enum class HijriCalendarMethodId {
    AUTOMATIC,
    UMM_AL_QURA,
    TABULAR
}

data class AppPreferences(
    val calculation: CalculationPreferences = CalculationPreferences(),
    val hijriMethod: HijriCalendarMethodId = HijriCalendarMethodId.AUTOMATIC,
    val hijriDayAdjustment: Int = 0,
    val languageTag: String? = null,
    val appearance: AppearanceMode = AppearanceMode.SYSTEM,
    /**
     * Below this compass accuracy the Qibla direction is hidden rather than shown
     * wrong. Null means "decide from the hardware": a device with a gyroscope gets
     * a sensor-fused heading and can hold the tighter default, one without is
     * magnetometer-only and needs the looser one.
     */
    val qiblaAccuracyThresholdDegrees: Int? = null,
    /**
     * How long each kerahat window runs, or null to leave them out entirely. The
     * app names the window and when it ends; it does not rule on what may be prayed,
     * because that varies by school and is not an app's to decide.
     */
    val kerahatMinutes: Int? = DayStatusCalculator.DEFAULT_KERAHAT_MINUTES
) {
    init {
        require(hijriDayAdjustment in -2..2) { "Hijri day adjustment must be between -2 and +2" }
        kerahatMinutes?.let {
            require(it in DayStatusCalculator.KERAHAT_MINUTES_RANGE) {
                "Kerahat minutes must be within ${DayStatusCalculator.KERAHAT_MINUTES_RANGE}"
            }
        }
        qiblaAccuracyThresholdDegrees?.let {
            require(it in QIBLA_THRESHOLD_RANGE) { "Qibla accuracy threshold must be within $QIBLA_THRESHOLD_RANGE" }
        }
    }

    companion object {
        val QIBLA_THRESHOLD_RANGE = 5..45
        /** Gyroscope present: the heading is sensor-fused and holds this. */
        const val QIBLA_THRESHOLD_FUSED = 10
        /** Magnetometer only: noisier, so the bar for trusting it is lower. */
        const val QIBLA_THRESHOLD_MAGNETOMETER_ONLY = 20
    }
}
