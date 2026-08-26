package app.salat.domain

import app.salat.model.CalculationMethodId
import app.salat.model.CalculationPreferences
import app.salat.model.HighLatitudeRuleId
import app.salat.model.MadhabId
import app.salat.model.PrayerAdjustments
import app.salat.model.PrayerDaySnapshot

/** Stable primitive-oriented entry point for Swift and other native platform code. */
object SalatApi {
    private val engine = SalatEngine()

    fun calculateDaySnapshot(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        timeZoneId: String,
        countryCode: String
    ): PrayerDaySnapshot = engine.calculateDaySnapshot(
        year = year,
        month = month,
        day = day,
        latitude = latitude,
        longitude = longitude,
        timeZoneId = timeZoneId,
        countryCode = countryCode
    )

    fun calculateDaySnapshotConfigured(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        timeZoneId: String,
        countryCode: String,
        methodOverride: String?,
        madhabOverride: String?,
        highLatitudeRule: String,
        fajrAdjustment: Int,
        sunriseAdjustment: Int,
        dhuhrAdjustment: Int,
        asrAdjustment: Int,
        maghribAdjustment: Int,
        ishaAdjustment: Int
    ): PrayerDaySnapshot {
        val preferences = CalculationPreferences(
            methodOverride = enumOrNull<CalculationMethodId>(methodOverride),
            madhabOverride = enumOrNull<MadhabId>(madhabOverride),
            highLatitudeRule = enumOrNull<HighLatitudeRuleId>(highLatitudeRule)
                ?: HighLatitudeRuleId.AUTOMATIC,
            adjustments = PrayerAdjustments(
                fajr = fajrAdjustment,
                sunrise = sunriseAdjustment,
                dhuhr = dhuhrAdjustment,
                asr = asrAdjustment,
                maghrib = maghribAdjustment,
                isha = ishaAdjustment
            )
        )
        return engine.calculateDaySnapshot(
            year = year,
            month = month,
            day = day,
            latitude = latitude,
            longitude = longitude,
            timeZoneId = timeZoneId,
            countryCode = countryCode,
            preferences = preferences
        )
    }

    fun qiblaBearing(latitude: Double, longitude: Double): Double =
        engine.qiblaBearing(latitude, longitude)

    private inline fun <reified T : Enum<T>> enumOrNull(raw: String?): T? =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
}
