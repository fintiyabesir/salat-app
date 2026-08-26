package app.salat.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun AdaptiveTodayScreen(
    location: ResolvedLocation,
    settings: AppPreferences = AppPreferences()
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
        today.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= 700.dp) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(34.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        LocationHeader(location, gregorianDate, hijriDate)
                        Spacer(Modifier.height(32.dp))
                        AdaptiveHero(next, zone, locale)
                    }
                    Column(Modifier.weight(1f)) {
                        PrayerList(day, next.prayer.takeIf { next.isToday }, zone, locale) { selectedPrayer = it }
                    }
                }
            } else {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                    LocationHeader(location, gregorianDate, hijriDate)
                    Spacer(Modifier.height(28.dp))
                    AdaptiveHero(next, zone, locale)
                    Spacer(Modifier.height(20.dp))
                    PrayerList(day, next.prayer.takeIf { next.isToday }, zone, locale) { selectedPrayer = it }
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
private fun LocationHeader(location: ResolvedLocation, gregorianDate: String, hijriDate: String) {
    Text(location.displayName, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    val region = listOfNotNull(location.regionName, location.countryCode).distinct().joinToString(" · ")
    if (region.isNotBlank()) Text(region, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(gregorianDate, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(hijriDate, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
}

@Composable
private fun AdaptiveHero(next: NextPrayerUi, zone: ZoneId, locale: Locale) {
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
            .padding(26.dp)
    ) {
        Text(
            stringResource(R.string.next_prayer),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp
        )
        Text(next.prayer.adaptiveLabel(), fontSize = 26.sp, fontWeight = FontWeight.Medium)
        Text(adaptiveFormat(next.epochMillis, zone, locale), fontSize = 58.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun PrayerList(
    day: PrayerDay,
    nextToday: PrayerName?,
    zone: ZoneId,
    locale: Locale,
    onPrayer: (PrayerName) -> Unit
) {
    PrayerName.entries.forEach { prayer ->
        val active = prayer == nextToday
        Row(
            Modifier.fillMaxWidth()
                .background(
                    if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    RoundedCornerShape(16.dp)
                )
                .clickable { onPrayer(prayer) }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(prayer.adaptiveLabel(), fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
            Text(
                adaptiveFormat(day.time(prayer).toEpochMilliseconds(), zone, locale),
                color = if (active) MaterialTheme.colorScheme.primary else Color.Unspecified,
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
private fun PrayerName.adaptiveLabel(): String = when (this) {
    PrayerName.FAJR -> stringResource(R.string.prayer_fajr)
    PrayerName.SUNRISE -> stringResource(R.string.prayer_sunrise)
    PrayerName.DHUHR -> stringResource(R.string.prayer_dhuhr)
    PrayerName.ASR -> stringResource(R.string.prayer_asr)
    PrayerName.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    PrayerName.ISHA -> stringResource(R.string.prayer_isha)
}
