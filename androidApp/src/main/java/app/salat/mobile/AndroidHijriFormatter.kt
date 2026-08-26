package app.salat.mobile

import android.icu.text.SimpleDateFormat
import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone as IcuTimeZone
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
        dayAdjustment: Int
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
        return SimpleDateFormat("d MMMM y", locale).apply {
            this.calendar = calendar
            timeZone = icuZone
        }.format(Date.from(instant))
    }
}
