package app.salat.domain

import app.salat.location.ManualCity
import app.salat.location.StarterManualCityCatalog
import app.salat.model.CalculationMethodId
import app.salat.model.CalculationPreferences
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

    /** Temporary primitive bridge for iOS until issue #13 replaces the starter catalog. */
    fun searchStarterCitiesEncoded(query: String, limit: Int = 30): String =
        StarterManualCityCatalog.search(query, limit).joinToString("\n") { it.encode() }

    fun starterCityEncoded(id: String): String? =
        StarterManualCityCatalog.byId(id)?.encode()

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

    private fun ManualCity.encode(): String = listOf(
        id,
        name,
        countryCode,
        countryName,
        point.latitude.toString(),
        point.longitude.toString(),
        timeZoneId,
        regionName.orEmpty()
    ).joinToString("\t")

    private inline fun <reified T : Enum<T>> enumOrNull(raw: String?): T? =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
}
