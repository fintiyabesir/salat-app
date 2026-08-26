package app.salat.domain

import app.salat.model.CalculationMethodId
import app.salat.model.CalculationProfile
import app.salat.model.GeoPoint
import app.salat.model.MadhabId
import app.salat.model.PrayerDay
import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import kotlinx.datetime.LocalDate

interface PrayerCalculator {
    fun calculate(date: LocalDate, point: GeoPoint, timeZoneId: String, profile: CalculationProfile): PrayerDay
}

class AdhanPrayerCalculator : PrayerCalculator {
    override fun calculate(
        date: LocalDate,
        point: GeoPoint,
        timeZoneId: String,
        profile: CalculationProfile
    ): PrayerDay {
        val parameters = profile.method.toAdhan().parameters.copy(
            madhab = when (profile.madhab) {
                MadhabId.HANAFI -> Madhab.HANAFI
                MadhabId.SHAFI -> Madhab.SHAFI
            },
            prayerAdjustments = PrayerAdjustments(
                fajr = profile.adjustments.fajr,
                sunrise = profile.adjustments.sunrise,
                dhuhr = profile.adjustments.dhuhr,
                asr = profile.adjustments.asr,
                maghrib = profile.adjustments.maghrib,
                isha = profile.adjustments.isha
            )
        )
        val calculated = PrayerTimes(
            Coordinates(point.latitude, point.longitude),
            DateComponents(date.year, date.monthNumber, date.dayOfMonth),
            parameters
        )
        return PrayerDay(
            date = date,
            fajr = calculated.fajr,
            sunrise = calculated.sunrise,
            dhuhr = calculated.dhuhr,
            asr = calculated.asr,
            maghrib = calculated.maghrib,
            isha = calculated.isha,
            calculationProfile = profile.id
        )
    }

    private fun CalculationMethodId.toAdhan(): CalculationMethod = when (this) {
        CalculationMethodId.TURKEY -> CalculationMethod.TURKEY
        CalculationMethodId.MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        CalculationMethodId.EGYPTIAN -> CalculationMethod.EGYPTIAN
        CalculationMethodId.KARACHI -> CalculationMethod.KARACHI
        CalculationMethodId.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA
        CalculationMethodId.DUBAI -> CalculationMethod.DUBAI
        CalculationMethodId.QATAR -> CalculationMethod.QATAR
        CalculationMethodId.KUWAIT -> CalculationMethod.KUWAIT
        CalculationMethodId.MOON_SIGHTING_COMMITTEE -> CalculationMethod.MOON_SIGHTING_COMMITTEE
        CalculationMethodId.SINGAPORE -> CalculationMethod.SINGAPORE
        CalculationMethodId.NORTH_AMERICA -> CalculationMethod.NORTH_AMERICA
    }
}
