package app.salat.domain

import app.salat.model.GeoPoint
import app.salat.model.PrayerDay
import app.salat.model.PrayerDaySnapshot
import app.salat.model.toSnapshot
import kotlinx.datetime.LocalDate

/** Small facade for platform apps while the repository/verification layer evolves. */
class SalatEngine(
    private val calculator: PrayerCalculator = AdhanPrayerCalculator()
) {
    fun calculateDay(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        timeZoneId: String,
        countryCode: String
    ): PrayerDay {
        val profile = RegionalCalculationProfileResolver.resolve(countryCode)
        return calculator.calculate(
            date = LocalDate(year, month, day),
            point = GeoPoint(latitude, longitude),
            timeZoneId = timeZoneId,
            profile = profile
        )
    }

    fun calculateDaySnapshot(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        timeZoneId: String,
        countryCode: String
    ): PrayerDaySnapshot = calculateDay(
        year, month, day, latitude, longitude, timeZoneId, countryCode
    ).toSnapshot()

    fun qiblaBearing(latitude: Double, longitude: Double): Double =
        QiblaCalculator.bearingDegrees(GeoPoint(latitude, longitude))
}
