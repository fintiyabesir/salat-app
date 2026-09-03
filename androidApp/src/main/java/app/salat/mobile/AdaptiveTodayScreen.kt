package app.salat.mobile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import app.salat.domain.SalatEngine
import app.salat.domain.dayTimes
import app.salat.domain.KerahatWindow
import app.salat.domain.KerahatId
import app.salat.domain.DayStatusCalculator
import app.salat.domain.DayStatus
import app.salat.domain.DayPeriodId
import app.salat.model.AppPreferences
import app.salat.model.PrayerDay
import app.salat.model.PrayerName
import app.salat.model.ResolvedLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Bridges the screen's java.time date to the shared calculator's plain integers. */
private fun SalatEngine.dayTimes(
    location: ResolvedLocation,
    date: LocalDate,
    preferences: app.salat.model.CalculationPreferences
) = dayTimes(
    year = date.year,
    month = date.monthValue,
    day = date.dayOfMonth,
    latitude = location.point.latitude,
    longitude = location.point.longitude,
    timeZoneId = location.timeZoneId,
    countryCode = location.countryCode ?: "ZZ",
    preferences = preferences
)

private data class NextPrayerUi(
    val prayer: PrayerName,
    val epochMillis: Long,
    val isToday: Boolean
)

/** Times are columns of digits people compare down the screen, so they never reflow. */
private val Tabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun AdaptiveTodayScreen(
    location: ResolvedLocation,
    settings: AppPreferences,
    dark: Boolean,
    onChooseCity: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val zone = remember(location.timeZoneId) { ZoneId.of(location.timeZoneId) }
    val today = remember(location, zone) { LocalDate.now(zone) }
    val engine = remember { SalatEngine() }
    var selectedPrayer by remember { mutableStateOf<PrayerName?>(null) }
    val day = remember(location, today, settings.calculation) {
        engine.calculateDay(
            year = today.year,
            month = today.monthValue,
            day = today.dayOfMonth,
            latitude = location.point.latitude,
            longitude = location.point.longitude,
            timeZoneId = location.timeZoneId,
            countryCode = location.countryCode ?: "ZZ",
            preferences = settings.calculation
        )
    }
    val nowMillis = System.currentTimeMillis()
    // Isha runs past midnight and the small hours still belong to it, so the
    // surrounding days are not optional.
    val status = remember(location, today, settings.calculation, settings.kerahatMinutes, nowMillis / 60_000L) {
        DayStatusCalculator.evaluate(
            nowMillis = nowMillis,
            today = engine.dayTimes(location, today, settings.calculation),
            yesterday = engine.dayTimes(location, today.minusDays(1L), settings.calculation),
            tomorrow = engine.dayTimes(location, today.plusDays(1L), settings.calculation),
            kerahatMinutes = settings.kerahatMinutes
        )
    }
    val next = PrayerName.entries.firstOrNull {
        day.time(it).toEpochMilliseconds() > nowMillis
    }?.let { prayer ->
        NextPrayerUi(prayer, day.time(prayer).toEpochMilliseconds(), isToday = true)
    } ?: run {
        val tomorrow = today.plusDays(1)
        val tomorrowDay = engine.calculateDay(
            year = tomorrow.year,
            month = tomorrow.monthValue,
            day = tomorrow.dayOfMonth,
            latitude = location.point.latitude,
            longitude = location.point.longitude,
            timeZoneId = location.timeZoneId,
            countryCode = location.countryCode ?: "ZZ",
            preferences = settings.calculation
        )
        NextPrayerUi(
            prayer = PrayerName.FAJR,
            epochMillis = tomorrowDay.time(PrayerName.FAJR).toEpochMilliseconds(),
            isToday = false
        )
    }
    val gregorianDate = remember(today, locale) {
        today.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
    }
    val hijriDate = remember(today, zone, locale, settings.hijriMethod, settings.hijriDayAdjustment) {
        AndroidHijriFormatter.format(
            date = today,
            zoneId = zone,
            locale = locale,
            method = settings.hijriMethod,
            dayAdjustment = settings.hijriDayAdjustment
        )
    }
    // The marked prayer is the one whose window is open, not the next instant to
    // arrive: before sunrise you are still inside Fajr, and marking sunrise made
    // the screen read as though the sun had already risen.
    val current = status.period.id.prayer

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // A short viewport — a phone on its side — cannot carry the design's
            // 72sp clock, so the hero steps down rather than being cut off. The whole
            // page scrolls either way; nothing is clipped to make room.
            val short = maxHeight < 520.dp
            if (maxWidth >= 700.dp) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 30.dp)) {
                    LocationHeader(location, gregorianDate, hijriDate, dark, short, onChooseCity, onOpenSettings)
                    Row(
                        Modifier.fillMaxWidth().padding(top = if (short) 12.dp else 20.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        Box(Modifier.weight(1f)) { HeroCard(next, status, day, current, zone, locale, dark, short) }
                        Column(Modifier.weight(1f)) {
                            PrayerList(day, current, zone, locale, dark) {
                                selectedPrayer = it
                            }
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    LocationHeader(location, gregorianDate, hijriDate, dark, short, onChooseCity, onOpenSettings)
                    Box(Modifier.padding(horizontal = 22.dp, vertical = if (short) 12.dp else 20.dp)) {
                        HeroCard(next, status, day, current, zone, locale, dark, short)
                    }
                    Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 16.dp)) {
                        PrayerList(day, current, zone, locale, dark) {
                            selectedPrayer = it
                        }
                    }
                }
            }
        }
    }

    selectedPrayer?.let { prayer ->
        PrayerNotificationSettingsSheet(
            prayer = prayer,
            location = location,
            onDismiss = { selectedPrayer = null }
        )
    }
}

@Composable
private fun LocationHeader(
    location: ResolvedLocation,
    gregorianDate: String,
    hijriDate: String,
    dark: Boolean,
    short: Boolean,
    onChooseCity: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = if (short) 12.dp else 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                location.displayName,
                fontSize = if (short) 22.sp else 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "$gregorianDate · $hijriDate",
                fontSize = if (short) 13.sp else 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = if (short) 2.dp else 5.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderAction(R.drawable.ic_action_search, stringResource(R.string.today_change_city), dark, onChooseCity)
            HeaderAction(R.drawable.ic_action_settings, stringResource(R.string.settings), dark, onOpenSettings)
        }
    }
}

@Composable
internal fun HeaderAction(@DrawableRes id: Int, label: String, dark: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (dark) ShellCardDark else Color.White,
        shadowElevation = if (dark) 0.dp else 3.dp,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(id), contentDescription = label, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun HeroCard(
    next: NextPrayerUi,
    status: DayStatus,
    day: PrayerDay,
    current: PrayerName?,
    zone: ZoneId,
    locale: Locale,
    dark: Boolean,
    short: Boolean
) {
    val kerahat = status.kerahat
    val palette = heroPalette(dark, kerahat != null)
    val shape = RoundedCornerShape(30.dp)
    CompositionLocalProvider(LocalContentColor provides palette.content) {
        Column(
            Modifier.fillMaxWidth()
                .background(palette.surface, shape)
                .then(palette.border?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                .padding(
                    start = 26.dp,
                    end = 26.dp,
                    top = if (short) 14.dp else 26.dp,
                    bottom = if (short) 12.dp else 22.dp
                )
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Which window you are standing in, not merely what comes next: the
                // countdown is the same number either way, but only one of them is
                // the question people are actually asking.
                Text(
                    if (kerahat != null) stringResource(R.string.kerahat_label)
                    else stringResource(R.string.period_now),
                    color = palette.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    // Arabic-script locales join their letters; spacing them breaks the word.
                    letterSpacing = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 0.sp else 1.8.sp
                )
                // The next prayer is supporting detail now, so it takes the chip the
                // countdown used to sit in.
                Row(
                    Modifier
                        .background(palette.chip, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(next.prayer.adaptiveLabel(), color = palette.accent, fontSize = 13.sp)
                    Text(
                        adaptiveFormat(next.epochMillis, zone, locale),
                        color = palette.accent,
                        fontSize = 13.sp,
                        style = Tabular
                    )
                }
            }
            // The card answers one question: which window is open, and how much of it
            // is left. Everything else on it is support.
            Text(
                kerahat?.label() ?: status.period.id.label(),
                fontSize = if (short) 20.sp else 28.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(top = if (short) 6.dp else 12.dp)
            )
            val remaining = countdownText(kerahat?.endMillis ?: status.period.endMillis, locale)
            Text(
                remaining,
                // Nine languages write durations at very different widths, so the size
                // follows the string rather than trusting one number to fit them all.
                fontSize = when {
                    short -> 36.sp
                    remaining.length <= 8 -> 56.sp
                    remaining.length <= 11 -> 46.sp
                    else -> 38.sp
                },
                fontWeight = FontWeight.ExtraLight,
                maxLines = 1,
                style = Tabular
            )
            DayStrip(day, current, palette, short)
        }
    }
}

/**
 * The six prayers laid out as one line of the day, so "where am I in today" is a
 * glance rather than a comparison of six timestamps. The gold marker is the same
 * mark the next prayer carries in the list below.
 */
@Composable
private fun DayStrip(day: PrayerDay, current: PrayerName?, palette: HeroPalette, short: Boolean) {
    val nowMillis = System.currentTimeMillis()
    fun elapsed(prayer: PrayerName) = day.time(prayer).toEpochMilliseconds() <= nowMillis
    Row(
        Modifier.fillMaxWidth().padding(top = if (short) 10.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PrayerName.entries.forEachIndexed { index, prayer ->
            if (index > 0) {
                Box(
                    Modifier.weight(1f)
                        .height(2.dp)
                        .background(if (elapsed(prayer)) palette.accent else palette.track)
                )
            }
            Box(Modifier.size(21.dp), contentAlignment = Alignment.Center) {
                when {
                    prayer == current -> {
                        Dot(21.dp, AwqatGold.copy(alpha = 0.22f))
                        Dot(13.dp, AwqatGold)
                    }
                    elapsed(prayer) -> Dot(9.dp, palette.accent)
                    else -> Dot(9.dp, palette.track)
                }
            }
        }
    }
    // The labels mirror the dot row's own spacing so each one sits under its mark;
    // distributing them evenly instead drifts the outer two off their dots.
    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        PrayerName.entries.forEachIndexed { index, prayer ->
            if (index > 0) Spacer(Modifier.weight(1f))
            Box(Modifier.width(21.dp), contentAlignment = Alignment.Center) {
                Text(
                    prayer.shortLabel(),
                    fontSize = 11.sp,
                    maxLines = 1,
                    color = if (prayer == current) AwqatGold else palette.trackLabel,
                    fontWeight = if (prayer == current) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.wrapContentWidth(unbounded = true)
                )
            }
        }
    }
}

@Composable
private fun Dot(size: androidx.compose.ui.unit.Dp, color: Color) {
    Box(Modifier.size(size).background(color, CircleShape))
}

/**
 * Live countdown to the next prayer. Both widgets already showed remaining time and
 * the phone did not, which is the number people actually look for.
 */
@Composable
private fun countdownText(epochMillis: Long, locale: Locale): String {
    val remaining by produceState(initialValue = epochMillis - System.currentTimeMillis(), epochMillis) {
        while (true) {
            value = epochMillis - System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val total = (remaining / 1000L).coerceAtLeast(0L)
    val hours = (total / 3600L).toInt()
    val minutes = ((total % 3600L) / 60L).toInt()
    val seconds = (total % 60L).toInt()
    return if (hours > 0) {
        stringResource(R.string.countdown_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.countdown_minutes_seconds, minutes, seconds)
    }
}

@Composable
private fun PrayerList(
    day: PrayerDay,
    current: PrayerName?,
    zone: ZoneId,
    locale: Locale,
    dark: Boolean,
    onPrayer: (PrayerName) -> Unit
) {
    val nowMillis = System.currentTimeMillis()
    val activeShape = RoundedCornerShape(18.dp)
    val spent = if (dark) Color(0xFF6D716E) else Color(0xFF9AA09A)
    val activeContent = if (dark) Color(0xFF91C9B5) else AwqatHeroSurface
    PrayerName.entries.forEach { prayer ->
        val active = prayer == current
        val passed = !active && day.time(prayer).toEpochMilliseconds() <= nowMillis
        val content = when {
            active -> activeContent
            passed -> spent
            else -> MaterialTheme.colorScheme.onBackground
        }
        Row(
            Modifier.fillMaxWidth()
                .padding(bottom = 4.dp)
                .then(
                    if (active) {
                        Modifier.background(MaterialTheme.colorScheme.surface, activeShape)
                            .then(
                                if (dark) Modifier.border(1.dp, Color(0xFF2C3A33), activeShape)
                                else Modifier
                            )
                    } else {
                        Modifier
                    }
                )
                .clickable { onPrayer(prayer) }
                .padding(horizontal = 16.dp, vertical = if (active) 15.dp else 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (active) {
                    Dot(8.dp, AwqatGold)
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    prayer.adaptiveLabel(),
                    fontSize = 18.sp,
                    color = content,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            Text(
                adaptiveFormat(day.time(prayer).toEpochMilliseconds(), zone, locale),
                fontSize = 18.sp,
                color = content,
                style = Tabular,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

private fun adaptiveFormat(epochMillis: Long, zone: ZoneId, locale: Locale): String =
    DateTimeFormatter.ofPattern("HH:mm", locale)
        .withZone(zone)
        .format(Instant.ofEpochMilli(epochMillis))

@Composable
private fun DayPeriodId.label(): String = stringResource(
    when (this) {
        DayPeriodId.FAJR -> R.string.period_fajr
        DayPeriodId.DUHA -> R.string.period_duha
        DayPeriodId.DHUHR -> R.string.period_dhuhr
        DayPeriodId.ASR -> R.string.period_asr
        DayPeriodId.MAGHRIB -> R.string.period_maghrib
        DayPeriodId.ISHA -> R.string.period_isha
    }
)

@Composable
private fun KerahatWindow.label(): String = stringResource(
    when (id) {
        KerahatId.SUNRISE -> R.string.kerahat_sunrise
        KerahatId.ZENITH -> R.string.kerahat_zenith
        KerahatId.SUNSET -> R.string.kerahat_sunset
    }
)

@Composable
internal fun PrayerName.adaptiveLabel(): String = when (this) {
    PrayerName.FAJR -> stringResource(R.string.prayer_fajr)
    PrayerName.SUNRISE -> stringResource(R.string.prayer_sunrise)
    PrayerName.DHUHR -> stringResource(R.string.prayer_dhuhr)
    PrayerName.ASR -> stringResource(R.string.prayer_asr)
    PrayerName.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    PrayerName.ISHA -> stringResource(R.string.prayer_isha)
}

@Composable
internal fun PrayerName.shortLabel(): String = when (this) {
    PrayerName.FAJR -> stringResource(R.string.prayer_fajr_short)
    PrayerName.SUNRISE -> stringResource(R.string.prayer_sunrise_short)
    PrayerName.DHUHR -> stringResource(R.string.prayer_dhuhr_short)
    PrayerName.ASR -> stringResource(R.string.prayer_asr_short)
    PrayerName.MAGHRIB -> stringResource(R.string.prayer_maghrib_short)
    PrayerName.ISHA -> stringResource(R.string.prayer_isha_short)
}
