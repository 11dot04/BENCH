package com.bench.engine

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

object Series {
    val e12 = doubleArrayOf(1.0, 1.2, 1.5, 1.8, 2.2, 2.7, 3.3, 3.9, 4.7, 5.6, 6.8, 8.2)
    val e24 = doubleArrayOf(
        1.0, 1.1, 1.2, 1.3, 1.5, 1.6, 1.8, 2.0, 2.2, 2.4, 2.7, 3.0,
        3.3, 3.6, 3.9, 4.3, 4.7, 5.1, 5.6, 6.2, 6.8, 7.5, 8.2, 9.1
    )
    val e96 = doubleArrayOf(
        1.00, 1.02, 1.05, 1.07, 1.10, 1.13, 1.15, 1.18, 1.21, 1.24, 1.27, 1.30, 1.33, 1.37, 1.40, 1.43,
        1.47, 1.50, 1.54, 1.58, 1.62, 1.65, 1.69, 1.74, 1.78, 1.82, 1.87, 1.91, 1.96, 2.00, 2.05, 2.10,
        2.15, 2.21, 2.26, 2.32, 2.37, 2.43, 2.49, 2.55, 2.61, 2.67, 2.74, 2.80, 2.87, 2.94, 3.01, 3.09,
        3.16, 3.24, 3.32, 3.40, 3.48, 3.57, 3.65, 3.74, 3.83, 3.92, 4.02, 4.12, 4.22, 4.32, 4.42, 4.53,
        4.64, 4.75, 4.87, 4.99, 5.11, 5.23, 5.36, 5.49, 5.62, 5.76, 5.90, 6.04, 6.19, 6.34, 6.49, 6.65,
        6.81, 6.98, 7.15, 7.32, 7.50, 7.68, 7.87, 8.06, 8.25, 8.45, 8.66, 8.87, 9.09, 9.31, 9.53, 9.76
    )

    /** Value of step [i] in a series, counting up through the decades. */
    fun at(series: DoubleArray, i: Int): Double = series[i % series.size] * 10.0.pow(i / series.size)

    /** The series value closest to [target] (by ratio). */
    fun nearest(series: DoubleArray, target: Double): Double {
        if (target <= 0.0) return series[0]
        val k0 = floor(log10(target)).toInt()
        var best = series[0] * 10.0.pow(k0)
        for (k in k0 - 1..k0 + 1) for (m in series) {
            val v = m * 10.0.pow(k)
            if (abs(v / target - 1.0) < abs(best / target - 1.0)) best = v
        }
        return best
    }
}

object Fmt {
    private fun sig(v: Double): String {
        val s = if (v >= 100.0) String.format(Locale.US, "%.0f", v)
        else if (v >= 10.0) String.format(Locale.US, "%.1f", v)
        else String.format(Locale.US, "%.2f", v)
        return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
    }

    fun ohms(v: Double): String =
        if (v >= 1e6) sig(v / 1e6) + " MΩ" else if (v >= 1e3) sig(v / 1e3) + " kΩ" else sig(v) + " Ω"

    fun farads(pf: Double): String =
        if (pf >= 1e6) sig(pf / 1e6) + " µF" else if (pf >= 1e3) sig(pf / 1e3) + " nF" else sig(pf) + " pF"

    fun hertz(v: Double): String =
        if (v >= 1e6) sig(v / 1e6) + " MHz" else if (v >= 1e3) sig(v / 1e3) + " kHz" else sig(v) + " Hz"

    fun seconds(v: Double): String =
        if (v >= 1.0) sig(v) + " s" else if (v >= 1e-3) sig(v * 1e3) + " ms" else if (v >= 1e-6) sig(v * 1e6) + " µs" else sig(v * 1e9) + " ns"
}

/** Four-band resistor colour code for the nearest E24 value: digit, digit, multiplier (as a code), tolerance. */
object ColorCode {
    val names = arrayOf("BLACK", "BROWN", "RED", "ORANGE", "YELLOW", "GREEN", "BLUE", "VIOLET", "GREY", "WHITE")
    val argb = longArrayOf(
        0xFF111111, 0xFF6B3E26, 0xFFD32F2F, 0xFFF57C00, 0xFFFBC02D,
        0xFF388E3C, 0xFF1976D2, 0xFF7B1FA2, 0xFF8D8D8D, 0xFFF2F2F2
    )
    const val GOLD = 0xFFC9A227
    const val SILVER = 0xFFBFC3C7

    /** Returns (digit1, digit2, multiplierExponent) where exponent -1 is gold and -2 silver. */
    fun bands(ohms: Double): Triple<Int, Int, Int> {
        val k = floor(log10(ohms)).toInt() - 1
        var m = Math.round(ohms / 10.0.pow(k)).toInt()
        var e = k
        if (m >= 100) {
            m /= 10
            e += 1
        }
        return Triple(m / 10, m % 10, e)
    }
}
