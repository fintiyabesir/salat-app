package app.salat.model

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
    val appearance: AppearanceMode = AppearanceMode.SYSTEM
) {
    init {
        require(hijriDayAdjustment in -2..2) { "Hijri day adjustment must be between -2 and +2" }
    }
}
