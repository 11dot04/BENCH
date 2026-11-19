package com.bench.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs
import kotlin.math.atan2

class Sample(val tNs: Long, val x: Float, val y: Float, val z: Float)

/** Sensors are only registered while a tool collects a flow, never from the home screen. */
object SensorHub {
    fun available(context: Context, type: Int): Boolean =
        context.getSystemService(SensorManager::class.java)?.getDefaultSensor(type) != null

    /** Raw samples from any sensor; unused axes are 0. [periodUs] is the requested sampling period. */
    fun samples(context: Context, type: Int, periodUs: Int): Flow<Sample> = callbackFlow {
        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(type)
        if (sm == null || sensor == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val v = e.values
                trySend(
                    Sample(
                        e.timestamp, v[0],
                        if (v.size > 1) v[1] else 0f,
                        if (v.size > 2) v[2] else 0f
                    )
                )
            }

            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, periodUs)
        awaitClose { sm.unregisterListener(listener) }
    }.buffer(Channel.UNLIMITED)

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
