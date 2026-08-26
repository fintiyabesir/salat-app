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
import androidx.compose.ui.res.stringResource
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
import app.salat.verification.OfficialSourceIntegrationStatus
import app.salat.verification.OfficialSourceReferenceResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidSettingsSheet(
    location: ResolvedLocation,
    value: AppPreferences,
    onChange: (AppPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    val autoMethod = RegionalCalculationProfileResolver.resolve(location.countryCode ?: "ZZ").method
    val officialSource = OfficialSourceReferenceResolver.resolve(location.countryCode)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.settings), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            SettingsHeading(stringResource(R.string.settings_location))
            Text(location.displayName, fontWeight = FontWeight.Medium)
            Text(location.timeZoneId, fontSize = 13.sp)

            SettingsHeading(stringResource(R.string.verification_official_source))
            if (officialSource != null) {
                Text(officialSource.displayName, fontWeight = FontWeight.Medium)
            }
            Text(
                when (officialSource?.status ?: OfficialSourceIntegrationStatus.LOCAL_ONLY) {
                    OfficialSourceIntegrationStatus.ADAPTER_AVAILABLE -> stringResource(R.string.verification_adapter_ready)
                    OfficialSourceIntegrationStatus.REFERENCE_CONFIGURED -> stringResource(R.string.verification_reference_only)
                    OfficialSourceIntegrationStatus.LOCAL_ONLY -> stringResource(R.string.verification_local_only)
                },
                fontSize = 13.sp
            )

            SettingsHeading(stringResource(R.string.settings_calculation_method))
            ChipFlow(
                choices = listOf<String?>(null) + CalculationMethodId.entries.map { it.name },
                selected = value.calculation.methodOverride?.name,
                label = { it?.methodLabel() ?: "${stringResource(R.string.settings_automatic)} · ${autoMethod.name.methodLabel()}" },
                onSelected = { raw ->
                    onChange(value.copy(calculation = value.calculation.copy(
                        methodOverride = raw?.let(CalculationMethodId::valueOf)
                    )))
                }
            )

            SettingsHeading(stringResource(R.string.settings_asr_method))
            ChipFlow(
                choices = listOf<String?>(null, MadhabId.SHAFI.name, MadhabId.HANAFI.name),
                selected = value.calculation.madhabOverride?.name,
                label = { raw -> when (raw) {
                    null -> stringResource(R.string.settings_automatic)
                    MadhabId.HANAFI.name -> stringResource(R.string.settings_hanafi)
                    else -> stringResource(R.string.settings_standard_shafi)
                } },
                onSelected = { raw ->
                    onChange(value.copy(calculation = value.calculation.copy(
                        madhabOverride = raw?.let(MadhabId::valueOf)
                    )))
                }
            )

            SettingsHeading(stringResource(R.string.settings_high_latitude))
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

            SettingsHeading(stringResource(R.string.settings_prayer_adjustments))
            PrayerName.entries.forEach { prayer ->
                AdjustmentRow(
                    label = prayer.localizedSettingsLabel(),
                    value = value.calculation.adjustments.value(prayer),
                    onValue = { minutes ->
                        val adjustments = value.calculation.adjustments.with(prayer, minutes.coerceIn(-30, 30))
                        onChange(value.copy(calculation = value.calculation.copy(adjustments = adjustments)))
                    }
                )
            }

            SettingsHeading(stringResource(R.string.settings_hijri_calendar))
            ChipFlow(
                choices = HijriCalendarMethodId.entries.map { it.name },
                selected = value.hijriMethod.name,
                label = { it.hijriLabel() },
                onSelected = { raw -> onChange(value.copy(hijriMethod = HijriCalendarMethodId.valueOf(raw))) }
            )
            AdjustmentRow(
                label = stringResource(R.string.settings_hijri_day_adjustment),
                value = value.hijriDayAdjustment,
                range = -2..2,
                onValue = { onChange(value.copy(hijriDayAdjustment = it.coerceIn(-2, 2))) }
            )

            SettingsHeading(stringResource(R.string.settings_language))
            ChipFlow(
                choices = listOf<String?>(null, "en", "tr", "ar", "fa", "ur", "bn", "ms", "zh-Hans", "zh-Hant"),
                selected = value.languageTag,
                label = { it.languageLabel() },
                onSelected = { onChange(value.copy(languageTag = it)) }
            )

            SettingsHeading(stringResource(R.string.settings_appearance))
            ChipFlow(
                choices = AppearanceMode.entries.map { it.name },
                selected = value.appearance.name,
                label = { it.appearanceLabel() },
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
    label: @Composable (T) -> String,
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
                stringResource(R.string.settings_minutes, value).let { if (value > 0) "+$it" else it },
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

@Composable
private fun String.highLatitudeLabel(): String = when (this) {
    HighLatitudeRuleId.AUTOMATIC.name -> stringResource(R.string.settings_automatic)
    HighLatitudeRuleId.MIDDLE_OF_THE_NIGHT.name -> stringResource(R.string.settings_middle_night)
    HighLatitudeRuleId.SEVENTH_OF_THE_NIGHT.name -> stringResource(R.string.settings_seventh_night)
    HighLatitudeRuleId.TWILIGHT_ANGLE.name -> stringResource(R.string.settings_twilight_angle)
    else -> this
}

@Composable
private fun String.hijriLabel(): String = when (this) {
    HijriCalendarMethodId.AUTOMATIC.name -> stringResource(R.string.settings_automatic)
    HijriCalendarMethodId.UMM_AL_QURA.name -> "Umm al-Qura"
    HijriCalendarMethodId.TABULAR.name -> "Tabular"
    else -> this
}

@Composable
private fun String.appearanceLabel(): String = when (this) {
    AppearanceMode.SYSTEM.name -> stringResource(R.string.settings_system)
    AppearanceMode.LIGHT.name -> stringResource(R.string.settings_light)
    AppearanceMode.DARK.name -> stringResource(R.string.settings_dark)
    else -> this
}

@Composable
private fun PrayerName.localizedSettingsLabel(): String = when (this) {
    PrayerName.FAJR -> stringResource(R.string.prayer_fajr)
    PrayerName.SUNRISE -> stringResource(R.string.prayer_sunrise)
    PrayerName.DHUHR -> stringResource(R.string.prayer_dhuhr)
    PrayerName.ASR -> stringResource(R.string.prayer_asr)
    PrayerName.MAGHRIB -> stringResource(R.string.prayer_maghrib)
    PrayerName.ISHA -> stringResource(R.string.prayer_isha)
}

@Composable
private fun String?.languageLabel(): String = when (this) {
    null -> stringResource(R.string.settings_system)
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
