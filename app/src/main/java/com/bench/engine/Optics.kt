package com.bench.engine

import kotlin.math.atan
import kotlin.math.hypot
import kotlin.math.pow

class SensorSize(val name: String, val w: Double, val h: Double) {
    val diag: Double get() = hypot(w, h)
    val crop: Double get() = 43.2666 / diag
}

val sensorSizes = listOf(
    SensorSize("FF", 36.0, 24.0),
    SensorSize("APS-C", 23.6, 15.6),
    SensorSize("M4/3", 17.3, 13.0),
    SensorSize("1\"", 13.2, 8.8),
    SensorSize("PHONE", 9.8, 7.4)
)

class OpticsResult(
    val hyperM: Double,
    val nearM: Double,
    val farM: Double,
    val hFov: Double,
    val vFov: Double,
    val dFov: Double,
    val equivMm: Double,
    val cocMm: Double,
    val frameWM: Double,
    val frameHM: Double
)

object Optics {
    /** Aperture from a third-stop index: 0 -> f/1, 6 -> f/2, 12 -> f/4 ... */
    fun fNumber(index: Double): Double = 2.0.pow(index / 6.0)

    fun fText(n: Double): String = "f/" + (if (n < 10.0) String.format(java.util.Locale.US, "%.1f", n) else Math.round(n).toString())

    /** Depth of field and field of view. Focal length in mm, subject distance in metres. */
    fun solve(sensor: SensorSize, focalMm: Double, n: Double, subjectM: Double): OpticsResult {
        val coc = sensor.diag / 1500.0
        val s = subjectM * 1000.0
        val hyper = focalMm * focalMm / (n * coc) + focalMm
        val near = s * (hyper - focalMm) / (hyper + s - 2.0 * focalMm)
        val far = if (s >= hyper) Double.POSITIVE_INFINITY else s * (hyper - focalMm) / (hyper - s)
        val hf = Math.toDegrees(2.0 * atan(sensor.w / (2.0 * focalMm)))
        val vf = Math.toDegrees(2.0 * atan(sensor.h / (2.0 * focalMm)))
        val df = Math.toDegrees(2.0 * atan(sensor.diag / (2.0 * focalMm)))
        return OpticsResult(
            hyper / 1000.0, near / 1000.0, if (far.isInfinite()) far else far / 1000.0,
            hf, vf, df, focalMm * sensor.crop, coc,
            sensor.w * subjectM / focalMm, sensor.h * subjectM / focalMm
        )
    }

    fun hyperfocalM(sensor: SensorSize, focalMm: Double, n: Double): Double =
        (focalMm * focalMm / (n * (sensor.diag / 1500.0)) + focalMm) / 1000.0
}
