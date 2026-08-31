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
import app.salat.model.AppPreferences
import app.salat.model.PrayerDay
import app.salat.model.PrayerName
import app.salat.model.ResolvedLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    // Everything after the next prayer is still ahead; once the day is spent the
    // whole strip reads as behind us.
    val reached = if (next.isToday) PrayerName.entries.indexOf(next.prayer) else PrayerName.entries.size

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= 700.dp) {
                Column(Modifier.fillMaxSize().padding(horizontal = 30.dp)) {
                    LocationHeader(location, gregorianDate, hijriDate, dark, onChooseCity, onOpenSettings)
                    Row(
                        Modifier.fillMaxWidth().padding(top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        Box(Modifier.weight(1f)) { HeroCard(next, reached, zone, locale, dark) }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            PrayerList(day, next.prayer.takeIf { next.isToday }, zone, locale, dark) {
                                selectedPrayer = it
                            }
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    LocationHeader(location, gregorianDate, hijriDate, dark, onChooseCity, onOpenSettings)
                    Box(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                        HeroCard(next, reached, zone, locale, dark)
                    }
                    Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 16.dp)) {
                        PrayerList(day, next.prayer.takeIf { next.isToday }, zone, locale, dark) {
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
    onChooseCity: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(location.displayName, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "$gregorianDate · $hijriDate",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderAction(R.drawable.ic_action_search, stringResource(R.string.today_change_city), dark, onChooseCity)
            HeaderAction(R.drawable.ic_action_settings, stringResource(R.string.settings), dark, onOpenSettings)
        }
    }
}

@Composable
private fun HeaderAction(@DrawableRes id: Int, label: String, dark: Boolean, onClick: () -> Unit) {
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
    reached: Int,
    zone: ZoneId,
    locale: Locale,
    dark: Boolean
) {
    val palette = heroPalette(dark)
    val shape = RoundedCornerShape(30.dp)
    CompositionLocalProvider(LocalContentColor provides palette.content) {
        Column(
            Modifier.fillMaxWidth()
                .background(palette.surface, shape)
                .then(palette.border?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                .padding(start = 26.dp, end = 26.dp, top = 26.dp, bottom = 22.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.next_prayer),
                    color = palette.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    // Arabic-script locales join their letters; spacing them breaks the word.
                    letterSpacing = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 0.sp else 1.8.sp
                )
                Text(
                    countdownText(next.epochMillis, locale),
                    color = palette.accent,
                    fontSize = 14.sp,
                    style = Tabular,
                    modifier = Modifier
                        .background(palette.chip, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
            Text(
                next.prayer.adaptiveLabel(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 14.dp)
            )
            Text(
                adaptiveFormat(next.epochMillis, zone, locale),
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraLight,
                style = Tabular
            )
            DayStrip(reached, palette)
        }
    }
}

/**
 * The six prayers laid out as one line of the day, so "where am I in today" is a
 * glance rather than a comparison of six timestamps. The gold marker is the same
 * mark the next prayer carries in the list below.
 */
@Composable
private fun DayStrip(reached: Int, palette: HeroPalette) {
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PrayerName.entries.forEachIndexed { index, _ ->
            if (index > 0) {
                Box(
                    Modifier.weight(1f)
                        .height(2.dp)
                        .background(if (index <= reached) palette.accent else palette.track)
                )
            }
            Box(Modifier.size(21.dp), contentAlignment = Alignment.Center) {
                when {
                    index < reached -> Dot(9.dp, palette.accent)
                    index == reached -> {
                        Dot(21.dp, AwqatGold.copy(alpha = 0.22f))
                        Dot(13.dp, AwqatGold)
                    }
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
                    color = if (index == reached) AwqatGold else palette.trackLabel,
                    fontWeight = if (index == reached) FontWeight.SemiBold else FontWeight.Normal,
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
    nextToday: PrayerName?,
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
        val active = prayer == nextToday
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
