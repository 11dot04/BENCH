package com.bench.engine

import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow

/** Nearest fraction of an inch for a given denominator: reduced text and the error in inches. */
object FractionMath {
    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    fun nearest(inch: Double, den: Int): Pair<String, Double> {
        val total = Math.round(inch * den).toInt()
        val whole = total / den
        val rem = total % den
        val err = total.toDouble() / den - inch
        if (rem == 0) return Pair(whole.toString(), err)
        val g = gcd(rem, den)
        val frac = (rem / g).toString() + "/" + (den / g)
        return Pair(if (whole == 0) frac else whole.toString() + " " + frac, err)
    }
}

enum class Material(val label: String, val sfm: Double, val chipK: Double) {
    ALU("ALU", 800.0, 0.020),
    MILD("MILD", 300.0, 0.012),
    INOX("INOX", 150.0, 0.008),
    TI("TI", 100.0, 0.006)
}

class FeedsResult(val rpm: Double, val feedMmMin: Double, val surfMMin: Double, val chipMm: Double)

object FeedsMath {
    /** Starting-point milling numbers; the chip load is a fraction of the cutter diameter. */
    fun solve(m: Material, diaMm: Double, flutes: Int, speedPct: Double): FeedsResult {
        val vc = m.sfm * 0.3048 * speedPct / 100.0
        val rpm = vc * 1000.0 / (PI * diaMm)
        val chip = diaMm * m.chipK
        return FeedsResult(rpm, rpm * flutes * chip, vc, chip)
    }
}

object Photo {
    private val speeds = doubleArrayOf(
        30.0, 15.0, 8.0, 4.0, 2.0, 1.0, 0.5, 0.25, 0.125, 1.0 / 15, 1.0 / 30, 1.0 / 60,
        1.0 / 125, 1.0 / 250, 1.0 / 500, 1.0 / 1000, 1.0 / 2000, 1.0 / 4000, 1.0 / 8000
    )

    /** EV at ISO 100 from illuminance (incident-light constant C = 250). */
    fun ev100(lux: Double): Double = if (lux <= 0.0) -10.0 else ln(lux / 2.5) / ln(2.0)

    fun shutter(lux: Double, iso: Double, f: Double): Double =
        f * f * 100.0 / (iso * 2.0.pow(ev100(lux)))

    fun nearest(t: Double): Double {
        var best = speeds[0]
        for (s in speeds) if (abs2(ln(s / t)) < abs2(ln(best / t))) best = s
        return best
    }

    private fun abs2(x: Double) = if (x < 0) -x else x

    fun text(t: Double): String =
        if (t >= 0.4) java.lang.String.format(java.util.Locale.US, "%.1f s", t)
        else "1/" + Math.round(1.0 / t)
}
