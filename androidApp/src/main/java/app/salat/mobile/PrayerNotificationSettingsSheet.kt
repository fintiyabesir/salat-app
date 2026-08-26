package app.salat.mobile

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.salat.model.PrayerName
import app.salat.model.ResolvedLocation
import app.salat.notification.NotificationSoundMode
import app.salat.notification.PrayerAlertRule
import app.salat.notification.PrayerNotificationSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerNotificationSettingsSheet(
    prayer: PrayerName,
    location: ResolvedLocation,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { AndroidPrayerNotificationSettingsStore(context.applicationContext) }
    val coordinator = remember { AndroidPrayerNotificationCoordinator(context.applicationContext) }
    val scheduler = remember { AndroidPrayerNotificationScheduler(context.applicationContext) }
    var settings by remember(prayer) { mutableStateOf(store.load()) }
    var pendingEnable by remember(prayer) { mutableStateOf(false) }

    fun applyRule(rule: PrayerAlertRule) {
        settings = PrayerNotificationSettings(settings.rules + (prayer to rule))
        store.save(settings)
        coordinator.rebuild(location)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingEnable && granted) {
            applyRule(settings.rule(prayer).copy(enabled = true))
        }
        pendingEnable = false
    }

    val rule = settings.rule(prayer)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(prayer.localizedName(), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.notifications), fontSize = 17.sp)
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            applyRule(rule.copy(enabled = false))
                        } else if (scheduler.needsNotificationPermission()) {
                            pendingEnable = true
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            applyRule(rule.copy(enabled = true))
                        }
                    }
                )
            }

            if (rule.enabled) {
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 5, 10, 15, 30).forEach { minutes ->
                        FilterChip(
                            selected = rule.minutesBefore == minutes,
                            onClick = { applyRule(rule.copy(minutesBefore = minutes)) },
                            label = {
                                Text(
                                    if (minutes == 0) stringResource(R.string.notification_at_time)
                                    else stringResource(R.string.notification_minutes_before, minutes)
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                SoundChoice(
                    label = stringResource(R.string.notification_sound_system),
                    selected = rule.soundMode == NotificationSoundMode.SYSTEM,
                    onClick = { applyRule(rule.copy(soundMode = NotificationSoundMode.SYSTEM)) }
                )
                SoundChoice(
                    label = stringResource(R.string.notification_sound_silent),
                    selected = rule.soundMode == NotificationSoundMode.SILENT,
                    onClick = { applyRule(rule.copy(soundMode = NotificationSoundMode.SILENT)) }
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SoundChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun PrayerName.localizedName(): String = when (this) {
    PrayerName.FAJR -> stringResource(R.string.prayer_fajr)
    PrayerName.SUNRISE -> stringResource(R.string.prayer_sunrise)
    PrayerName.DHUHR -> stringResource(R.string.prayer_dhuhr)
    PrayerName.ASR -> stringResource(R.string.prayer_asr)
    PrayerName.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    PrayerName.ISHA -> stringResource(R.string.prayer_isha)
}
