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

/** Design tokens that sit outside the Material scheme because their meaning is
 *  fixed rather than role-based: gold marks only the Kaaba, the needle, the active
 *  prayer and Friday; the hero surface is the deep field the icon is built on. */
internal val AwqatGold = Color(0xFFC29653)
internal val AwqatHeroSurface = Color(0xFF1E3A32)
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
            onSurfaceVariant = Color(0xFFAAB0A8),
            // Material3's baseline outline is lavender; the design's is a warm grey.
            outline = Color(0xFF585C54),
            outlineVariant = Color(0xFF3A3D36),
            // Sheets, menus and cards pull these roles rather than `surface`. Left
            // unset they fall back to the baseline purple, which is how the city
            // picker ended up on a lavender ground.
            surfaceDim = ShellCanvasDark,
            surfaceBright = Color(0xFF2A2D28),
            surfaceContainerLowest = Color(0xFF101210),
            surfaceContainerLow = ShellCanvasDark,
            surfaceContainer = Color(0xFF1C1F1A),
            surfaceContainerHigh = ShellCardDark,
            surfaceContainerHighest = Color(0xFF2A2D28)
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
            onSurfaceVariant = Color(0xFF6D716E),
            outline = Color(0xFF9AA09A),
            outlineVariant = Color(0xFFE0DED6),
            surfaceDim = Color(0xFFE8E5DC),
            surfaceBright = ShellCanvas,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = ShellCanvas,
            surfaceContainer = Color(0xFFF5F2EA),
            surfaceContainerHigh = Color(0xFFF0EDE4),
            surfaceContainerHighest = Color(0xFFEAE7DE)
        )
    }
}

@Composable
internal fun AwqatTheme(appearance: AppearanceMode, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = awqatColorScheme(appearance), content = content)
}

/**
 * The hero card is the one "loud" surface in the design, so it carries its own
 * palette rather than a Material role: in light mode it inverts to the deep field
 * the icon is built on, in dark mode it is the ordinary card plus a hairline.
 */
internal data class HeroPalette(
    val surface: Color,
    val border: Color?,
    val content: Color,
    val accent: Color,
    val chip: Color,
    /** The unreached part of the day strip, and its labels. */
    val track: Color,
    val trackLabel: Color
)

internal fun heroPalette(dark: Boolean): HeroPalette = if (dark) {
    HeroPalette(
        surface = ShellCardDark,
        border = Color(0xFF2C3A33),
        content = Color(0xFFF2F1EC),
        accent = Color(0xFF91C9B5),
        chip = Color(0xFF91C9B5).copy(alpha = 0.14f),
        track = Color(0xFF33362F),
        trackLabel = Color(0xFF6D716E)
    )
} else {
    HeroPalette(
        surface = AwqatHeroSurface,
        border = null,
        content = ShellCanvas,
        accent = Color(0xFFA9C4B8),
        chip = Color(0xFFA9C4B8).copy(alpha = 0.18f),
        track = Color(0xFF3A5348),
        trackLabel = Color(0xFF7E978C)
    )
}
