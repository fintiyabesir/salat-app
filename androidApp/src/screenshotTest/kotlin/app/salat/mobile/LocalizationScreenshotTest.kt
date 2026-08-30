package app.salat.mobile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.salat.model.AppearanceMode
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(
    name = "Location start English LTR",
    locale = "en",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
fun locationStartEnglishLtr() {
    LocationStartScreen(
        resolving = false,
        showError = true,
        onUseLocation = {},
        onChooseCity = {}
    )
}

@PreviewTest
@Preview(
    name = "Location start Arabic RTL",
    locale = "ar",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
fun locationStartArabicRtl() {
    LocationStartScreen(
        resolving = false,
        showError = true,
        onUseLocation = {},
        onChooseCity = {}
    )
}

// The start screen hardcoded the light canvas, so the first screen a user ever saw
// ignored dark mode. This locks that in.
@PreviewTest
@Preview(
    name = "Location start dark",
    locale = "tr",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
fun locationStartDark() {
    LocationStartScreen(
        resolving = false,
        showError = false,
        onUseLocation = {},
        onChooseCity = {},
        appearance = AppearanceMode.DARK
    )
}
