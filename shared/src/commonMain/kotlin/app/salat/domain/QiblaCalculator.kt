package app.salat.domain

import app.salat.model.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object QiblaCalculator {
    private val kaaba = GeoPoint(21.422487, 39.826206)

    /** Bearing clockwise from true north in degrees, normalized to [0, 360). */
    fun bearingDegrees(from: GeoPoint): Double {
        val phi1 = from.latitude.toRadians()
        val phi2 = kaaba.latitude.toRadians()
        val deltaLambda = (kaaba.longitude - from.longitude).toRadians()
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        return (atan2(y, x).toDegrees() + 360.0) % 360.0
    }

    private fun Double.toRadians() = this * kotlin.math.PI / 180.0
    private fun Double.toDegrees() = this * 180.0 / kotlin.math.PI
}
