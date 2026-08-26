package app.salat.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.salat.domain.SalatEngine
import app.salat.model.PrayerDay
import app.salat.model.PrayerName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SalatTodayScreen() }
    }
}

private val Canvas = Color(0xFFFAF8F3)
private val Sage = Color(0xFF467A69)
private val Warm = Color(0xFFF5EEDB)
private val ActiveWarm = Color(0xFFFFF1D8)

private data class DemoLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val countryCode: String
)

private val Istanbul = DemoLocation(
    name = "Istanbul",
    latitude = 41.005616,
    longitude = 28.976380,
    timeZoneId = "Europe/Istanbul",
    countryCode = "TR"
)

@Composable
fun SalatTodayScreen() {
    val location = Istanbul
    val zone = remember { ZoneId.of(location.timeZoneId) }
    val today = remember { LocalDate.now(zone) }
    val day = remember(today) {
        SalatEngine().calculateDay(
            year = today.year,
            month = today.monthValue,
            day = today.dayOfMonth,
            latitude = location.latitude,
            longitude = location.longitude,
            timeZoneId = location.timeZoneId,
            countryCode = location.countryCode
        )
    }
    val nowMillis = System.currentTimeMillis()
    val next = PrayerName.entries.firstOrNull { prayer -> day.time(prayer).toEpochMilliseconds() > nowMillis }
        ?: PrayerName.FAJR

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                Text(location.name, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text(today.format(DateTimeFormatter.ofPattern("d MMM yyyy")), color = Color(0xFF6D716E))
                Spacer(Modifier.height(28.dp))
                NextPrayerCard(day, next, zone)
                Spacer(Modifier.height(20.dp))
                PrayerName.entries.forEach { prayer ->
                    PrayerRow(
                        name = prayer.label(),
                        time = format(day, prayer, zone),
                        active = prayer == next
                    )
                }
            }
        }
    }
}

@Composable
private fun NextPrayerCard(day: PrayerDay, prayer: PrayerName, zone: ZoneId) {
    Column(
        Modifier.fillMaxWidth().background(Warm, RoundedCornerShape(28.dp)).padding(24.dp)
    ) {
        Text("NEXT PRAYER", color = Sage, fontSize = 12.sp, letterSpacing = 1.2.sp)
        Text(prayer.label(), fontSize = 25.sp, fontWeight = FontWeight.Medium)
        Text(format(day, prayer, zone), fontSize = 54.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun PrayerRow(name: String, time: String, active: Boolean = false) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (active) ActiveWarm else Color.Transparent, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
        Text(time, color = if (active) Sage else Color.Unspecified, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
    }
}

private fun format(day: PrayerDay, prayer: PrayerName, zone: ZoneId): String {
    val javaInstant = Instant.ofEpochMilli(day.time(prayer).toEpochMilliseconds())
    return DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(javaInstant)
}

private fun PrayerName.label(): String = when (this) {
    PrayerName.FAJR -> "Fajr"
    PrayerName.SUNRISE -> "Sunrise"
    PrayerName.DHUHR -> "Dhuhr"
    PrayerName.ASR -> "Asr"
    PrayerName.MAGHRIB -> "Maghrib"
    PrayerName.ISHA -> "Isha"
}
