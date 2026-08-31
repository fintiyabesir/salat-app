package app.salat.mobile

import android.icu.text.SimpleDateFormat
import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone as IcuTimeZone
import android.icu.util.ULocale
import app.salat.model.HijriCalendarMethodId
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

object AndroidHijriFormatter {
    fun format(
        date: LocalDate,
        zoneId: ZoneId,
        locale: Locale,
        method: HijriCalendarMethodId,
        dayAdjustment: Int,
        /** ICU pattern; the calendar header wants a month alone, not a full date. */
        pattern: String = "d MMMM y"
    ): String {
        val adjustedDate = date.plusDays(dayAdjustment.coerceIn(-2, 2).toLong())
        val instant = adjustedDate.atTime(12, 0).atZone(zoneId).toInstant()
        val icuZone = IcuTimeZone.getTimeZone(zoneId.id)
        val calendar = IslamicCalendar(icuZone, locale).apply {
            calculationType = when (method) {
                HijriCalendarMethodId.AUTOMATIC,
                HijriCalendarMethodId.UMM_AL_QURA -> IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
                HijriCalendarMethodId.TABULAR -> IslamicCalendar.CalculationType.ISLAMIC_CIVIL
            }
            time = Date.from(instant)
        }
        // SimpleDateFormat binds its month names to the calendar it is constructed
        // with. Assigning .calendar afterwards swaps the arithmetic but keeps the
        // Gregorian symbols, which rendered Hijri dates as "17 March 1448". Building
        // the formatter from a locale that carries the calendar keyword makes ICU
        // load the Islamic month names.
        val icuLocale = ULocale.forLocale(locale).setKeywordValue(
            "calendar",
            when (method) {
                HijriCalendarMethodId.TABULAR -> "islamic-civil"
                else -> "islamic-umalqura"
            }
        )
        return SimpleDateFormat(pattern, icuLocale).apply {
            this.calendar = calendar
            timeZone = icuZone
        }.format(Date.from(instant))
    }
}
