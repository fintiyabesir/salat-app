package app.salat.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay

/**
 * The current time as composition state, ticking on the [stepMillis] boundary.
 *
 * Screens that describe "now" must read the clock through this rather than calling
 * System.currentTimeMillis() during composition: a direct read happens once, so the
 * screen goes on describing the moment it was first drawn. That is how an app left
 * open overnight showed the previous day, and how a window that had closed kept its
 * name while its countdown sat at zero.
 */
@Composable
internal fun rememberNowMillis(stepMillis: Long = 1_000L): Long {
    val now by produceState(System.currentTimeMillis(), stepMillis) {
        while (true) {
            val current = System.currentTimeMillis()
            value = current
            // Wake on the boundary itself, so a window turns over when it should
            // rather than up to a whole step late.
            delay(stepMillis - current % stepMillis)
        }
    }
    return now
}
