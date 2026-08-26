package app.salat.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.salat.domain.RegionalCalculationProfileResolver
import app.salat.model.AppPreferences
import app.salat.model.AppearanceMode
import app.salat.model.CalculationMethodId
import app.salat.model.HighLatitudeRuleId
import app.salat.model.HijriCalendarMethodId
import app.salat.model.MadhabId
import app.salat.model.PrayerAdjustments
import app.salat.model.PrayerName
import app.salat.model.ResolvedLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidSettingsSheet(
    location: ResolvedLocation,
    value: AppPreferences,
    onChange: (AppPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    val autoMethod = RegionalCalculationProfileResolver.resolve(location.countryCode ?: "ZZ").method

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(24.dp))

            SettingsHeading("Calculation method")
            ChipFlow(
                choices = listOf<String?>(null) + CalculationMethodId.entries.map { it.name },
                selected = value.calculation.methodOverride?.name,
                label = { it?.methodLabel() ?: "Automatic · ${autoMethod.methodLabel()}" },
                onSelected = { raw ->
                    onChange(value.copy(calculation = value.calculation.copy(
                        methodOverride = raw?.let(CalculationMethodId::valueOf)
                    )))
                }
            )

            SettingsHeading("Asr method")
            ChipFlow(
                choices = listOf<String?>(null, MadhabId.SHAFI.name, MadhabId.HANAFI.name),
                selected = value.calculation.madhabOverride?.name,
                label = { raw -> when (raw) {
                    null -> "Automatic"
                    MadhabId.HANAFI.name -> "Hanafi"
                    else -> "Standard / Shafi"
                } },
                onSelected = { raw ->
                    onChange(value.copy(calculation = value.calculation.copy(
                        madhabOverride = raw?.let(MadhabId::valueOf)
                    )))
                }
            )

            SettingsHeading("High latitude")
            ChipFlow(
                choices = HighLatitudeRuleId.entries.map { it.name },
                selected = value.calculation.highLatitudeRule.name,
                label = { it.highLatitudeLabel() },
                onSelected = { raw ->
                    onChange(value.copy(calculation = value.calculation.copy(
                        highLatitudeRule = HighLatitudeRuleId.valueOf(raw)
                    )))
                }
            )

            SettingsHeading("Prayer time adjustments")
            PrayerName.entries.forEach { prayer ->
                AdjustmentRow(
                    label = prayer.name.lowercase().replaceFirstChar { it.uppercase() },
                    value = value.calculation.adjustments.value(prayer),
                    onValue = { minutes ->
                        val adjustments = value.calculation.adjustments.with(prayer, minutes.coerceIn(-30, 30))
                        onChange(value.copy(calculation = value.calculation.copy(adjustments = adjustments)))
                    }
                )
            }

            SettingsHeading("Hijri calendar")
            ChipFlow(
                choices = HijriCalendarMethodId.entries.map { it.name },
                selected = value.hijriMethod.name,
                label = { it.hijriLabel() },
                onSelected = { raw -> onChange(value.copy(hijriMethod = HijriCalendarMethodId.valueOf(raw))) }
            )
            AdjustmentRow(
                label = "Hijri day adjustment",
                value = value.hijriDayAdjustment,
                range = -2..2,
                onValue = { onChange(value.copy(hijriDayAdjustment = it.coerceIn(-2, 2))) }
            )

            SettingsHeading("Language")
            ChipFlow(
                choices = listOf<String?>(null, "en", "tr", "ar", "fa", "ur", "bn", "ms", "zh-Hans", "zh-Hant"),
                selected = value.languageTag,
                label = { it.languageLabel() },
                onSelected = { onChange(value.copy(languageTag = it)) }
            )

            SettingsHeading("Appearance")
            ChipFlow(
                choices = AppearanceMode.entries.map { it.name },
                selected = value.appearance.name,
                label = { it.lowercase().replaceFirstChar { c -> c.uppercase() } },
                onSelected = { onChange(value.copy(appearance = AppearanceMode.valueOf(it))) }
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsHeading(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun <T> ChipFlow(
    choices: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        choices.chunked(3).forEach { rowChoices ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowChoices.forEach { item ->
                    FilterChip(
                        selected = item == selected,
                        onClick = { onSelected(item) },
                        label = { Text(label(item)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustmentRow(
    label: String,
    value: Int,
    range: IntRange = -30..30,
    onValue: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (value > range.first) onValue(value - 1) }) { Text("−") }
            Text(
                if (value > 0) "+$value min" else "$value min",
                modifier = Modifier.padding(top = 12.dp)
            )
            OutlinedButton(onClick = { if (value < range.last) onValue(value + 1) }) { Text("+") }
        }
    }
}

private fun PrayerAdjustments.value(prayer: PrayerName): Int = when (prayer) {
    PrayerName.FAJR -> fajr
    PrayerName.SUNRISE -> sunrise
    PrayerName.DHUHR -> dhuhr
    PrayerName.ASR -> asr
    PrayerName.MAGHRIB -> maghrib
    PrayerName.ISHA -> isha
}

private fun PrayerAdjustments.with(prayer: PrayerName, minutes: Int): PrayerAdjustments = when (prayer) {
    PrayerName.FAJR -> copy(fajr = minutes)
    PrayerName.SUNRISE -> copy(sunrise = minutes)
    PrayerName.DHUHR -> copy(dhuhr = minutes)
    PrayerName.ASR -> copy(asr = minutes)
    PrayerName.MAGHRIB -> copy(maghrib = minutes)
    PrayerName.ISHA -> copy(isha = minutes)
}

private fun String.methodLabel(): String = when (this) {
    CalculationMethodId.TURKEY.name -> "Turkey / Diyanet"
    CalculationMethodId.MALAYSIA.name -> "Malaysia · 18°/18°"
    CalculationMethodId.MUSLIM_WORLD_LEAGUE.name -> "Muslim World League"
    CalculationMethodId.EGYPTIAN.name -> "Egyptian"
    CalculationMethodId.KARACHI.name -> "Karachi"
    CalculationMethodId.UMM_AL_QURA.name -> "Umm al-Qura"
    CalculationMethodId.DUBAI.name -> "Dubai"
    CalculationMethodId.QATAR.name -> "Qatar"
    CalculationMethodId.KUWAIT.name -> "Kuwait"
    CalculationMethodId.MOON_SIGHTING_COMMITTEE.name -> "Moonsighting Committee"
    CalculationMethodId.SINGAPORE.name -> "Singapore"
    CalculationMethodId.NORTH_AMERICA.name -> "North America"
    else -> this
}

private fun String.highLatitudeLabel(): String = when (this) {
    HighLatitudeRuleId.AUTOMATIC.name -> "Automatic"
    HighLatitudeRuleId.MIDDLE_OF_THE_NIGHT.name -> "Middle of night"
    HighLatitudeRuleId.SEVENTH_OF_THE_NIGHT.name -> "Seventh of night"
    HighLatitudeRuleId.TWILIGHT_ANGLE.name -> "Twilight angle"
    else -> this
}

private fun String.hijriLabel(): String = when (this) {
    HijriCalendarMethodId.AUTOMATIC.name -> "Automatic"
    HijriCalendarMethodId.UMM_AL_QURA.name -> "Umm al-Qura"
    HijriCalendarMethodId.TABULAR.name -> "Tabular"
    else -> this
}

private fun String?.languageLabel(): String = when (this) {
    null -> "System"
    "en" -> "English"
    "tr" -> "Türkçe"
    "ar" -> "العربية"
    "fa" -> "فارسی"
    "ur" -> "اردو"
    "bn" -> "বাংলা"
    "ms" -> "Bahasa Melayu"
    "zh-Hans" -> "简体中文"
    "zh-Hant" -> "繁體中文"
    else -> this
}
