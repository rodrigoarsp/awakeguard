package com.awakeguard.alarm.mission

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeMissionDetector(
    context: Context,
    private val totalTargetCount: Int,
    private val onProgress: (remaining: Int) -> Unit,
    private val onCompleted: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var remainingCount = totalTargetCount
    private var lastShakeTime = 0L
    private var isListening = false

    companion object {
        private const val SHAKE_THRESHOLD_ACCELERATION = 14.5f // m/s² acima da gravidade
        private const val SHAKE_INTERVAL_DEBOUNCE_MS = 250L
    }

    fun startListening() {
        if (isListening || accelerometer == null) return
        isListening = true
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onProgress(remainingCount)
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calcula a aceleração resultante e subtrai a gravidade da Terra (~9.81 m/s²)
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val accelerationDelta = magnitude - SensorManager.GRAVITY_EARTH

        val now = System.currentTimeMillis()
        if (accelerationDelta > SHAKE_THRESHOLD_ACCELERATION) {
            if (now - lastShakeTime > SHAKE_INTERVAL_DEBOUNCE_MS) {
                lastShakeTime = now
                remainingCount--
                onProgress(remainingCount.coerceAtLeast(0))

                if (remainingCount <= 0) {
                    stopListening()
                    onCompleted()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Sem ação necessária
    }
}
