package app.salat.mobile

import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import java.util.Locale
import app.salat.domain.DayPeriodId
import app.salat.domain.KerahatId
import app.salat.domain.KerahatWindow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import java.util.Date

class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestTimelineRefresh()
        setContent {
            MaterialTheme {
                WearPrayerScreen(WearTimelineStore(this))
            }
        }
    }

    private fun requestTimelineRefresh() {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    Wearable.getMessageClient(this).sendMessage(
                        node.id,
                        WearTimelineStore.TIMELINE_REQUEST_PATH,
                        byteArrayOf()
                    )
                }
            }
    }
}

@Composable
private fun WearPrayerScreen(store: WearTimelineStore) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            nowMillis = System.currentTimeMillis()
        }
    }

    val timeline = store.load()
    val next = timeline?.next(nowMillis)
    val context = LocalContext.current
    val timeFormat = remember { DateFormat.getTimeFormat(context) }

    Box(
        Modifier.fillMaxSize().background(WearCanvas),
        contentAlignment = Alignment.Center
    ) {
        if (timeline == null || next == null) {
            Column(
                Modifier.padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = context.getString(R.string.app_name),
                    color = WearAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = context.getString(R.string.open_phone_to_sync),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = WearMuted
                )
            }
            return@Box
        }

        // The ring is how far through the current interval we are, so a glance at the
        // arc answers "soon or not" without reading a single digit.
        val today = timeline.eventsOn(nowMillis)
        val previous = timeline.events.lastOrNull { it.atMillis <= nowMillis }
        val span = next.atMillis - (previous?.atMillis ?: (next.atMillis - DEFAULT_SPAN_MILLIS))
        val progress = if (span <= 0L) 0f
        else ((nowMillis - (next.atMillis - span)).toFloat() / span).coerceIn(0f, 1f)

        IntervalRing(progress, if (timeline.status(nowMillis)?.kerahat != null) WearKerahat else WearAccent)

        Column(
            Modifier.fillMaxWidth(0.72f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(timeline.locationName, maxLines = 1, fontSize = 12.sp, color = WearMuted)
            // Which window we are standing in, the same line the phone and the
            // widget lead with.
            val status = timeline.status(nowMillis)
            val kerahat = status?.kerahat
            status?.let {
                Text(
                    kerahat?.let { k -> context.getString(k.labelRes()) }
                        ?: context.getString(it.period.id.labelRes()),
                    maxLines = 1,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (kerahat != null) WearKerahat else WearAccent,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            Text(
                next.localizedName(context),
                maxLines = 1,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = WearMuted,
                modifier = Modifier.padding(top = 1.dp)
            )
            Text(
                timeFormat.format(Date(next.atMillis)),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraLight,
                style = WearTabular
            )
            Text(
                // In kerahat the number people want is when the window lifts.
                remainingText((kerahat?.endMillis ?: next.atMillis) - nowMillis),
                fontSize = 12.sp,
                color = WearMuted,
                style = WearTabular
            )
            // Two columns of what comes after, so the whole day fits one round face.
            val upcoming = today.filter { it.atMillis > next.atMillis }.take(4)
            if (upcoming.isNotEmpty()) {
                Column(
                    Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    upcoming.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { event ->
                                Row(
                                    Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        event.localizedName(context),
                                        maxLines = 1,
                                        fontSize = 10.5.sp,
                                        color = WearMuted
                                    )
                                    Text(
                                        timeFormat.format(Date(event.atMillis)),
                                        maxLines = 1,
                                        fontSize = 10.5.sp,
                                        style = WearTabular
                                    )
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntervalRing(progress: Float, accent: Color) {
    Canvas(Modifier.fillMaxSize()) {
        // Proportions from the artboard: r 104 and stroke 5 within a 224pt face.
        val stroke = size.minDimension * (5f / 224f)
        val diameter = size.minDimension * (208f / 224f) - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        drawArc(
            color = WearTrack,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke)
        )
        if (progress > 0f) {
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

private val WearCanvas = Color(0xFF171916)
private val WearTrack = Color(0xFF22251F)
private val WearAccent = Color(0xFF91C9B5)
private val WearMuted = Color(0xFFAAB0A8)
private val WearKerahat = Color(0xFFE0B878)
private val WearTabular = TextStyle(fontFeatureSettings = "tnum")

private fun DayPeriodId.labelRes(): Int = when (this) {
    DayPeriodId.FAJR -> R.string.period_fajr
    DayPeriodId.DUHA -> R.string.period_duha
    DayPeriodId.DHUHR -> R.string.period_dhuhr
    DayPeriodId.ASR -> R.string.period_asr
    DayPeriodId.MAGHRIB -> R.string.period_maghrib
    DayPeriodId.ISHA -> R.string.period_isha
}

private fun KerahatWindow.labelRes(): Int = when (id) {
    KerahatId.SUNRISE -> R.string.kerahat_sunrise
    KerahatId.ZENITH -> R.string.kerahat_zenith
    KerahatId.SUNSET -> R.string.kerahat_sunset
}

/** With no earlier prayer to measure from, assume a three-hour interval. */
private const val DEFAULT_SPAN_MILLIS = 3 * 60 * 60 * 1000L

private fun remainingText(remainingMillis: Long): String {
    val total = remainingMillis.coerceAtLeast(0L) / 1_000L
    val hours = total / 3600L
    val minutes = (total % 3600L) / 60L
    val seconds = total % 60L
    return if (hours > 0L) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
