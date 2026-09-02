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
     * Seed values each platform picks between when a device is first set up. There
     * is deliberately no "automatic" mode: the threshold is always a concrete number
     * the user can see and override, because a hidden rule is impossible to reason
     * about when the Qibla will not appear.
     */
    val qiblaThresholdModernDevice: Int
        get() = AppPreferences.QIBLA_THRESHOLD_FUSED

    val qiblaThresholdOlderDevice: Int
        get() = AppPreferences.QIBLA_THRESHOLD_MAGNETOMETER_ONLY

    /**
     * The seed for an Apple device, from its hardware identifier.
     *
     * iPhone 12 is `iPhone13,x` — Apple's internal numbering runs a year ahead of
     * the marketing name, so "iPhone 12 or newer" means a major of 13 or above.
     * Anything unrecognised (an iPad, a future naming scheme) is treated as older,
     * which errs towards showing a reading rather than hiding one.
     *
     * The rule lives here rather than in Swift so it is pinned by tests.
     */
    fun qiblaSeedThresholdForAppleDevice(modelIdentifier: String): Int {
        val major = modelIdentifier
            .takeIf { it.startsWith(APPLE_PHONE_PREFIX) }
            ?.removePrefix(APPLE_PHONE_PREFIX)
            ?.takeWhile { it.isDigit() }
            ?.toIntOrNull()
        return if (major != null && major >= FIRST_MODERN_IPHONE_MAJOR) {
            qiblaThresholdModernDevice
        } else {
            qiblaThresholdOlderDevice
        }
    }

    private const val APPLE_PHONE_PREFIX = "iPhone"
    private const val FIRST_MODERN_IPHONE_MAJOR = 13

    /**
     * The three kerahat windows of one day, flattened for Swift: id, start and end
     * repeated three times. The phone writes these into the glance payload so the
     * widget and the watch never have to reimplement the rule.
     */
    fun kerahatWindowsEncoded(
        sunriseMillis: Long,
        dhuhrMillis: Long,
        maghribMillis: Long,
        minutes: Int
    ): List<String> = DayStatusCalculator.windows(
        DayTimes(
            fajr = 0L,
            sunrise = sunriseMillis,
            dhuhr = dhuhrMillis,
            asr = 0L,
            maghrib = maghribMillis,
            isha = 0L
        ),
        minutes
    ).map { "${it.id.name}\t${it.startMillis}\t${it.endMillis}" }

    val kerahatDefaultMinutes: Int get() = DayStatusCalculator.DEFAULT_KERAHAT_MINUTES

    private inline fun <reified T : Enum<T>> enumOrNull(raw: String?): T? =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
}
