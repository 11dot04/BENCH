package com.bench.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Stepped zoom: the drawing only re-zooms at round window sizes, so growing a radius or a
 * diameter visibly grows the figure against fixed grid ticks instead of silently re-fitting.
 */
object NiceScale {
    private val seq = doubleArrayOf(1.0, 1.2, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0)

    fun ceil(x: Double): Double {
        if (x <= 0.0) return 1.0
        val p = 10.0.pow(floor(log10(x)))
        val m = x / p
        for (s in seq) if (m <= s + 1e-9) return s * p
        return 10.0 * p
    }

    fun step(x: Double): Double {
        if (x <= 0.0) return 1.0
        val p = 10.0.pow(floor(log10(x)))
        val m = x / p
        val k = if (m <= 1.0) 1.0 else if (m <= 2.0) 2.0 else if (m <= 5.0) 5.0 else 10.0
        return k * p
    }
}

/** Pixels per mm for a figure spanning the given mm, zoomed in round steps of the display unit. */
fun niceFit(spanXMm: Double, spanYMm: Double, availW: Float, availH: Float, perMm: Double): Float {
    val nx = NiceScale.ceil(max(spanXMm * perMm, 1e-9))
    val ny = NiceScale.ceil(max(spanYMm * perMm, 1e-9))
    val pxPerDisp = min(availW / nx, availH / ny)
    return (pxPerDisp * perMm).toFloat()
}

/** Grid spacing in mm so that about six ticks span [availPx]. */
fun gridStepMm(availPx: Float, pxPerMm: Float, perMm: Double): Double {
    val spanDisp = availPx.toDouble() * perMm / pxPerMm
    return NiceScale.step(spanDisp / 6.0) / perMm
}

/** Small ticks along both axes through [origin], one every [stepPx]. */
fun DrawScope.axisTicks(origin: Offset, stepPx: Float, tint: Color = BenchColors.dim) {
    if (stepPx < 6f) return
    val t = 4.dp.toPx()
    val n = (max(size.width, size.height) / stepPx).toInt() + 2
    for (k in -n..n) {
        val x = origin.x + k * stepPx
        if (x >= 0f && x <= size.width) drawLine(tint.copy(alpha = 0.7f), Offset(x, origin.y - t), Offset(x, origin.y + t), 1.5f)
        val y = origin.y + k * stepPx
        if (y >= 0f && y <= size.height) drawLine(tint.copy(alpha = 0.7f), Offset(origin.x - t, y), Offset(origin.x + t, y), 1.5f)
    }
}

fun unitVec(v: Offset): Offset {
    val l = max(hypot(v.x, v.y), 1e-6f)
    return Offset(v.x / l, v.y / l)
}

/**
 * Grid ticks along both axes through [origin] at every power of ten of the display unit. Each level
 * fades with its on-screen spacing, so zooming never makes the grid jump. Returns the finest level
 * that is fully visible, in display units (for the "GRID" caption).
 */
fun DrawScope.fadingTicks(origin: Offset, pxPerMm: Float, perMm: Double, bounds: Rect, tint: Color = BenchColors.dim): Double {
    var finest = 0.0
    var fallback = 1.0
    for (k in -3..4) {
        val stepDisp = 10.0.pow(k)
        val stepPx = (stepDisp / perMm * pxPerMm).toFloat()
        val a = ((stepPx - 7f) / 16f).coerceIn(0f, 1f)
        if (a <= 0.02f) continue
        fallback = stepDisp
        if (a >= 0.95f && finest == 0.0) finest = stepDisp
        val half = (1.6f + (k + 3) * 0.6f).dp.toPx()
        val c = tint.copy(alpha = 0.75f * a)
        if (origin.y >= bounds.top && origin.y <= bounds.bottom) {
            val i0 = ceil((bounds.left - origin.x) / stepPx).toInt()
            val i1 = floor((bounds.right - origin.x) / stepPx).toInt()
            for (i in i0..i1) {
                val x = origin.x + i * stepPx
                drawLine(c, Offset(x, origin.y - half), Offset(x, origin.y + half), 1.5f)
            }
        }
        if (origin.x >= bounds.left && origin.x <= bounds.right) {
            val j0 = ceil((bounds.top - origin.y) / stepPx).toInt()
            val j1 = floor((bounds.bottom - origin.y) / stepPx).toInt()
            for (j in j0..j1) {
                val y = origin.y + j * stepPx
                drawLine(c, Offset(origin.x - half, y), Offset(origin.x + half, y), 1.5f)
            }
        }
    }
    return if (finest > 0.0) finest else fallback
}

fun gridCaption(stepDisp: Double, unitLabel: String): String {
    val t = if (stepDisp >= 1.0) java.lang.String.format(java.util.Locale.US, "%.0f", stepDisp)
    else java.lang.String.format(java.util.Locale.US, "%.3f", stepDisp).trimEnd('0').trimEnd('.')
    return "GRID $t $unitLabel"
}
