package app.salat.mobile

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
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


@Composable
fun SalatMainShell(
    location: ResolvedLocation,
    onChooseCity: () -> Unit,
    onAppearanceChanged: (AppearanceMode) -> Unit
) {
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
    val colorScheme = awqatColorScheme(settings.appearance)

    fun persist(next: AppPreferences) {
        val languageChanged = next.languageTag != settings.languageTag
        if (next.appearance != settings.appearance) onAppearanceChanged(next.appearance)
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
            bottomBar = { AwqatBottomNav(section, dark) { section = it } }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).background(canvas)) {
                when (section) {
                    MainSection.TODAY -> AdaptiveTodayScreen(
                        location = location,
                        settings = settings,
                        dark = dark,
                        onChooseCity = onChooseCity,
                        onOpenSettings = { showSettings = true }
                    )
                    MainSection.CALENDAR -> AndroidCalendarScreen(location, settings, dark)
                    MainSection.QIBLA -> AndroidQiblaScreen(location, settings)
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

/**
 * Artboard 3e: a floating pill rather than a full-width bar, so the canvas runs
 * behind it and the tab set reads as one object. Material3's NavigationBar cannot
 * be reshaped this far, and its item defaults kept pulling in roles this app does
 * not define.
 */
@Composable
private fun AwqatBottomNav(section: MainSection, dark: Boolean, onSelect: (MainSection) -> Unit) {
    val shape = RoundedCornerShape(34.dp)
    Box(
        // A floating bar has to keep clear of the gesture handle itself; the Material
        // bar this replaces consumed that inset on its own.
        Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth()
                .then(if (dark) Modifier else Modifier.shadow(8.dp, shape))
                .background(if (dark) Color(0xFF1F221E) else ShellCanvas, shape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavPill(
                Modifier.weight(1f),
                icon = R.drawable.ic_nav_today,
                label = stringResource(R.string.today),
                selected = section == MainSection.TODAY,
                dark = dark
            ) { onSelect(MainSection.TODAY) }
            NavPill(
                Modifier.weight(1f),
                icon = R.drawable.ic_nav_calendar,
                label = stringResource(R.string.calendar),
                selected = section == MainSection.CALENDAR,
                dark = dark
            ) { onSelect(MainSection.CALENDAR) }
            NavPill(
                Modifier.weight(1f),
                // Only the Qibla glyph has a distinct filled twin: it is the brand mark,
                // and an outline Kaaba at 23dp loses its band.
                icon = if (section == MainSection.QIBLA) R.drawable.ic_nav_qibla_filled
                else R.drawable.ic_nav_qibla,
                label = stringResource(R.string.qibla),
                selected = section == MainSection.QIBLA,
                dark = dark
            ) { onSelect(MainSection.QIBLA) }
        }
    }
}

@Composable
private fun NavPill(
    modifier: Modifier,
    @DrawableRes icon: Int,
    label: String,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    val content = when {
        selected && dark -> Color(0xFF91C9B5)
        selected -> AwqatHeroSurface
        dark -> Color(0xFFAAB0A8)
        else -> Color(0xFF6D716E)
    }
    Column(
        modifier
            .clip(shape)
            .background(
                if (!selected) Color.Transparent
                else if (dark) Color(0xFF2C3A33) else Color(0xFFE4EEE9)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(23.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = content,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

