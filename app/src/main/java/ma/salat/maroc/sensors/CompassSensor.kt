package ma.salat.maroc.sensors

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.sqrt

/** Lecture de la boussole (degrés vrais, 0 = Nord géographique). */
data class CompassReading(
    val heading: Float,
    /** Vrai si Android signale une précision faible / non fiable du magnétomètre. */
    val needsCalibration: Boolean,
    /** Champ magnétique anormal (métal, aimant, coque magnétique…). */
    val interference: Boolean,
    /** Téléphone trop incliné : la boussole est moins précise. */
    val tilted: Boolean,
)

/**
 * Boussole hors ligne : accéléromètre + magnétomètre (SensorManager.getRotationMatrix),
 * filtrage passe-bas, correction de la déclinaison magnétique (modèle intégré à Android).
 */
class CompassSensor(context: Context) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val accelerometer: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val isAvailable: Boolean get() = accelerometer != null && magnetometer != null

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasMagnetic = false
    private var magAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
    private var declination = 0f
    private val rotation = FloatArray(9)
    private val remapped = FloatArray(9)
    private val orientation = FloatArray(3)

    var onReading: ((CompassReading) -> Unit)? = null

    fun setLocation(lat: Double, lon: Double) {
        declination = GeomagneticField(lat.toFloat(), lon.toFloat(), 0f, System.currentTimeMillis()).declination
    }

    fun start() {
        if (!isAvailable) return
        hasGravity = false
        hasMagnetic = false
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sm.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sm.unregisterListener(this)
    }

    /** « Recalibrer » : on repart de zéro (filtres remis à vide). */
    fun reset() {
        hasGravity = false
        hasMagnetic = false
        magAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
    }

    private fun lowPass(input: FloatArray, output: FloatArray, initialized: Boolean, alpha: Float) {
        for (i in 0..2) {
            output[i] = if (initialized) output[i] + alpha * (input[i] - output[i]) else input[i]
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lowPass(event.values, gravity, hasGravity, 0.15f); hasGravity = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                lowPass(event.values, geomagnetic, hasMagnetic, 0.15f); hasMagnetic = true
                magAccuracy = event.accuracy
            }
        }
        if (!hasGravity || !hasMagnetic) return
        if (!SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)) return

        @Suppress("DEPRECATION")
        val displayRotation = windowManager.defaultDisplay.rotation
        val matrix = when (displayRotation) {
            Surface.ROTATION_90 -> {
                SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, remapped); remapped
            }
            Surface.ROTATION_180 -> {
                SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, remapped); remapped
            }
            Surface.ROTATION_270 -> {
                SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, remapped); remapped
            }
            else -> rotation
        }
        SensorManager.getOrientation(matrix, orientation)

        val azimuthMagnetic = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val heading = ((azimuthMagnetic + declination) % 360f + 360f) % 360f
        val pitch = Math.toDegrees(orientation[1].toDouble())
        val roll = Math.toDegrees(orientation[2].toDouble())
        val strength = sqrt(geomagnetic[0] * geomagnetic[0] + geomagnetic[1] * geomagnetic[1] + geomagnetic[2] * geomagnetic[2])

        onReading?.invoke(
            CompassReading(
                heading = heading,
                needsCalibration = magAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                    magAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW,
                // Champ terrestre au Maroc ≈ 40–45 µT. En dehors de 25–65 µT : perturbation probable.
                interference = strength < 25f || strength > 65f,
                tilted = abs(pitch) > 35 || abs(roll) > 35,
            )
        )
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) magAccuracy = accuracy
    }
}
