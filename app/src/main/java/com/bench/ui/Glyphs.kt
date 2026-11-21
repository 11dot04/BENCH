package com.bench.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

enum class Glyph { FILLET, SLOPE, BOLT, FEEDS, LENGTH, MASS, TEMP, LEVEL, FIELD, SOUND, LIGHT, POWER, BATTERY, WIFI, CORES, SHAKE, AIRTIME, HEIGHT, PRESSURE, FRACTION, SPIN, SWING, GFORCE, LENS, CIRCUIT, TYPE }

/** Micrographics, drawn in a square anchored bottom-left of the available area. */
fun DrawScope.drawGlyph(g: Glyph, tint: Color) {
    val s = minOf(size.width, size.height)
    val oy = size.height - s
    fun p(x: Float, y: Float) = Offset(x * s, oy + y * s)
    val w = s * 0.035f
    val acc = BenchColors.accent
    val dash = PathEffect.dashPathEffect(floatArrayOf(s * 0.05f, s * 0.05f))
    when (g) {
        Glyph.FILLET -> {
            drawLine(tint, p(0.1f, 0.05f), p(0.1f, 0.5f), w)
            drawLine(tint, p(0.5f, 0.9f), p(0.95f, 0.9f), w)
            drawCircle(tint.copy(alpha = 0.5f), 0.4f * s, p(0.5f, 0.5f), style = Stroke(w * 0.6f, pathEffect = dash))
            drawArc(acc, 90f, 90f, false, p(0.1f, 0.1f), Size(0.8f * s, 0.8f * s), style = Stroke(w * 1.5f, cap = StrokeCap.Round))
        }
        Glyph.SLOPE -> {
            drawLine(tint, p(0.05f, 0.9f), p(0.95f, 0.9f), w)
            drawLine(tint, p(0.95f, 0.9f), p(0.95f, 0.4f), w)
            drawLine(acc, p(0.05f, 0.9f), p(0.95f, 0.4f), w * 1.4f, StrokeCap.Round)
        }
        Glyph.BOLT -> {
            drawCircle(tint.copy(alpha = 0.5f), 0.38f * s, p(0.5f, 0.5f), style = Stroke(w * 0.6f, pathEffect = dash))
            for (i in 0 until 6) {
                val a = Math.toRadians(i * 60.0 - 90.0)
                drawCircle(tint, w * 1.6f, p(0.5f + 0.38f * cos(a).toFloat(), 0.5f + 0.38f * sin(a).toFloat()))
            }
            drawCircle(acc, w * 1.4f, p(0.5f, 0.5f))
        }
        Glyph.FEEDS -> {
            drawCircle(tint, 0.4f * s, p(0.5f, 0.5f), style = Stroke(w))
            drawLine(acc, p(0.5f, 0.5f), p(0.78f, 0.22f), w * 1.4f, StrokeCap.Round)
            drawCircle(acc, w * 1.4f, p(0.5f, 0.5f))
        }
        Glyph.LENGTH -> {
            drawLine(tint, p(0.05f, 0.9f), p(0.95f, 0.9f), w)
            for (i in 0..8) {
                val long = i % 4 == 0
                drawLine(if (long) acc else tint, p(0.05f + i * 0.1125f, 0.9f), p(0.05f + i * 0.1125f, if (long) 0.45f else 0.65f), w)
            }
        }
        Glyph.MASS -> {
            val path = Path().apply {
                moveTo(p(0.28f, 0.3f).x, p(0.28f, 0.3f).y)
                lineTo(p(0.72f, 0.3f).x, p(0.72f, 0.3f).y)
                lineTo(p(0.92f, 0.9f).x, p(0.92f, 0.9f).y)
                lineTo(p(0.08f, 0.9f).x, p(0.08f, 0.9f).y)
                close()
            }
            drawPath(path, tint, style = Stroke(w))
            drawCircle(acc, w * 1.6f, p(0.5f, 0.18f))
        }
        Glyph.TEMP -> {
            drawLine(tint, p(0.5f, 0.1f), p(0.5f, 0.65f), w * 2.5f, StrokeCap.Round)
            drawCircle(acc, 0.14f * s, p(0.5f, 0.78f))
        }
        Glyph.LEVEL -> {
            drawCircle(tint, 0.4f * s, p(0.5f, 0.5f), style = Stroke(w))
            drawLine(tint.copy(alpha = 0.5f), p(0.5f, 0.1f), p(0.5f, 0.9f), w * 0.6f)
            drawLine(tint.copy(alpha = 0.5f), p(0.1f, 0.5f), p(0.9f, 0.5f), w * 0.6f)
            drawCircle(acc, 0.08f * s, p(0.57f, 0.45f))
        }
        Glyph.FIELD -> {
            for (r in listOf(0.3f, 0.5f, 0.7f)) {
                drawArc(
                    if (r == 0.5f) acc else tint, -50f, 100f, false,
                    p(0.1f - r, 0.5f - r), Size(2 * r * s, 2 * r * s), style = Stroke(w, cap = StrokeCap.Round)
                )
            }
        }
        Glyph.SOUND -> {
            val hs = listOf(0.3f, 0.7f, 0.5f, 0.9f, 0.4f)
            hs.forEachIndexed { i, h ->
                val x = 0.12f + i * 0.19f
                drawLine(if (h == 0.9f) acc else tint, p(x, 0.9f), p(x, 0.9f - h), w * 3f, StrokeCap.Round)
            }
        }
        Glyph.LIGHT -> {
            drawCircle(acc, 0.17f * s, p(0.5f, 0.5f))
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val c = cos(a).toFloat()
                val sn = sin(a).toFloat()
                drawLine(tint, p(0.5f + 0.28f * c, 0.5f + 0.28f * sn), p(0.5f + 0.42f * c, 0.5f + 0.42f * sn), w, StrokeCap.Round)
            }
        }
        Glyph.POWER -> {
            val path = Path().apply {
                moveTo(p(0.55f, 0.05f).x, p(0.55f, 0.05f).y)
                lineTo(p(0.25f, 0.55f).x, p(0.25f, 0.55f).y)
                lineTo(p(0.5f, 0.55f).x, p(0.5f, 0.55f).y)
                lineTo(p(0.4f, 0.95f).x, p(0.4f, 0.95f).y)
                lineTo(p(0.75f, 0.4f).x, p(0.75f, 0.4f).y)
                lineTo(p(0.5f, 0.4f).x, p(0.5f, 0.4f).y)
                close()
            }
            drawPath(path, tint, style = Stroke(w))
        }
        Glyph.BATTERY -> {
            drawRect(tint, p(0.08f, 0.3f), Size(0.72f * s, 0.42f * s), style = Stroke(w))
            drawRect(tint, p(0.83f, 0.43f), Size(0.08f * s, 0.16f * s), style = Fill)
            drawRect(acc, p(0.15f, 0.37f), Size(0.32f * s, 0.28f * s), style = Fill)
        }
        Glyph.WIFI -> {
            for (r in listOf(0.25f, 0.5f, 0.75f)) {
                drawArc(tint, 225f, 90f, false, p(0.5f - r, 0.85f - r), Size(2 * r * s, 2 * r * s), style = Stroke(w, cap = StrokeCap.Round))
            }
            drawCircle(acc, w * 1.6f, p(0.5f, 0.85f))
        }
        Glyph.SHAKE -> {
            val path = Path().apply {
                moveTo(p(0.05f, 0.5f).x, p(0.05f, 0.5f).y)
                val ys = listOf(0.2f, 0.8f, 0.3f, 0.7f, 0.4f, 0.55f, 0.5f)
                ys.forEachIndexed { i, y -> lineTo(p(0.05f + (i + 1) * 0.13f, y).x, p(0.05f + (i + 1) * 0.13f, y).y) }
            }
            drawPath(path, acc, style = Stroke(w * 1.2f, cap = StrokeCap.Round))
        }
        Glyph.AIRTIME -> {
            val path = Path().apply {
                moveTo(p(0.05f, 0.9f).x, p(0.05f, 0.9f).y)
                quadraticBezierTo(p(0.5f, -0.3f).x, p(0.5f, -0.3f).y, p(0.95f, 0.9f).x, p(0.95f, 0.9f).y)
            }
            drawPath(path, tint, style = Stroke(w, pathEffect = dash))
            drawCircle(acc, w * 2f, p(0.5f, 0.3f))
            drawLine(tint, p(0.05f, 0.92f), p(0.95f, 0.92f), w)
        }
        Glyph.HEIGHT -> {
            drawLine(tint, p(0.3f, 0.95f), p(0.3f, 0.1f), w)
            drawLine(acc, p(0.3f, 0.1f), p(0.85f, 0.55f), w * 1.4f, StrokeCap.Round)
            drawCircle(tint, w * 1.6f, p(0.85f, 0.55f))
            drawLine(tint, p(0.05f, 0.95f), p(0.95f, 0.95f), w)
        }
        Glyph.PRESSURE -> {
            drawArc(tint, 150f, 240f, false, p(0.08f, 0.08f), Size(0.84f * s, 0.84f * s), style = Stroke(w, cap = StrokeCap.Round))
            drawLine(acc, p(0.5f, 0.5f), p(0.72f, 0.3f), w * 1.4f, StrokeCap.Round)
            drawCircle(acc, w * 1.4f, p(0.5f, 0.5f))
        }
        Glyph.FRACTION -> {
            drawCircle(tint, w * 1.8f, p(0.5f, 0.2f))
            drawCircle(tint, w * 1.8f, p(0.5f, 0.8f))
            drawLine(acc, p(0.2f, 0.5f), p(0.8f, 0.5f), w * 1.4f, StrokeCap.Round)
        }
        Glyph.SPIN -> {
            drawArc(tint, 40f, 270f, false, p(0.12f, 0.12f), Size(0.76f * s, 0.76f * s), style = Stroke(w, cap = StrokeCap.Round))
            drawCircle(acc, w * 2f, p(0.744f, 0.209f))
        }
        Glyph.SWING -> {
            drawLine(tint, p(0.5f, 0.05f), p(0.25f, 0.7f), w)
            drawCircle(acc, 0.12f * s, p(0.25f, 0.7f))
            drawArc(tint.copy(alpha = 0.6f), 68f, 44f, false, p(-0.16f, -0.61f), Size(1.32f * s, 1.32f * s), style = Stroke(w * 0.8f, pathEffect = dash))
        }
        Glyph.GFORCE -> {
            drawCircle(tint, 0.42f * s, p(0.5f, 0.5f), style = Stroke(w))
            drawCircle(tint.copy(alpha = 0.5f), 0.21f * s, p(0.5f, 0.5f), style = Stroke(w * 0.6f))
            drawLine(tint.copy(alpha = 0.5f), p(0.08f, 0.5f), p(0.92f, 0.5f), w * 0.6f)
            drawLine(tint.copy(alpha = 0.5f), p(0.5f, 0.08f), p(0.5f, 0.92f), w * 0.6f)
            drawCircle(acc, w * 2.2f, p(0.64f, 0.36f))
        }
        Glyph.LENS -> {
            drawCircle(tint, 0.4f * s, p(0.5f, 0.5f), style = Stroke(w))
            drawCircle(tint.copy(alpha = 0.6f), 0.24f * s, p(0.5f, 0.5f), style = Stroke(w * 0.7f))
            drawCircle(acc, 0.09f * s, p(0.5f, 0.5f))
            for (i in 0 until 6) {
                val a = Math.toRadians(i * 60.0)
                drawLine(tint.copy(alpha = 0.6f), p(0.5f + 0.1f * cos(a).toFloat(), 0.5f + 0.1f * sin(a).toFloat()), p(0.5f + 0.24f * cos(a).toFloat(), 0.5f + 0.24f * sin(a).toFloat()), w * 0.7f)
            }
        }
        Glyph.CIRCUIT -> {
            val path = Path().apply {
                moveTo(p(0.02f, 0.5f).x, p(0.02f, 0.5f).y)
                lineTo(p(0.22f, 0.5f).x, p(0.22f, 0.5f).y)
                val ys = listOf(0.2f, 0.8f, 0.2f, 0.8f, 0.2f, 0.8f)
                ys.forEachIndexed { i, y -> lineTo(p(0.28f + i * 0.08f, y).x, p(0.28f + i * 0.08f, y).y) }
                lineTo(p(0.8f, 0.5f).x, p(0.8f, 0.5f).y)
                lineTo(p(0.98f, 0.5f).x, p(0.98f, 0.5f).y)
            }
            drawPath(path, acc, style = Stroke(w * 1.2f, cap = StrokeCap.Round))
        }
        Glyph.TYPE -> {
            drawLine(tint, p(0.1f, 0.9f), p(0.32f, 0.12f), w * 1.4f, StrokeCap.Round)
            drawLine(tint, p(0.32f, 0.12f), p(0.54f, 0.9f), w * 1.4f, StrokeCap.Round)
            drawLine(tint, p(0.18f, 0.62f), p(0.46f, 0.62f), w)
            drawCircle(acc, 0.12f * s, p(0.75f, 0.72f), style = Stroke(w * 1.2f))
            drawLine(acc, p(0.87f, 0.6f), p(0.87f, 0.9f), w * 1.2f)
        }
        Glyph.CORES -> {
            val a = 0.4f
            val gap = 0.1f
            for (r in 0..1) for (c in 0..1) {
                val tl = p(0.05f + c * (a + gap), 0.05f + r * (a + gap))
                if (r == 0 && c == 0) drawRect(acc, tl, Size(a * s, a * s), style = Fill)
                else drawRect(tint, tl, Size(a * s, a * s), style = Stroke(w))
            }
        }
    }
}
