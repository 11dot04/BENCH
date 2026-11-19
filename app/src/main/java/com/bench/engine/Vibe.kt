package com.bench.engine

import kotlin.math.sqrt

class VibeResult(
    val hz: Double,
    val rpm: Double,
    val rms: Double,
    val fs: Double,
    val secondHz: Double,
    val spectrum: List<Float>
)

/** Turns accelerometer samples into a dominant vibration frequency (and so a shaft speed). */
class VibeAnalyzer(private val n: Int = 1024, private val hop: Int = 128) {
    private val t = LongArray(n)
    private val ax = arrayOf(DoubleArray(n), DoubleArray(n), DoubleArray(n))
    private var count = 0L
    private var since = 0

    fun push(s: Sample): VibeResult? {
        val i = (count % n).toInt()
        t[i] = s.tNs
        ax[0][i] = s.x.toDouble()
        ax[1][i] = s.y.toDouble()
        ax[2][i] = s.z.toDouble()
        count++
        since++
        if (count < n || since < hop) return null
        since = 0
        val start = (count % n).toInt()
        val dt = (t[(start + n - 1) % n] - t[start]) / 1e9 / (n - 1)
        if (dt <= 0.0) return null
        val fs = 1.0 / dt

        var bestVar = -1.0
        var sig = DoubleArray(n)
        for (a in 0..2) {
            val raw = DoubleArray(n) { ax[a][(start + it) % n] }
            val mean = raw.average()
            var v = 0.0
            for (k in 0 until n) {
                raw[k] -= mean
                v += raw[k] * raw[k]
            }
            v /= n
            if (v > bestVar) {
                bestVar = v
                sig = raw
            }
        }
        val mag = Fft.magnitude(sig)
        val binHz = fs / n
        val top = fs / 2.0 * 0.95
        val first = Fft.peak(mag, binHz, 3.0, top)
        val masked = mag.copyOf()
        val kc = Math.round(first.first / binHz).toInt()
        for (k in maxOf(0, kc - 4)..minOf(masked.size - 1, kc + 4)) masked[k] = 0.0
        val second = Fft.peak(masked, binHz, 3.0, top)

        val groups = 160
        val per = maxOf(1, mag.size / groups)
        val spec = ArrayList<Float>()
        var g = 0
        while (g < mag.size) {
            var m = 0.0
            for (k in g until minOf(mag.size, g + per)) if (mag[k] > m) m = mag[k]
            spec.add(m.toFloat())
            g += per
        }
        return VibeResult(first.first, first.first * 60.0, sqrt(bestVar), fs, second.first, spec)
    }
}

/** Free-fall detector: reports the airborne time of a jump or hop, in seconds. */
class AirDetector {
    private var inFlight = false
    private var startNs = 0L

    fun push(s: Sample): Double? {
        val g = sqrt((s.x * s.x + s.y * s.y + s.z * s.z).toDouble())
        if (!inFlight) {
            if (g < 2.5) {
                inFlight = true
                startNs = s.tNs
            }
        } else if (g > 5.0) {
            inFlight = false
            val dt = (s.tNs - startNs) / 1e9
            return if (dt >= 0.12 && dt <= 1.5) dt else null
        }
        return null
    }
}
