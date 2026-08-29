package app.salat.mobile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
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
