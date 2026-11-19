package com.bench.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs
import kotlin.math.atan2

/** Sensors are only registered while a tool collects the flow, never from the home screen. */
object SensorHub {
    /** Inclination in degrees (0..90) of the phone's long axis when it lies on a surface. */
    fun tiltDegrees(context: Context): Flow<Double> = callbackFlow {
        val sm = context.getSystemService(SensorManager::class.java)
        val acc = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sm == null || acc == null) {
            close()
            return@callbackFlow
        }
        var smooth = Double.NaN
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val a = Math.toDegrees(atan2(e.values[1].toDouble(), e.values[2].toDouble()))
                var t = abs(a)
                if (t > 90.0) t = 180.0 - t
                smooth = if (smooth.isNaN()) t else smooth + 0.15 * (t - smooth)
                trySend(smooth)
            }

            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, acc, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sm.unregisterListener(listener) }
    }
}
