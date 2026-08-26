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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.salat.domain.SalatEngine
import app.salat.model.PrayerDay
import app.salat.model.PrayerName
import app.salat.model.ResolvedLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AdaptiveCanvas = Color(0xFFFAF8F3)
private val AdaptiveSage = Color(0xFF467A69)
private val AdaptiveWarm = Color(0xFFF5EEDB)
private val AdaptiveActiveWarm = Color(0xFFFFF1D8)

@Composable
fun AdaptiveTodayScreen(location: ResolvedLocation) {
    val zone = remember(location.timeZoneId) { ZoneId.of(location.timeZoneId) }
    val today = remember(location, zone) { LocalDate.now(zone) }
    var selectedPrayer by remember { mutableStateOf<PrayerName?>(null) }
    val day = remember(location, today) {
        SalatEngine().calculateDay(
            year = today.year,
            month = today.monthValue,
            day = today.dayOfMonth,
            latitude = location.point.latitude,
            longitude = location.point.longitude,
            timeZoneId = location.timeZoneId,
            countryCode = location.countryCode ?: "ZZ"
        )
    }
    val next = PrayerName.entries.firstOrNull {
        day.time(it).toEpochMilliseconds() > System.currentTimeMillis()
    } ?: PrayerName.FAJR

    Surface(modifier = Modifier.fillMaxSize(), color = AdaptiveCanvas) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= 700.dp) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(34.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        LocationHeader(location, today)
                        Spacer(Modifier.height(32.dp))
                        AdaptiveHero(day, next, zone)
                    }
                    Column(Modifier.weight(1f)) {
                        PrayerList(day, next, zone) { selectedPrayer = it }
                    }
                }
            } else {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                    LocationHeader(location, today)
                    Spacer(Modifier.height(28.dp))
                    AdaptiveHero(day, next, zone)
                    Spacer(Modifier.height(20.dp))
                    PrayerList(day, next, zone) { selectedPrayer = it }
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
private fun LocationHeader(location: ResolvedLocation, today: LocalDate) {
    Text(location.displayName, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    val region = listOfNotNull(location.regionName, location.countryCode).distinct().joinToString(" · ")
    if (region.isNotBlank()) Text(region, color = Color(0xFF6D716E))
    Text(
        today.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())),
        color = Color(0xFF6D716E)
    )
}

@Composable
private fun AdaptiveHero(day: PrayerDay, prayer: PrayerName, zone: ZoneId) {
    Column(
        Modifier.fillMaxWidth()
            .background(AdaptiveWarm, RoundedCornerShape(28.dp))
            .padding(26.dp)
    ) {
        Text(stringResource(R.string.next_prayer), color = AdaptiveSage, fontSize = 12.sp, letterSpacing = 1.2.sp)
        Text(prayer.adaptiveLabel(), fontSize = 26.sp, fontWeight = FontWeight.Medium)
        Text(adaptiveFormat(day, prayer, zone), fontSize = 58.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun PrayerList(
    day: PrayerDay,
    next: PrayerName,
    zone: ZoneId,
    onPrayer: (PrayerName) -> Unit
) {
    PrayerName.entries.forEach { prayer ->
        val active = prayer == next
        Row(
            Modifier.fillMaxWidth()
                .background(if (active) AdaptiveActiveWarm else Color.Transparent, RoundedCornerShape(16.dp))
                .clickable { onPrayer(prayer) }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(prayer.adaptiveLabel(), fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
            Text(
                adaptiveFormat(day, prayer, zone),
                color = if (active) AdaptiveSage else Color.Unspecified,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

private fun adaptiveFormat(day: PrayerDay, prayer: PrayerName, zone: ZoneId): String =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        .withZone(zone)
        .format(Instant.ofEpochMilli(day.time(prayer).toEpochMilliseconds()))

@Composable
private fun PrayerName.adaptiveLabel(): String = when (this) {
    PrayerName.FAJR -> stringResource(R.string.prayer_fajr)
    PrayerName.SUNRISE -> stringResource(R.string.prayer_sunrise)
    PrayerName.DHUHR -> stringResource(R.string.prayer_dhuhr)
    PrayerName.ASR -> stringResource(R.string.prayer_asr)
    PrayerName.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    PrayerName.ISHA -> stringResource(R.string.prayer_isha)
}
