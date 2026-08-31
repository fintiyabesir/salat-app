package app.salat.domain

import app.salat.model.AppPreferences
import app.salat.model.CalculationMethodId
import app.salat.model.CalculationPreferences
import app.salat.model.GeoPoint
import app.salat.model.HighLatitudeRuleId
import app.salat.model.MadhabId
import app.salat.model.PrayerAdjustments
import app.salat.model.PrayerDaySnapshot
import app.salat.verification.OfficialSourceReferenceResolver

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

    /**
     * Encoded as sourceId<TAB>displayName<TAB>integrationStatus. This is reference
     * metadata only and never implies that today's prayer values were verified.
     */
    fun officialSourceReferenceEncoded(countryCode: String): String? =
        OfficialSourceReferenceResolver.resolve(countryCode)?.let { source ->
            listOf(source.sourceId, source.displayName, source.status.name).joinToString("\t")
        }

    fun qiblaBearing(latitude: Double, longitude: Double): Double =
        engine.qiblaBearing(latitude, longitude)

    fun qiblaDistanceKilometres(latitude: Double, longitude: Double): Double =
        QiblaCalculator.distanceKilometres(GeoPoint(latitude, longitude))

    /**
     * The signed turn to face the Kaaba, or null when the reading may not be shown.
     * Swift goes through here rather than reimplementing the rule, so both platforms
     * are bound by the same tests.
     */
    fun qiblaDeviationDegrees(
        bearingDegrees: Double,
        headingDegrees: Double?,
        accuracyDegrees: Double?,
        thresholdDegrees: Int
    ): Double? = when (
        val display = QiblaDirectionPolicy.evaluate(
            bearingDegrees = bearingDegrees.toFloat(),
            headingDegrees = headingDegrees?.toFloat(),
            // CLHeading reports degrees; a negative value means the reading is invalid.
            accuracyDegrees = accuracyDegrees?.takeIf { it >= 0.0 }?.let { kotlin.math.ceil(it).toInt() },
            thresholdDegrees = thresholdDegrees
        )
    ) {
        is QiblaDisplay.Direction -> display.deviationDegrees.toDouble()
        QiblaDisplay.Hidden -> null
    }

    /**
     * Platforms that report compass accuracy as an angle rather than a coarse level
     * can hold the tight threshold; Android picks its own from the sensor set.
     */
    val qiblaDefaultThresholdDegrees: Int
        get() = AppPreferences.QIBLA_THRESHOLD_FUSED

    private inline fun <reified T : Enum<T>> enumOrNull(raw: String?): T? =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
}
