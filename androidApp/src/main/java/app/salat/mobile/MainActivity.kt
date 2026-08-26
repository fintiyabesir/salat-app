package app.salat.mobile

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resolver = AndroidLocationResolver(this)
        val notificationCoordinator = AndroidPrayerNotificationCoordinator(this)
        setContent { SalatApp(resolver, notificationCoordinator) }
    }
}

private val Canvas = Color(0xFFFAF8F3)
private val Sage = Color(0xFF467A69)
private val Warm = Color(0xFFF5EEDB)
private val ActiveWarm = Color(0xFFFFF1D8)

@Composable
private fun SalatApp(
    resolver: AndroidLocationResolver,
    notificationCoordinator: AndroidPrayerNotificationCoordinator
) {
    var location by remember { mutableStateOf<ResolvedLocation?>(null) }
    var resolving by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf(false) }

    fun resolve() {
        resolving = true
        locationError = false
        resolver.resolve { resolved ->
            location = resolved
            resolving = false
            locationError = resolved == null
            if (resolved != null) notificationCoordinator.rebuild(resolved)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) resolve() else locationError = true
    }

    LaunchedEffect(Unit) {
        if (resolver.hasPermission()) resolve()
    }

    if (location == null) {
        LocationStartScreen(
            resolving = resolving,
            showError = locationError,
            onUseLocation = {
                if (resolver.hasPermission()) resolve()
                else permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        )
    } else {
        SalatMainShell(requireNotNull(location))
    }
}

@Composable
private fun LocationStartScreen(
    resolving: Boolean,
    showError: Boolean,
    onUseLocation: () -> Unit
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
            Column(
                Modifier.padding(horizontal = 26.dp, vertical = 52.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.brand_name), color = Sage, fontSize = 14.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.location_title), fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.location_privacy), color = Color(0xFF6D716E), lineHeight = 22.sp)
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = onUseLocation,
                    enabled = !resolving,
                    colors = ButtonDefaults.buttonColors(containerColor = Sage),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (resolving) CircularProgressIndicator(color = Color.White)
                    else Text(stringResource(R.string.use_current_location))
                }
                if (showError) {
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.location_unavailable), color = Color(0xFF9A5B45), lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
fun SalatTodayScreen(location: ResolvedLocation) {
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
    val nowMillis = System.currentTimeMillis()
    val next = PrayerName.entries.firstOrNull { prayer -> day.time(prayer).toEpochMilliseconds() > nowMillis }
        ?: PrayerName.FAJR

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                Text(location.displayName, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                val regionLabel = listOfNotNull(location.regionName, location.countryCode).distinct().joinToString(" · ")
                if (regionLabel.isNotBlank()) Text(regionLabel, color = Color(0xFF6D716E))
                Text(
                    today.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())),
                    color = Color(0xFF6D716E)
                )
                Spacer(Modifier.height(28.dp))
                NextPrayerCard(day, next, zone)
                Spacer(Modifier.height(20.dp))
                PrayerName.entries.forEach { prayer ->
                    PrayerRow(
                        name = prayer.label(),
                        time = format(day, prayer, zone),
                        active = prayer == next,
                        onClick = { selectedPrayer = prayer }
                    )
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
}

@Composable
private fun NextPrayerCard(day: PrayerDay, prayer: PrayerName, zone: ZoneId) {
    Column(Modifier.fillMaxWidth().background(Warm, RoundedCornerShape(28.dp)).padding(24.dp)) {
        Text(stringResource(R.string.next_prayer), color = Sage, fontSize = 12.sp, letterSpacing = 1.2.sp)
        Text(prayer.label(), fontSize = 25.sp, fontWeight = FontWeight.Medium)
        Text(format(day, prayer, zone), fontSize = 54.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun PrayerRow(name: String, time: String, active: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (active) ActiveWarm else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
        Text(time, color = if (active) Sage else Color.Unspecified, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
    }
}

private fun format(day: PrayerDay, prayer: PrayerName, zone: ZoneId): String {
    val javaInstant = Instant.ofEpochMilli(day.time(prayer).toEpochMilliseconds())
    return DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).withZone(zone).format(javaInstant)
}

@Composable
private fun PrayerName.label(): String = when (this) {
    PrayerName.FAJR -> stringResource(R.string.prayer_fajr)
    PrayerName.SUNRISE -> stringResource(R.string.prayer_sunrise)
    PrayerName.DHUHR -> stringResource(R.string.prayer_dhuhr)
    PrayerName.ASR -> stringResource(R.string.prayer_asr)
    PrayerName.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    PrayerName.ISHA -> stringResource(R.string.prayer_isha)
}
