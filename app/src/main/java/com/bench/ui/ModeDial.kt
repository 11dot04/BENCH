package com.bench.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class ModeGlyph { GAUGE, SPOKES, ANGLE, DIAMETER, COUNT }

/** One of the three selectable parameters around the dial: icon, short name, current value text. */
data class DialMode(val glyph: ModeGlyph, val label: String, val value: String)

private fun DrawScope.drawModeGlyph(g: ModeGlyph, tint: Color) {
    val s = size.minDimension
    val c = Offset(s / 2f, s / 2f)
    val w = s * 0.06f
    when (g) {
        ModeGlyph.GAUGE -> {
            drawCircle(tint, s * 0.44f, c, style = Stroke(w))
            drawLine(tint, c, Offset(s * 0.74f, s * 0.28f), w, StrokeCap.Round)
            drawCircle(tint, w, c)
        }
        ModeGlyph.SPOKES -> {
            drawCircle(tint, s * 0.44f, c, style = Stroke(w))
            drawCircle(tint, s * 0.12f, c, style = Stroke(w))
            for (i in 0 until 3) {
                val a = Math.toRadians(i * 120.0 - 90.0)
                val ca = cos(a).toFloat()
                val sa = sin(a).toFloat()
                drawLine(tint, Offset(c.x + s * 0.12f * ca, c.y + s * 0.12f * sa), Offset(c.x + s * 0.34f * ca, c.y + s * 0.34f * sa), w, StrokeCap.Round)
                drawCircle(tint, w * 1.5f, Offset(c.x + s * 0.44f * ca, c.y + s * 0.44f * sa))
            }
        }
        ModeGlyph.ANGLE -> {
            drawLine(tint, Offset(s * 0.12f, s * 0.84f), Offset(s * 0.9f, s * 0.84f), w, StrokeCap.Round)
            drawLine(tint, Offset(s * 0.12f, s * 0.84f), Offset(s * 0.64f, s * 0.16f), w, StrokeCap.Round)
            drawArc(tint, -52f, 52f, false, Offset(s * 0.12f - s * 0.3f, s * 0.84f - s * 0.3f), Size(s * 0.6f, s * 0.6f), style = Stroke(w * 0.8f))
        }
        ModeGlyph.DIAMETER -> {
            drawCircle(tint, s * 0.42f, c, style = Stroke(w))
            drawLine(tint, Offset(s * 0.08f, s * 0.5f), Offset(s * 0.92f, s * 0.5f), w * 0.8f, StrokeCap.Round)
        }
        ModeGlyph.COUNT -> {
            drawCircle(tint, w * 1.5f, Offset(s * 0.26f, s * 0.72f))
            drawCircle(tint, w * 1.5f, Offset(s * 0.74f, s * 0.72f))
            drawCircle(tint, w * 1.5f, Offset(s * 0.5f, s * 0.28f))
        }
    }
}

/**
 * Circular dial: drag around the ring to change the selected parameter, one haptic tick per step.
 * Tap an icon to pick the parameter; the centre button switches between fine and coarse steps.
 * Slot order: 0 top, 1 left, 2 right.
 */
@Composable
fun ModeDial(
    modes: List<DialMode>,
    selected: Int,
    onSelect: (Int) -> Unit,
    value: Double,
    step: Double,
    min: Double,
    max: Double,
    onValue: (Double) -> Unit,
    coarseMul: Double,
    allowFine: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val curValue by rememberUpdatedState(value)
    val curOnValue by rememberUpdatedState(onValue)
    var speed by remember { mutableStateOf(0) }
    val effStep = when (speed) { 1 -> step * coarseMul; 2 -> step / 5.0; else -> step }
    var accum by remember { mutableFloatStateOf(0f) }
    var lastAng by remember { mutableFloatStateOf(Float.NaN) }
    val tickDeg = 7f
    val cy: Dp = 170.dp
    val ring: Dp = 100.dp

    BoxWithConstraints(modifier.fillMaxWidth().height(284.dp)) {
        val cx = maxWidth / 2
        Canvas(
            Modifier.fillMaxSize().pointerInput(effStep, min, max) {
                val center = Offset(size.width / 2f, cy.toPx())
                fun angleOf(p: Offset): Float = Math.toDegrees(atan2(p.y - center.y, p.x - center.x).toDouble()).toFloat()
                detectDragGestures(
                    onDragStart = { o -> lastAng = angleOf(o) },
                    onDragEnd = { lastAng = Float.NaN },
                    onDragCancel = { lastAng = Float.NaN }
                ) { change, _ ->
                    change.consume()
                    val a = angleOf(change.position)
                    val last = lastAng
                    lastAng = a
                    if (!last.isNaN()) {
                        var d = a - last
                        while (d > 180f) d -= 360f
                        while (d < -180f) d += 360f
                        accum += d * (1f + minOf(abs(d) / 3f, 4f))
                        var cur = curValue
                        while (abs(accum) >= tickDeg) {
                            val dir = if (accum > 0f) 1 else -1
                            accum -= dir * tickDeg
                            val next = (Math.round((cur + dir * effStep) / effStep) * effStep).coerceIn(min, max)
                            if (next != cur) {
                                cur = next
                                curOnValue(next)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else {
                                accum = 0f
                                break
                            }
                        }
                    }
                }
            }
        ) {
            val c = Offset(size.width / 2f, cy.toPx())
            val r = ring.toPx()
            drawCircle(BenchColors.line, r, c, style = Stroke(1.5f))
            val base = Math.round(curValue / effStep)
            for (j in -14..14) {
                val a = 90f - j * tickDeg + accum
                val off = a - 90f
                if (abs(off) > 80f) continue
                val idx = base + j
                val major = Math.floorMod(idx, 10L) == 0L
                val mid = Math.floorMod(idx, 5L) == 0L
                val len = if (major) 12.dp.toPx() else if (mid) 8.dp.toPx() else 5.dp.toPx()
                val rad = Math.toRadians(a.toDouble())
                val dx = cos(rad).toFloat()
                val dy = sin(rad).toFloat()
                val fade = (1f - abs(off) / 80f).coerceIn(0f, 1f)
                drawLine(
                    BenchColors.dim.copy(alpha = fade * 0.6f),
                    Offset(c.x + dx * (r - len), c.y + dy * (r - len)),
                    Offset(c.x + dx * r, c.y + dy * r),
                    strokeWidth = if (major) 2.5f else 1.5f
                )
            }
            drawLine(
                BenchColors.accent, Offset(c.x, c.y + r - 16.dp.toPx()), Offset(c.x, c.y + r + 6.dp.toPx()),
                strokeWidth = 3.5f, cap = StrokeCap.Round
            )
        }

        val slots = listOf(Pair(0.dp, -ring), Pair(-ring, 0.dp), Pair(ring, 0.dp))
        modes.forEachIndexed { i, m ->
            val sel = i == selected
            val dx = slots[i].first
            val dy = slots[i].second
            val textColor = if (sel) BenchColors.accent else BenchColors.dim
            // Readout: the top one sits on one line above its icon; the side ones stack, outboard, like the concept.
            when (i) {
                0 -> Box(Modifier.fillMaxWidth().offset(y = cy + dy - 28.dp - 30.dp), contentAlignment = Alignment.TopCenter) {
                    BText(m.label + " " + m.value, BenchType.dial, color = textColor)
                }
                1 -> Box(Modifier.fillMaxWidth().offset(y = cy - 78.dp), contentAlignment = Alignment.TopStart) {
                    Column {
                        BText(m.label + ".", BenchType.dial, color = textColor)
                        BText(m.value, BenchType.dial, color = textColor)
                    }
                }
                else -> Box(Modifier.fillMaxWidth().offset(y = cy - 78.dp), contentAlignment = Alignment.TopEnd) {
                    Column(horizontalAlignment = Alignment.End) {
                        BText(m.label + ".", BenchType.dial, color = textColor, align = TextAlign.End)
                        BText(m.value, BenchType.dial, color = textColor, align = TextAlign.End)
                    }
                }
            }
            // Bare line icon; the chosen one gets a small orange arc beneath it.
            PressBox(
                modifier = Modifier.offset(x = cx + dx - 28.dp, y = cy + dy - 28.dp).size(width = 56.dp, height = 64.dp),
                color = Color.Transparent,
                shape = RoundedCornerShape(16.dp),
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSelect(i) }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val gs = 40.dp.toPx()
                    val side = (size.width - gs) / 2f
                    inset(side, 4.dp.toPx(), side, size.height - 4.dp.toPx() - gs) {
                        drawModeGlyph(m.glyph, BenchColors.ink)
                    }
                    if (sel) {
                        drawArc(
                            BenchColors.accent, 20f, 140f, false,
                            Offset(size.width / 2f - 10.dp.toPx(), size.height - 24.dp.toPx()), Size(20.dp.toPx(), 12.dp.toPx()),
                            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        // Soft centre button: step size. Dark dome, drop shadow, grey bowl.
        PressBox(
            modifier = Modifier.offset(x = cx - 40.dp, y = cy - 40.dp).size(80.dp)
                .shadow(14.dp, CircleShape, clip = false, ambientColor = Color.Black, spotColor = Color.Black),
            color = Color(0xFF1B1E23),
            shape = CircleShape,
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); speed = (speed + 1) % (if (allowFine) 3 else 2) }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xFF282C33), Color(0xFF181A1F)), Offset(size.width * 0.4f, size.height * 0.35f), size.width * 0.75f),
                    size.width / 2f, Offset(size.width / 2f, size.height / 2f)
                )
                drawArc(
                    Color(0xFF4A4F58), 20f, 140f, false,
                    Offset(size.width * 0.27f, size.height * 0.2f), Size(size.width * 0.46f, size.height * 0.46f),
                    style = Stroke(7.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            BText(
                when (speed) { 1 -> "×" + coarseMul.toInt(); 2 -> "×0.2"; else -> "×1" },
                BenchType.label, Modifier.align(Alignment.BottomCenter).padding(bottom = 11.dp), align = TextAlign.Center
            )
        }
    }
}
