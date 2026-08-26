package app.salat.domain

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

    fun qiblaBearing(latitude: Double, longitude: Double): Double =
        engine.qiblaBearing(latitude, longitude)
}
