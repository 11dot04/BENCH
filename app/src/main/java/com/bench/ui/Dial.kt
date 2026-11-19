package com.bench.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Horizontal ruler dial. Drag to scrub; faster drags travel further. One haptic tick per step. */
@Composable
fun RulerDial(
    value: Double,
    step: Double,
    min: Double,
    max: Double,
    onValue: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val pitch = with(LocalDensity.current) { 11.dp.toPx() }
    val curValue by rememberUpdatedState(value)
    val curOnValue by rememberUpdatedState(onValue)
    var accum by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier.pointerInput(step, min, max) {
            detectHorizontalDragGestures(
                onDragEnd = { accum = 0f },
                onDragCancel = { accum = 0f }
            ) { change, drag ->
                change.consume()
                accum += drag * (1f + minOf(abs(drag) / 6f, 5f))
                var cur = curValue
                while (abs(accum) >= pitch) {
                    val dir = if (accum < 0f) 1 else -1
                    accum += dir * pitch
                    val next = (Math.round((cur + dir * step) / step) * step).coerceIn(min, max)
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
    ) {
        val cx = size.width / 2f
        val h = size.height
        val n = (size.width / pitch / 2f).toInt() + 2
        val base = Math.round(curValue / step)
        for (i in -n..n) {
            val x = cx + i * pitch + accum
            val idx = base + i
            val fade = (1f - abs(x - cx) / cx).coerceIn(0f, 1f)
            val major = Math.floorMod(idx, 10L) == 0L
            val mid = Math.floorMod(idx, 5L) == 0L
            val len = if (major) h * 0.55f else if (mid) h * 0.4f else h * 0.26f
            drawLine(
                BenchColors.dim.copy(alpha = fade),
                Offset(x, h - len), Offset(x, h),
                strokeWidth = if (major) 2.5f else 1.5f
            )
        }
        drawLine(BenchColors.accent, Offset(cx, 0f), Offset(cx, h), strokeWidth = 3f, cap = StrokeCap.Round)
    }
}
