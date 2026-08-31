package app.salat.mobile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.salat.model.AppearanceMode

internal val ShellSage = Color(0xFF467A69)
internal val ShellCanvas = Color(0xFFFAF8F3)
internal val ShellCanvasDark = Color(0xFF171916)
internal val ShellCardDark = Color(0xFF22251F)

/**
 * One theme for every screen. The location start screen used to hardcode the light
 * canvas and sage, so the first screen a user ever sees ignored dark mode entirely.
 */
@Composable
internal fun awqatColorScheme(appearance: AppearanceMode): ColorScheme {
    val dark = when (appearance) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
    }
    return if (dark) {
        darkColorScheme(
            primary = Color(0xFF91C9B5),
            // Without an explicit onPrimary, Material3's default indigo lands on the
            // mint button and reads as another product's palette.
            onPrimary = Color(0xFF10231D),
            // Without these, Material3's default indigo lands on the selected prayer
            // row and the bottom navigation pill.
            secondaryContainer = Color(0xFF2C3A33),
            onSecondaryContainer = Color(0xFFDCEDE4),
            background = ShellCanvasDark,
            surface = ShellCardDark,
            surfaceVariant = ShellCardDark,
            onBackground = Color(0xFFF2F1EC),
            onSurface = Color(0xFFF2F1EC),
            onSurfaceVariant = Color(0xFFAAB0A8)
        )
    } else {
        lightColorScheme(
            primary = ShellSage,
            onPrimary = Color.White,
            secondaryContainer = Color(0xFFDCEDE4),
            onSecondaryContainer = Color(0xFF20221F),
            background = ShellCanvas,
            surface = Color.White,
            onBackground = Color(0xFF20221F),
            onSurface = Color(0xFF20221F),
            onSurfaceVariant = Color(0xFF6D716E)
        )
    }
}

@Composable
internal fun AwqatTheme(appearance: AppearanceMode, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = awqatColorScheme(appearance), content = content)
}
