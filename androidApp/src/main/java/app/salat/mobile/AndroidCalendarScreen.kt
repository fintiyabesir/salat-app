package app.salat.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.salat.domain.SalatEngine
import app.salat.model.AppPreferences
import app.salat.model.PrayerName
import app.salat.model.ResolvedLocation
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val CALENDAR_DAYS = 30L
private val DateCellWidth = 70.dp
private val Tabular = TextStyle(fontFeatureSettings = "tnum")

private data class CalendarRow(
    val date: LocalDate,
    val label: String,
    val times: List<String>,
    val isFriday: Boolean
)

/**
 * Artboard 2e. Today is pinned above a table that scrolls under it, so the day you
 * are actually in never leaves the screen while you look ahead.
 */
@Composable
internal fun AndroidCalendarScreen(
    location: ResolvedLocation,
    settings: AppPreferences,
    dark: Boolean,
    onOpenSettings: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val zone = remember(location.timeZoneId) { ZoneId.of(location.timeZoneId) }
    val today = remember(location, zone) { LocalDate.now(zone) }
    val engine = remember { SalatEngine() }

    fun timesFor(date: LocalDate): List<String> {
        val day = engine.calculateDay(
            year = date.year,
            month = date.monthValue,
            day = date.dayOfMonth,
            latitude = location.point.latitude,
            longitude = location.point.longitude,
            timeZoneId = location.timeZoneId,
            countryCode = location.countryCode ?: "ZZ",
            preferences = settings.calculation
        )
        return PrayerName.entries.map { prayer ->
            DateTimeFormatter.ofPattern("HH:mm", locale)
                .withZone(zone)
                .format(Instant.ofEpochMilli(day.time(prayer).toEpochMilliseconds()))
        }
    }

    val todayTimes = remember(location, today, settings.calculation, locale) { timesFor(today) }
    val rows = remember(location, today, settings.calculation, locale) {
        (1L..CALENDAR_DAYS).map { offset ->
            val date = today.plusDays(offset)
            CalendarRow(
                date = date,
                label = date.format(DateTimeFormatter.ofPattern("d EEE", locale)),
                times = timesFor(date),
                isFriday = date.dayOfWeek == DayOfWeek.FRIDAY
            )
        }
    }
    val hijriToday = remember(today, zone, locale, settings.hijriMethod, settings.hijriDayAdjustment) {
        AndroidHijriFormatter.format(today, zone, locale, settings.hijriMethod, settings.hijriDayAdjustment, "d MMMM")
    }
    val hijriMonth = remember(today, zone, locale, settings.hijriMethod, settings.hijriDayAdjustment) {
        AndroidHijriFormatter.format(today, zone, locale, settings.hijriMethod, settings.hijriDayAdjustment, "MMMM y")
    }
    val gregorianRange = remember(today, locale) {
        val last = today.plusDays(CALENDAR_DAYS)
        val month = DateTimeFormatter.ofPattern("MMMM", locale)
        val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
        if (today.month == last.month) today.format(monthYear)
        else "${today.format(month)}–${last.format(monthYear)}"
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.calendar), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "$gregorianRange · $hijriMonth",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            HeaderAction(
                R.drawable.ic_action_settings,
                stringResource(R.string.settings),
                dark,
                onOpenSettings
            )
        }
        TodayCard(today, hijriToday, todayTimes, dark, locale)
        BoxWithConstraints(Modifier.weight(1f)) {
            val wide = maxWidth >= 700.dp
            Column(
                Modifier.fillMaxSize()
                    .padding(horizontal = if (wide) 30.dp else 22.dp)
                    .padding(top = 14.dp, bottom = 8.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp)
            ) {
                // Kept outside the list rather than as a sticky item: it is the table's
                // legend, and it must never scroll away from the column it names.
                MonthTableHeader(dark)
                LazyColumn(contentPadding = PaddingValues(bottom = 14.dp)) {
                    items(rows, key = { it.date }) { row -> MonthTableRow(row, dark) }
                }
            }
        }
    }
}

@Composable
private fun TodayCard(
    today: LocalDate,
    hijri: String,
    times: List<String>,
    dark: Boolean,
    locale: Locale
) {
    val palette = heroPalette(dark)
    val shape = RoundedCornerShape(24.dp)
    CompositionLocalProvider(LocalContentColor provides palette.content) {
        Column(
            Modifier.fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 16.dp)
                .background(palette.surface, shape)
                .then(palette.border?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    stringResource(
                        R.string.calendar_today_card,
                        today.format(DateTimeFormatter.ofPattern("d MMMM", locale))
                    ),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(hijri, fontSize = 13.sp, color = palette.accent)
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                PrayerName.entries.forEachIndexed { index, prayer ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            prayer.adaptiveLabel(),
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = palette.trackLabel
                        )
                        Text(
                            times[index],
                            fontSize = 15.sp,
                            style = Tabular,
                            maxLines = 1,
                            color = if (prayer == PrayerName.SUNRISE) AwqatGold else palette.content,
                            fontWeight = if (prayer == PrayerName.SUNRISE) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthTableHeader(dark: Boolean) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(DateCellWidth))
            PrayerName.entries.forEach { prayer ->
                Text(
                    prayer.shortLabel(),
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = if (dark) Color(0xFF6D716E) else Color(0xFF9AA09A)
                )
            }
        }
        Divider(dark)
    }
}

@Composable
private fun MonthTableRow(row: CalendarRow, dark: Boolean) {
    // Friday is the one day of the week that carries an obligation the others do
    // not, so it gets the gold mark the design reserves for "look here".
    val fridayTint = if (dark) AwqatGold.copy(alpha = 0.10f) else Color(0xFFFBF7EE)
    val dateColor = when {
        row.isFriday && dark -> AwqatGold
        row.isFriday -> Color(0xFFB08544)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier.fillMaxWidth()
            .then(if (row.isFriday) Modifier.background(fridayTint, RoundedCornerShape(10.dp)) else Modifier)
            .padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.width(DateCellWidth), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.label,
                fontSize = 13.5.sp,
                maxLines = 1,
                color = dateColor,
                fontWeight = if (row.isFriday) FontWeight.SemiBold else FontWeight.Normal
            )
            if (row.isFriday) {
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(6.dp).background(AwqatGold, CircleShape))
            }
        }
        row.times.forEach { time ->
            Text(
                time,
                modifier = Modifier.weight(1f),
                fontSize = 13.5.sp,
                style = Tabular,
                maxLines = 1,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    if (!row.isFriday) Divider(dark)
}

@Composable
private fun Divider(dark: Boolean) {
    Box(
        Modifier.fillMaxWidth()
            .height(1.dp)
            .background(if (dark) Color(0xFF2C3A33) else Color(0xFFF0EEE7))
    )
}
