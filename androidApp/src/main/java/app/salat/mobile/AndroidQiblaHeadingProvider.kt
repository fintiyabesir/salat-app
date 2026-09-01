package app.salat.mobile

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import app.salat.domain.SalatApi

/** What the compass is currently worth, in the terms the Qibla screen reasons about. */
data class QiblaHeading(
    val degrees: Float,
    /** Estimated error of [degrees]. Null when the sensor has not reported yet. */
    val accuracyDegrees: Int?
)

/**
 * Compass provider backed by TYPE_ROTATION_VECTOR. No network or background
 * location access is required. Headings are degrees clockwise from north in the
 * [0, 360) range; the UI compares them with the shared Qibla bearing.
 */
class AndroidQiblaHeadingProvider(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var callback: ((QiblaHeading) -> Unit)? = null
    private var accuracyDegrees: Int? = null

    val isAvailable: Boolean get() = rotationSensor != null

    /**
     * The seed written on first run, after which the threshold is a plain stored
     * number the user can change.
     *
     * Android's device spread is too wide for a model-year rule, so this asks the
     * question that actually decides compass quality: a gyroscope means
     * TYPE_ROTATION_VECTOR is sensor-fused rather than derived from the
     * magnetometer alone. It is the analogue of "iPhone 12 or newer".
     */
    val seedAccuracyThresholdDegrees: Int
        get() = if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            SalatApi.qiblaThresholdModernDevice
        } else {
            SalatApi.qiblaThresholdOlderDevice
        }

    fun start(onHeading: (QiblaHeading) -> Unit) {
        callback = onHeading
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        callback = null
        accuracyDegrees = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        // Every event carries the current accuracy. Relying on onAccuracyChanged
        // alone left this null forever on devices that never fire it for the
        // rotation vector, and a null accuracy hides the direction unconditionally —
        // so on those handsets the Qibla never appeared at any threshold.
        accuracyDegrees = event.accuracy.toApproximateDegrees()
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        SensorManager.getRotationMatrixFromVector(rotation, event.values)
        SensorManager.getOrientation(rotation, orientation)
        val degrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
        callback?.invoke(QiblaHeading((degrees + 360f) % 360f, accuracyDegrees))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return
        accuracyDegrees = accuracy.toApproximateDegrees()
    }

    private companion object {
        /**
         * Android reports compass accuracy as a coarse level, not an angle, while the
         * design and the user-facing setting are both in degrees. These are the
         * representative errors each level stands for; iOS needs no such mapping
         * because CLHeading reports degrees directly.
         */
        fun Int.toApproximateDegrees(): Int = when (this) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> 5
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> 15
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> 30
            else -> 180 // UNRELIABLE or NO_CONTACT: never good enough to show.
        }
    }
}
