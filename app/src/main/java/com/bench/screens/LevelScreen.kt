package com.bench.screens

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bench.engine.*
import com.bench.ui.*
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.tan

/** Two-axis bubble level, flat on a surface or upright against a wall, with a zero calibration. */
@Composable
fun LevelScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var gx by remember { mutableStateOf(0.0) }
    var gy by remember { mutableStateOf(0.0) }
    var gz by remember { mutableStateOf(9.81) }
    var mode by rememberSaveable { mutableStateOf(0) }
    var zeroA by rememberSaveable { mutableStateOf(0.0) }
    var zeroB by rememberSaveable { mutableStateOf(0.0) }
    LaunchedEffect(Unit) {
        SensorHub.samples(ctx, Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME).collect { s ->
            gx += 0.2 * (s.x - gx)
            gy += 0.2 * (s.y - gy)
            gz += 0.2 * (s.z - gz)
        }
    }
    val rawA = Math.toDegrees(if (mode == 0) atan2(gx, gz) else atan2(gx, gy))
    val rawB = Math.toDegrees(if (mode == 0) atan2(gy, gz) else atan2(gz, gy))
    val a = rawA - zeroA
    val b = rawB - zeroB
    val total = sqrt(a * a + b * b)
    val onTarget = total < 0.3
    ToolScaffold("BUBBLE\nLEVEL", unit, onUnit, onBack) {
        Segmented(listOf("FLAT", "UPRIGHT"), mode, Modifier.fillMaxWidth()) { mode = it; zeroA = 0.0; zeroB = 0.0 }
        Gap(8)
        BigValue(if (onTarget) "LEVEL" else "OFF BY", plain(total, 1) + "°")
        Canvas(Modifier.fillMaxWidth().height(290.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = minOf(size.width, size.height) / 2f - 10.dp.toPx()
            val range = 5.0
            for (i in 1..5) drawCircle(BenchColors.line2.copy(alpha = if (i == 5) 1f else 0.6f), r * i / 5f, c, style = Stroke(1.5f))
            drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1.5f)
            drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1.5f)
            val br = 18.dp.toPx()
            var dx = (a / range).toFloat() * r
            var dy = (-b / range).toFloat() * r
            val len = sqrt(dx * dx + dy * dy)
            val maxLen = r - br
            if (len > maxLen) { dx *= maxLen / len; dy *= maxLen / len }
            drawCircle(if (onTarget) BenchColors.accent else BenchColors.ink, br, Offset(c.x + dx, c.y + dy))
            drawCircle(BenchColors.accent, br + 4.dp.toPx(), c, style = Stroke(if (onTarget) 4f else 1.5f))
        }
        Gap(8)
        Row(Modifier.fillMaxWidth()) {
            ActionButton("ZERO HERE", Modifier.weight(1f), primary = true) { zeroA = rawA; zeroB = rawB }
            Spacer(Modifier.width(8.dp))
            ActionButton("CLEAR ZERO", Modifier.weight(1f)) { zeroA = 0.0; zeroB = 0.0 }
        }
        Gap(8)
        Readout(
            listOf(
                ReadoutRow("↔", "Side to side", listOf(degText(a))),
                ReadoutRow("↕", if (mode == 0) "Front to back" else "Lean", listOf(degText(b))),
                ReadoutRow("∠", "Total tilt", listOf(degText(total))),
                ReadoutRow("%", "Slope", listOf(plain(100.0 * tan(Math.toRadians(total)), 2)), "%"),
                ReadoutRow("m", "Per metre", listOf(plain(1000.0 * tan(Math.toRadians(total)), 1)), "mm")
            )
        )
        Note("TO CALIBRATE: PUT THE PHONE ON A SURFACE, PRESS ZERO, TURN IT 180° AND CHECK. IF IT READS THE SAME, THE SURFACE IS WHAT IS OFF.")
    }
}
