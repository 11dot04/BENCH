package com.bench.engine

import kotlin.math.abs

/** Swing period from gyroscope zero-crossings, averaged over the last few swings. */
class SwingMeter {
    private val avg = DoubleArray(3)
    private var lastSign = 0
    private val crossings = ArrayList<Long>()

    /** Returns the current mean period in seconds whenever a new full swing completes. */
    fun push(s: Sample): Double? {
        val v = doubleArrayOf(s.x.toDouble(), s.y.toDouble(), s.z.toDouble())
        for (i in 0..2) avg[i] += 0.02 * (abs(v[i]) - avg[i])
        val ax = if (avg[0] >= avg[1] && avg[0] >= avg[2]) 0 else if (avg[1] >= avg[2]) 1 else 2
        if (avg[ax] < 0.15) {
            crossings.clear()
            lastSign = 0
            return null
        }
        val thr = 0.25 * avg[ax]
        val sign = if (v[ax] > thr) 1 else if (v[ax] < -thr) -1 else 0
        if (sign == 0) return null
        var result: Double? = null
        if (lastSign == -1 && sign == 1) {
            crossings.add(s.tNs)
            if (crossings.size > 8) crossings.removeAt(0)
            if (crossings.size >= 3) {
                val t = (crossings.last() - crossings.first()) / 1e9 / (crossings.size - 1)
                if (t >= 0.3 && t <= 6.0) result = t
            }
        }
        lastSign = sign
        return result
    }
}
