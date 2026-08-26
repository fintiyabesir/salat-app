package app.salat.domain

import app.salat.model.CalculationMethodId
import app.salat.model.CalculationProfile
import app.salat.model.GeoPoint
import app.salat.model.MadhabId
import app.salat.model.PrayerDay
import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerAdjustments as AdhanPrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.batoulapps.adhan2.model.Rounding
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
        val parameters = profile.method.toAdhanParameters().copy(
            madhab = when (profile.madhab) {
                MadhabId.HANAFI -> Madhab.HANAFI
                MadhabId.SHAFI -> Madhab.SHAFI
            },
            prayerAdjustments = AdhanPrayerAdjustments(
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

    private fun CalculationMethodId.toAdhanParameters(): CalculationParameters = when (this) {
        CalculationMethodId.MALAYSIA -> CalculationParameters(
            fajrAngle = 18.0,
            ishaAngle = 18.0,
            method = CalculationMethod.OTHER,
            methodAdjustments = AdhanPrayerAdjustments(dhuhr = 1),
            rounding = Rounding.UP
        )
        CalculationMethodId.TURKEY -> CalculationMethod.TURKEY.parameters
        CalculationMethodId.MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        CalculationMethodId.EGYPTIAN -> CalculationMethod.EGYPTIAN.parameters
        CalculationMethodId.KARACHI -> CalculationMethod.KARACHI.parameters
        CalculationMethodId.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA.parameters
        CalculationMethodId.DUBAI -> CalculationMethod.DUBAI.parameters
        CalculationMethodId.QATAR -> CalculationMethod.QATAR.parameters
        CalculationMethodId.KUWAIT -> CalculationMethod.KUWAIT.parameters
        CalculationMethodId.MOON_SIGHTING_COMMITTEE -> CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters
        CalculationMethodId.SINGAPORE -> CalculationMethod.SINGAPORE.parameters
        CalculationMethodId.NORTH_AMERICA -> CalculationMethod.NORTH_AMERICA.parameters
    }
}
