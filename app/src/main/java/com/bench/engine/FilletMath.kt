package com.bench.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

data class Pt(val x: Double, val y: Double)

data class FilletResult(
    val center: Pt,
    val p1: Pt,
    val p2: Pt,
    val tangentLen: Double,
    val arcDeg: Double,
    val arcLen: Double,
    val chord: Double
)

/** Two lines meet at the origin; ori is the bisector direction, ang the angle between the lines. */
object FilletMath {
    fun solve(radius: Double, oriDeg: Double, angDeg: Double): FilletResult {
        val half = Math.toRadians(angDeg / 2.0)
        val ori = Math.toRadians(oriDeg)
        val d = radius / tan(half)
        val cd = radius / sin(half)
        val p1 = Pt(d * cos(ori - half), d * sin(ori - half))
        val p2 = Pt(d * cos(ori + half), d * sin(ori + half))
        val c = Pt(cd * cos(ori), cd * sin(ori))
        val arcDeg = 180.0 - angDeg
        val arcRad = Math.toRadians(arcDeg)
        return FilletResult(c, p1, p2, d, arcDeg, radius * arcRad, 2.0 * radius * sin(arcRad / 2.0))
    }
}
