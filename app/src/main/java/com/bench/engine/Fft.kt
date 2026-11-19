package com.bench.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Fft {
    /** Hann-windowed magnitude spectrum, n/2 bins. Input length must be a power of two. */
    fun magnitude(input: DoubleArray): DoubleArray {
        val n = input.size
        val re = DoubleArray(n) { input[it] * (0.5 - 0.5 * cos(2.0 * PI * it / (n - 1))) }
        val im = DoubleArray(n)
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while ((j and bit) != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wlr = cos(ang)
            val wli = sin(ang)
            var i = 0
            while (i < n) {
                var wr = 1.0
                var wi = 0.0
                val half = len / 2
                for (k in 0 until half) {
                    val ur = re[i + k]
                    val ui = im[i + k]
                    val vr = re[i + k + half] * wr - im[i + k + half] * wi
                    val vi = re[i + k + half] * wi + im[i + k + half] * wr
                    re[i + k] = ur + vr
                    im[i + k] = ui + vi
                    re[i + k + half] = ur - vr
                    im[i + k + half] = ui - vi
                    val nwr = wr * wlr - wi * wli
                    wi = wr * wli + wi * wlr
                    wr = nwr
                }
                i += len
            }
            len = len shl 1
        }
        return DoubleArray(n / 2) { sqrt(re[it] * re[it] + im[it] * im[it]) / n }
    }

    /** Strongest bin between [minHz] and [maxHz], refined by parabolic interpolation: (Hz, magnitude). */
    fun peak(mag: DoubleArray, binHz: Double, minHz: Double, maxHz: Double): Pair<Double, Double> {
        val k0 = maxOf(1, Math.ceil(minHz / binHz).toInt())
        val k1 = minOf(mag.size - 2, Math.floor(maxHz / binHz).toInt())
        if (k1 < k0) return Pair(0.0, 0.0)
        var best = k0
        for (k in k0..k1) if (mag[k] > mag[best]) best = k
        val a = mag[best - 1]
        val b = mag[best]
        val c = mag[best + 1]
        val den = a - 2.0 * b + c
        val delta = if (den != 0.0) 0.5 * (a - c) / den else 0.0
        return Pair((best + delta) * binHz, b)
    }
}
