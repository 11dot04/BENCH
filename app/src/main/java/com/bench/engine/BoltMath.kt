package com.bench.engine

import kotlin.math.cos
import kotlin.math.sin

data class BoltResult(val holes: List<Pt>, val pitchDeg: Double, val chord: Double)

/** Holes evenly spaced on a pitch circle centred on the origin; hole 1 sits at [startDeg] from +X. */
object BoltMath {
    fun solve(pcd: Double, n: Int, startDeg: Double): BoltResult {
        val r = pcd / 2.0
        val holes = List(n) { i ->
            val a = Math.toRadians(startDeg + i * 360.0 / n)
            Pt(r * cos(a), r * sin(a))
        }
        return BoltResult(holes, 360.0 / n, pcd * sin(Math.PI / n))
    }
}
