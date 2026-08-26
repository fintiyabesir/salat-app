package app.salat.mobile

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.salat.domain.SalatEngine
import app.salat.model.AppPreferences
import app.salat.model.AppearanceMode
import app.salat.model.CalculationPreferences
import app.salat.model.PrayerName
import app.salat.model.ResolvedLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private enum class MainSection { TODAY, CALENDAR, QIBLA }

private val ShellCanvas = Color(0xFFFAF8F3)
private val ShellCanvasDark = Color(0xFF171916)
private val ShellSage = Color(0xFF467A69)
private val ShellCardDark = Color(0xFF242823)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalatMainShell(location: ResolvedLocation) {
    val context = LocalContext.current
    val settingsStore = remember(context) { AndroidAppSettingsStore(context) }
    val notificationCoordinator = remember(context) { AndroidPrayerNotificationCoordinator(context) }
    var settings by remember { mutableStateOf(settingsStore.load()) }
    var section by remember { mutableStateOf(MainSection.TODAY) }
    var showSettings by remember { mutableStateOf(false) }

    val dark = when (settings.appearance) {
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
        AppearanceMode.DARK -> true
        AppearanceMode.LIGHT -> false
    }
    val canvas = if (dark) ShellCanvasDark else ShellCanvas
    val colorScheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF91C9B5),
            background = ShellCanvasDark,
            surface = ShellCardDark,
            onBackground = Color(0xFFF2F1EC),
            onSurface = Color(0xFFF2F1EC)
        )
    } else {
        lightColorScheme(
            primary = ShellSage,
            background = ShellCanvas,
            surface = Color.White,
            onBackground = Color(0xFF20221F),
            onSurface = Color(0xFF20221F)
        )
    }

    fun persist(next: AppPreferences) {
        val languageChanged = next.languageTag != settings.languageTag
        settings = next
        settingsStore.save(next)
        notificationCoordinator.rebuild(location)
        if (languageChanged) {
            AndroidLocaleController.apply(context, next.languageTag)
            (context as? Activity)?.recreate()
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            containerColor = canvas,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.brand_name), color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp) },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Text("⚙", fontSize = 22.sp)
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = canvas) {
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
            Box(Modifier.fillMaxSize().padding(padding).background(canvas)) {
                when (section) {
                    MainSection.TODAY -> AdaptiveTodayScreen(location, settings)
                    MainSection.CALENDAR -> SalatCalendarScreen(location, settings.calculation, dark)
                    MainSection.QIBLA -> SalatQiblaScreen(location)
                }
            }
        }

        if (showSettings) {
            AndroidSettingsSheet(
                location = location,
                value = settings,
                onChange = ::persist,
                onDismiss = { showSettings = false }
            )
        }
    }
}

private data class CalendarDayUi(
    val date: LocalDate,
    val rows: List<Pair<PrayerName, String>>
)

@Composable
private fun SalatCalendarScreen(
    location: ResolvedLocation,
    preferences: CalculationPreferences,
    dark: Boolean
) {
    val locale = LocalConfiguration.current.locales[0]
    val zone = remember(location.timeZoneId) { ZoneId.of(location.timeZoneId) }
    val engine = remember { SalatEngine() }
    val start = remember(location, zone) { LocalDate.now(zone) }
    val days = remember(location, start, preferences, locale) {
        (0L until 30L).map { offset ->
            val date = start.plusDays(offset)
            val day = engine.calculateDay(
                year = date.year,
                month = date.monthValue,
                day = date.dayOfMonth,
                latitude = location.point.latitude,
                longitude = location.point.longitude,
                timeZoneId = location.timeZoneId,
                countryCode = location.countryCode ?: "ZZ",
                preferences = preferences
            )
            CalendarDayUi(
                date = date,
                rows = PrayerName.entries.map { prayer ->
                    prayer to DateTimeFormatter.ofPattern("HH:mm", locale)
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
                    .background(if (dark) ShellCardDark else Color.White.copy(alpha = 0.72f), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Text(
                    item.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)),
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
                        Text(time, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
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
        Text("${qibla.toInt()}°", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
        Spacer(Modifier.height(54.dp))

        Box(
            Modifier
                .background(
                    if (aligned) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(120.dp)
                )
                .padding(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "▲",
                modifier = Modifier.rotate(delta ?: 0f),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 72.sp
            )
        }

        Spacer(Modifier.height(34.dp))
        when {
            !provider.isAvailable -> Text(stringResource(R.string.qibla_compass_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
            heading == null -> Text(stringResource(R.string.qibla_calibrating), color = MaterialTheme.colorScheme.onSurfaceVariant)
            aligned -> Text(stringResource(R.string.qibla_aligned), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            else -> Text("${abs(delta ?: 0f).toInt()}°", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
