package app.salat.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import java.util.Locale

/**
 * Artboard "Ayarlar Karanlık Yeni": a full screen of grouped cards rather than a
 * flat scroll of headings. Every setting the app supports keeps a home here, so
 * this carries more cards than the mock draws, using the same grammar.
 */
@Composable
fun AndroidSettingsSheet(
    location: ResolvedLocation,
    value: AppPreferences,
    onChange: (AppPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    val autoMethod = RegionalCalculationProfileResolver.resolve(location.countryCode ?: "ZZ").method
    val officialSource = OfficialSourceReferenceResolver.resolve(location.countryCode)
    val dark = MaterialTheme.colorScheme.background == ShellCanvasDark

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.settings_done),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
                Column(
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    SettingsCard(stringResource(R.string.settings_location).tracked()) {
                        ValueRow(stringResource(R.string.settings_place), location.displayName)
                        ValueRow(stringResource(R.string.settings_timezone), location.timeZoneId)
                        val status = officialSource?.status ?: OfficialSourceIntegrationStatus.LOCAL_ONLY
                        ValueRow(
                            stringResource(R.string.verification_official_source),
                            officialSource?.displayName ?: "—",
                            // The dot says the source is named, not that it is being read.
                            trailingDot = if (officialSource != null) MaterialTheme.colorScheme.primary else null
                        )
                        Text(
                            when (status) {
                                OfficialSourceIntegrationStatus.ADAPTER_AVAILABLE ->
                                    stringResource(R.string.verification_adapter_ready)
                                OfficialSourceIntegrationStatus.REFERENCE_CONFIGURED ->
                                    stringResource(R.string.verification_reference_only)
                                OfficialSourceIntegrationStatus.LOCAL_ONLY ->
                                    stringResource(R.string.verification_local_only)
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                    SettingsCard(stringResource(R.string.settings_calculation_method).tracked()) {
                        FieldLabel(stringResource(R.string.settings_method))
                        ChipFlow(
                            choices = listOf<String?>(null) + CalculationMethodId.entries.map { it.name },
                            selected = value.calculation.methodOverride?.name,
                            label = {
                                it?.methodLabel()
                                    ?: "${stringResource(R.string.settings_automatic)} · ${autoMethod.name.methodLabel()}"
                            },
                            onSelected = { raw ->
                                onChange(
                                    value.copy(
                                        calculation = value.calculation.copy(
                                            methodOverride = raw?.let(CalculationMethodId::valueOf)
                                        )
                                    )
                                )
                            }
                        )
                        FieldLabel(stringResource(R.string.settings_asr_method), top = 14.dp)
                        ChipFlow(
                            choices = listOf<String?>(null, MadhabId.SHAFI.name, MadhabId.HANAFI.name),
                            selected = value.calculation.madhabOverride?.name,
                            label = { raw ->
                                when (raw) {
                                    null -> stringResource(R.string.settings_automatic)
                                    MadhabId.HANAFI.name -> stringResource(R.string.settings_hanafi)
                                    else -> stringResource(R.string.settings_standard_shafi)
                                }
                            },
                            onSelected = { raw ->
                                onChange(
                                    value.copy(
                                        calculation = value.calculation.copy(
                                            madhabOverride = raw?.let(MadhabId::valueOf)
                                        )
                                    )
                                )
                            }
                        )
                        FieldLabel(stringResource(R.string.settings_high_latitude), top = 14.dp)
                        ChipFlow(
                            choices = HighLatitudeRuleId.entries.map { it.name },
                            selected = value.calculation.highLatitudeRule.name,
                            label = { it.highLatitudeLabel() },
                            onSelected = { raw ->
                                onChange(
                                    value.copy(
                                        calculation = value.calculation.copy(
                                            highLatitudeRule = HighLatitudeRuleId.valueOf(raw)
                                        )
                                    )
                                )
                            }
                        )
                    }

                    SettingsCard(stringResource(R.string.settings_prayer_adjustments).tracked()) {
                        PrayerName.entries.forEach { prayer ->
                            StepperRow(
                                label = prayer.adaptiveLabel(),
                                value = value.calculation.adjustments.value(prayer),
                                suffix = stringResource(R.string.settings_minutes, value.calculation.adjustments.value(prayer)),
                                range = -30..30
                            ) { minutes ->
                                val adjustments = value.calculation.adjustments.with(prayer, minutes)
                                onChange(value.copy(calculation = value.calculation.copy(adjustments = adjustments)))
                            }
                        }
                    }

                    SettingsCard(stringResource(R.string.settings_hijri_calendar).tracked()) {
                        FieldLabel(stringResource(R.string.settings_method))
                        ChipFlow(
                            choices = HijriCalendarMethodId.entries.map { it.name },
                            selected = value.hijriMethod.name,
                            label = { it.hijriLabel() },
                            onSelected = { raw -> onChange(value.copy(hijriMethod = HijriCalendarMethodId.valueOf(raw))) }
                        )
                        StepperRow(
                            label = stringResource(R.string.settings_day_adjustment),
                            value = value.hijriDayAdjustment,
                            suffix = value.hijriDayAdjustment.toString(),
                            range = -2..2,
                            top = 12.dp
                        ) { onChange(value.copy(hijriDayAdjustment = it)) }
                    }

                    SettingsCard(stringResource(R.string.settings_qibla_threshold).tracked()) {
                        ChipFlow(
                            choices = listOf<String?>(null) + QIBLA_THRESHOLD_CHOICES.map { it.toString() },
                            selected = value.qiblaAccuracyThresholdDegrees?.toString(),
                            label = { raw ->
                                raw?.let { stringResource(R.string.settings_qibla_threshold_degrees, it.toInt()) }
                                    ?: stringResource(R.string.settings_qibla_threshold_auto)
                            },
                            onSelected = { raw -> onChange(value.copy(qiblaAccuracyThresholdDegrees = raw?.toInt())) }
                        )
                    }

                    SettingsCard(stringResource(R.string.settings_appearance_language).tracked()) {
                        FieldLabel(stringResource(R.string.settings_theme))
                        Segmented(
                            choices = AppearanceMode.entries.map { it.name },
                            selected = value.appearance.name,
                            label = { it.appearanceLabel() },
                            onSelected = { onChange(value.copy(appearance = AppearanceMode.valueOf(it))) }
                        )
                        FieldLabel(stringResource(R.string.settings_language), top = 14.dp)
                        ChipFlow(
                            choices = listOf<String?>(null, "en", "tr", "ar", "fa", "ur", "bn", "ms", "zh-Hans", "zh-Hant"),
                            selected = value.languageTag,
                            label = { it.languageLabel() },
                            onSelected = { onChange(value.copy(languageTag = it)) }
                        )
                    }

                    Text(
                        stringResource(R.string.settings_data_credit),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}

/** Section labels are set in caps by the design; Kotlin's invariant uppercase
 *  would turn Turkish "Hicri" into "HICRI" rather than "HİCRİ". */
private fun String.tracked(): String = uppercase(Locale.getDefault())

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp, vertical = 15.dp)
    ) {
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            // Arabic-script locales join their letters; spacing them breaks the word.
            letterSpacing = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 0.sp else 1.4.sp
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun FieldLabel(text: String, top: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = top, bottom = 8.dp)
    )
}

@Composable
private fun ValueRow(label: String, value: String, trailingDot: Color? = null) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp)
        )
        Row(Modifier.weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 16.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
            trailingDot?.let {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(8.dp).background(it, CircleShape))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipFlow(
    choices: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        choices.forEach { item -> Chip(label(item), item == selected) { onSelected(item) } }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier.clip(shape)
            .then(
                if (selected) Modifier.background(MaterialTheme.colorScheme.secondaryContainer, shape)
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            fontSize = 14.sp,
            maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Three mutually exclusive options that all fit one line get the design's inset track. */
@Composable
private fun <T> Segmented(
    choices: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        choices.forEach { item ->
            val active = item == selected
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    )
                    .clickable { onSelected(item) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(item),
                    fontSize = 14.sp,
                    maxLines = 1,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    suffix: String,
    range: IntRange,
    top: androidx.compose.ui.unit.Dp = 0.dp,
    onValue: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(top = top, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton("−", enabled = value > range.first) { onValue(value - 1) }
            Text(
                if (value > 0) "+$suffix" else suffix,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            StepButton("+", enabled = value < range.last) { onValue(value + 1) }
        }
    }
}

@Composable
private fun StepButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    val content = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier.size(32.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, fontSize = 16.sp, color = if (enabled) content else content.copy(alpha = 0.35f))
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

/** Offered thresholds, inside AppPreferences.QIBLA_THRESHOLD_RANGE. */
private val QIBLA_THRESHOLD_CHOICES = listOf(5, 10, 15, 20, 30, 45)
