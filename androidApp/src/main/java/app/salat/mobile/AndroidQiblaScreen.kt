package app.salat.mobile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.salat.domain.QiblaCalculator
import app.salat.domain.QiblaDirectionPolicy
import app.salat.domain.QiblaDisplay
import app.salat.model.AppPreferences
import app.salat.model.GeoPoint
import app.salat.model.ResolvedLocation
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Qibla screen, artboards 2a/2b/2c of docs/design/awqat-final-designs.dc.html.
 *
 * The rose carries north, so it counter-rotates with the heading; the needle sits
 * at the deviation, which puts it straight up exactly when the device faces the
 * Kaaba. Below the accuracy threshold the needle and every degree are removed
 * rather than softened — the design is explicit that a wrong Qibla is never shown.
 */
@Composable
internal fun AndroidQiblaScreen(location: ResolvedLocation, settings: AppPreferences) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val provider = remember { AndroidQiblaHeadingProvider(context) }

    val point = remember(location) { GeoPoint(location.point.latitude, location.point.longitude) }
    val bearing = remember(point) { QiblaCalculator.bearingDegrees(point).toFloat() }
    val distanceKm = remember(point) { QiblaCalculator.distanceKilometres(point) }

    var heading by remember { mutableStateOf<QiblaHeading?>(null) }
    DisposableEffect(provider) {
        provider.start { heading = it }
        onDispose { provider.stop() }
    }

    val threshold = settings.qiblaAccuracyThresholdDegrees ?: provider.defaultAccuracyThresholdDegrees
    // The "never show a wrong Qibla" rule lives in the shared module, where it is
    // pinned by tests rather than by this screen.
    val display = QiblaDirectionPolicy.evaluate(
        bearingDegrees = bearing,
        headingDegrees = heading?.degrees,
        accuracyDegrees = heading?.accuracyDegrees,
        thresholdDegrees = threshold
    )
    val deviation = (display as? QiblaDisplay.Direction)?.deviationDegrees
    val trusted = deviation != null
    val aligned = deviation != null && abs(deviation) <= 3f

    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(aligned) {
        if (aligned && !wasAligned) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        wasAligned = aligned
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(26.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.qibla), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${location.displayName} → ${stringResource(R.string.qibla_mecca)}",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (!provider.isAvailable) {
                Text(
                    stringResource(R.string.qibla_compass_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                QiblaRose(
                    headingDegrees = heading?.degrees,
                    deviationDegrees = deviation,
                    aligned = aligned
                )
            }
        }

        if (provider.isAvailable && !trusted) {
            LowAccuracyNotice(calibrating = heading != null)
            Spacer(Modifier.height(16.dp))
        }

        QiblaReadout(
            bearing = bearing,
            distanceKm = distanceKm,
            deviation = deviation
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun QiblaRose(headingDegrees: Float?, deviationDegrees: Float?, aligned: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val tick = scheme.onSurfaceVariant.copy(alpha = 0.35f)
    val face = scheme.surface
    val needle = AwqatGold
    val tail = scheme.onSurfaceVariant.copy(alpha = 0.30f)
    val hub = AwqatHeroSurface

    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(320.dp)) {
            val radius = size.minDimension / 2f
            val centre = Offset(radius, radius)
            drawCircle(face, radius = radius * 0.90f, center = centre)

            // The rose carries north, so it turns opposite the device.
            rotate(-(headingDegrees ?: 0f), centre) {
                repeat(TICK_COUNT) { index ->
                    val major = index % 5 == 0
                    val angle = index * (360f / TICK_COUNT)
                    drawTick(centre, radius * 0.86f, angle, if (major) 12f else 7f, tick, if (major) 3f else 2f)
                }
            }

            deviationDegrees?.let { deviation ->
                rotate(deviation, centre) {
                    drawNeedle(centre, radius * 0.86f, needle, tail)
                }
                drawKaabaBadge(centre, radius, deviation, face, tick)
            }
            drawCircle(hub, radius = radius * 0.030f, center = centre)
        }

        // Cardinal letters ride the same rotation as the ticks but stay upright.
        CardinalLetters(headingDegrees ?: 0f)

        // Artboard 2a puts the reading below the hub. The mock only ever shows a
        // needle pointing up, but at southerly bearings it swings straight through
        // this text, so the reading gets its own backing to stay legible.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset(y = 74.dp)
                .background(scheme.surface.copy(alpha = 0.88f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            if (deviationDegrees == null) {
                Text("—°", fontSize = 34.sp, fontWeight = FontWeight.Light, color = scheme.onSurfaceVariant)
            } else {
                Text(
                    "${abs(deviationDegrees).roundToInt()}°",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    when {
                        aligned -> stringResource(R.string.qibla_aligned)
                        deviationDegrees > 0 -> stringResource(R.string.qibla_turn_right)
                        else -> stringResource(R.string.qibla_turn_left)
                    },
                    fontSize = 14.sp,
                    color = if (aligned) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CardinalLetters(headingDegrees: Float) {
    val letters = listOf(
        stringResource(R.string.qibla_cardinal_north) to 0f,
        stringResource(R.string.qibla_cardinal_east) to 90f,
        stringResource(R.string.qibla_cardinal_south) to 180f,
        stringResource(R.string.qibla_cardinal_west) to 270f
    )
    Box(Modifier.size(320.dp), contentAlignment = Alignment.Center) {
        letters.forEach { (label, angle) ->
            val radians = Math.toRadians(((angle - headingDegrees) - 90f).toDouble())
            val distance = 132.dp
            Text(
                label,
                fontSize = if (angle == 0f) 16.sp else 15.sp,
                fontWeight = if (angle == 0f) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.offset(
                    x = distance * cos(radians).toFloat(),
                    y = distance * sin(radians).toFloat()
                )
            )
        }
    }
}

@Composable
private fun LowAccuracyNotice(calibrating: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            stringResource(R.string.qibla_low_accuracy_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            stringResource(R.string.qibla_low_accuracy_body),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (calibrating) {
            Text(
                stringResource(R.string.qibla_calibrating),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun QiblaReadout(bearing: Float, distanceKm: Double, deviation: Float?) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ReadoutCell(stringResource(R.string.qibla_label_direction), "${bearing.roundToInt()}°")
        ReadoutCell(
            stringResource(R.string.qibla_label_distance),
            stringResource(R.string.qibla_distance_km, formatThousands(distanceKm.roundToInt()))
        )
        ReadoutCell(
            stringResource(R.string.qibla_label_deviation),
            deviation?.let { (if (it > 0) "+" else "") + it.roundToInt() + "°" } ?: "—",
            accent = deviation != null
        )
    }
}

@Composable
private fun ReadoutCell(label: String, value: String, accent: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

private const val TICK_COUNT = 60

private fun DrawScope.drawTick(
    centre: Offset,
    radius: Float,
    angleDegrees: Float,
    length: Float,
    colour: Color,
    width: Float
) {
    val radians = Math.toRadians((angleDegrees - 90f).toDouble())
    val cosA = cos(radians).toFloat()
    val sinA = sin(radians).toFloat()
    drawLine(
        color = colour,
        start = Offset(centre.x + (radius - length) * cosA, centre.y + (radius - length) * sinA),
        end = Offset(centre.x + radius * cosA, centre.y + radius * sinA),
        strokeWidth = width,
        cap = StrokeCap.Round
    )
}

/**
 * Proportions come straight from artboard 2a, where the needle runs 89 units of a
 * 165-unit radius and is 7 units wide at the hub. Expressing them as fractions is
 * what keeps the shape right at any canvas size.
 */
private fun DrawScope.drawNeedle(centre: Offset, radius: Float, head: Color, tailColour: Color) {
    val length = radius * 0.54f
    val halfWidth = radius * 0.042f
    val notch = radius * 0.042f
    translate(centre.x, centre.y) {
        drawPath(
            Path().apply {
                moveTo(0f, -length)
                lineTo(halfWidth, 0f)
                lineTo(0f, -notch)
                lineTo(-halfWidth, 0f)
                close()
            },
            head
        )
        drawPath(
            Path().apply {
                moveTo(0f, length)
                lineTo(radius * 0.030f, 0f)
                lineTo(-radius * 0.030f, 0f)
                close()
            },
            tailColour
        )
    }
}

/**
 * The Kaaba medallion rides the needle but is drawn unrotated, because the design
 * keeps it upright at every bearing. Detail degrades the same way the icon does:
 * body, hizam, door.
 */
private fun DrawScope.drawKaabaBadge(
    centre: Offset,
    radius: Float,
    angleDegrees: Float,
    face: Color,
    outline: Color
) {
    val radians = Math.toRadians((angleDegrees - 90f).toDouble())
    val at = Offset(
        centre.x + radius * 0.62f * cos(radians).toFloat(),
        centre.y + radius * 0.62f * sin(radians).toFloat()
    )
    val badge = radius * 0.125f
    drawCircle(face, radius = badge, center = at)
    drawCircle(outline, radius = badge, center = at, style = Stroke(width = radius * 0.008f))

    val body = badge * 0.90f
    translate(at.x - body / 2f, at.y - body / 2f) {
        drawRect(KaabaBody, topLeft = Offset(0f, body * 0.12f), size = Size(body, body * 0.82f))
        drawRect(AwqatGold, topLeft = Offset(0f, body * 0.29f), size = Size(body, body * 0.15f))
        drawRect(AwqatGold, topLeft = Offset(body * 0.58f, body * 0.62f), size = Size(body * 0.22f, body * 0.32f))
    }
}

private val KaabaBody = Color(0xFF1B1D1A)

/** Groups thousands without pulling in a locale-specific number formatter. */
private fun formatThousands(value: Int): String =
    value.toString().reversed().chunked(3).joinToString(".").reversed()
