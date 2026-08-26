package app.salat.mobile

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Lightweight compass provider backed by TYPE_ROTATION_VECTOR. No network or
 * background location access is required. The callback receives degrees clockwise
 * from true-ish magnetic north in the [0, 360) range; the UI compares it with the
 * shared Qibla bearing.
 */
class AndroidQiblaHeadingProvider(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var callback: ((Float) -> Unit)? = null

    val isAvailable: Boolean get() = rotationSensor != null

    fun start(onHeading: (Float) -> Unit) {
        callback = onHeading
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        callback = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        SensorManager.getRotationMatrixFromVector(rotation, event.values)
        SensorManager.getOrientation(rotation, orientation)
        val degrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
        callback?.invoke((degrees + 360f) % 360f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
