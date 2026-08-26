package app.salat.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.salat.domain.SalatEngine
import app.salat.model.PrayerName
import app.salat.model.ResolvedLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private enum class MainSection { TODAY, CALENDAR, QIBLA }

private val ShellCanvas = Color(0xFFFAF8F3)
private val ShellSage = Color(0xFF467A69)
private val ShellWarm = Color(0xFFF5EEDB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalatMainShell(location: ResolvedLocation) {
    var section by remember { mutableStateOf(MainSection.TODAY) }
    var showSettingsPlaceholder by remember { mutableStateOf(false) }

    MaterialTheme {
        Scaffold(
            containerColor = ShellCanvas,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.brand_name), color = ShellSage, letterSpacing = 2.sp) },
                    actions = {
                        IconButton(onClick = { showSettingsPlaceholder = !showSettingsPlaceholder }) {
                            Text("⚙", fontSize = 22.sp)
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = ShellCanvas) {
                    NavigationBarItem(
                        selected = section == MainSection.TODAY,
                        onClick = { section = MainSection.TODAY },
                        icon = { Text("●") },
                        label = { Text(stringResource(R.string.today)) }
                    )
                    NavigationBarItem(
                        selected = section == MainSection.CALENDAR,
                        onClick = { section = MainSection.CALENDAR },
                        icon = { Text("▦") },
                        label = { Text(stringResource(R.string.calendar)) }
                    )
                    NavigationBarItem(
                        selected = section == MainSection.QIBLA,
                        onClick = { section = MainSection.QIBLA },
                        icon = { Text("⌁") },
                        label = { Text(stringResource(R.string.qibla)) }
                    )
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (section) {
                    MainSection.TODAY -> AdaptiveTodayScreen(location)
                    MainSection.CALENDAR -> SalatCalendarScreen(location)
                    MainSection.QIBLA -> SalatQiblaScreen(location)
                }

                if (showSettingsPlaceholder) {
                    Text(
                        stringResource(R.string.settings),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(ShellWarm, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private data class CalendarDayUi(
    val date: LocalDate,
    val rows: List<Pair<PrayerName, String>>
)

@Composable
private fun SalatCalendarScreen(location: ResolvedLocation) {
    val zone = remember(location.timeZoneId) { ZoneId.of(location.timeZoneId) }
    val engine = remember { SalatEngine() }
    val start = remember(location, zone) { LocalDate.now(zone) }
    val days = remember(location, start) {
        (0L until 30L).map { offset ->
            val date = start.plusDays(offset)
            val day = engine.calculateDay(
                year = date.year,
                month = date.monthValue,
                day = date.dayOfMonth,
                latitude = location.point.latitude,
                longitude = location.point.longitude,
                timeZoneId = location.timeZoneId,
                countryCode = location.countryCode ?: "ZZ"
            )
            CalendarDayUi(
                date = date,
                rows = PrayerName.entries.map { prayer ->
                    prayer to DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                        .withZone(zone)
                        .format(Instant.ofEpochMilli(day.time(prayer).toEpochMilliseconds()))
                }
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(6.dp)) }
        items(days) { item ->
            Column(
                Modifier.fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.72f), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Text(
                    item.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                item.rows.forEach { (prayer, time) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(prayer.localizedLabel())
                        Text(time, color = ShellSage, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SalatQiblaScreen(location: ResolvedLocation) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val provider = remember { AndroidQiblaHeadingProvider(context) }
    val qibla = remember(location) {
        SalatEngine().qiblaBearing(location.point.latitude, location.point.longitude).toFloat()
    }
    var heading by remember { mutableStateOf<Float?>(null) }
    val delta = heading?.let { normalizedDelta(qibla - it) }
    val aligned = delta?.let { abs(it) <= 3f } == true
    var wasAligned by remember { mutableStateOf(false) }

    DisposableEffect(provider) {
        provider.start { heading = it }
        onDispose { provider.stop() }
    }

    LaunchedEffect(aligned) {
        if (aligned && !wasAligned) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasAligned = aligned
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.qibla), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("${qibla.toInt()}°", color = ShellSage, fontSize = 18.sp)
        Spacer(Modifier.height(54.dp))

        Box(
            Modifier
                .background(if (aligned) Color(0xFFE2EFE8) else ShellWarm, RoundedCornerShape(120.dp))
                .padding(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "▲",
                modifier = Modifier.rotate(delta ?: 0f),
                color = ShellSage,
                fontSize = 72.sp
            )
        }

        Spacer(Modifier.height(34.dp))
        when {
            !provider.isAvailable -> Text("Compass sensor unavailable", color = Color(0xFF8A5C4A))
            heading == null -> Text("Calibrating compass…", color = Color(0xFF6D716E))
            aligned -> Text("Aligned with Qibla", color = ShellSage, fontWeight = FontWeight.SemiBold)
            else -> Text("${abs(delta ?: 0f).toInt()}°", color = Color(0xFF6D716E))
        }
    }
}

private fun normalizedDelta(value: Float): Float {
    var result = (value + 180f) % 360f
    if (result < 0) result += 360f
    return result - 180f
}

@Composable
private fun PrayerName.localizedLabel(): String = when (this) {
    PrayerName.FAJR -> stringResource(R.string.prayer_fajr)
    PrayerName.SUNRISE -> stringResource(R.string.prayer_sunrise)
    PrayerName.DHUHR -> stringResource(R.string.prayer_dhuhr)
    PrayerName.ASR -> stringResource(R.string.prayer_asr)
    PrayerName.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    PrayerName.ISHA -> stringResource(R.string.prayer_isha)
}
