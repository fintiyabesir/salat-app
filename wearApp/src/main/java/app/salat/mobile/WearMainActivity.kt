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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            delay(30_000)
            nowMillis = System.currentTimeMillis()
        }
    }

    val timeline = store.load()
    val next = timeline?.next(nowMillis)
    val context = LocalContext.current
    val timeFormat = remember { DateFormat.getTimeFormat(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (timeline == null || next == null) {
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Salat",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = context.getString(R.string.open_phone_to_sync),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            return@Column
        }

        Text(
            text = timeline.locationName,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            fontSize = 11.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = next.localizedName(context),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = timeFormat.format(Date(next.atMillis)),
            fontSize = 30.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = remainingText(context, next.atMillis - nowMillis),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(10.dp))

        timeline.eventsOn(nowMillis).forEach { event ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(event.localizedName(context), fontSize = 11.sp)
                Text(
                    timeFormat.format(Date(event.atMillis)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

private fun remainingText(context: android.content.Context, remainingMillis: Long): String {
    val totalMinutes = (remainingMillis.coerceAtLeast(0L) / 60_000L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        context.getString(R.string.remaining_hours_minutes, hours, minutes)
    } else {
        context.getString(R.string.remaining_minutes, minutes)
    }
}
