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
import kotlin.math.sqrt

/** Friction-circle g-meter with peak hold. Lay the phone flat, top edge toward the direction of travel. */
@Composable
fun GForceScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var gx by remember { mutableStateOf(0.0) }
    var gy by remember { mutableStateOf(0.0) }
    var zx by remember { mutableStateOf(0.0) }
    var zy by remember { mutableStateOf(0.0) }
    var maxFwd by remember { mutableStateOf(0.0) }
    var maxBrake by remember { mutableStateOf(0.0) }
    var maxLeft by remember { mutableStateOf(0.0) }
    var maxRight by remember { mutableStateOf(0.0) }
    var maxTotal by remember { mutableStateOf(0.0) }
    var range by rememberSaveable { mutableStateOf(1) }
    val trail = remember { mutableStateListOf<Float>() }
    LaunchedEffect(Unit) {
        val type = if (SensorHub.available(ctx, Sensor.TYPE_LINEAR_ACCELERATION)) Sensor.TYPE_LINEAR_ACCELERATION else Sensor.TYPE_ACCELEROMETER
        SensorHub.samples(ctx, type, SensorManager.SENSOR_DELAY_GAME).collect { s ->
            gx += 0.35 * (s.x / 9.80665 - gx)
            gy += 0.35 * (s.y / 9.80665 - gy)
            val x = gx - zx
            val y = gy - zy
            if (y > maxFwd) maxFwd = y
            if (-y > maxBrake) maxBrake = -y
            if (-x > maxLeft) maxLeft = -x
            if (x > maxRight) maxRight = x
            val tot = sqrt(x * x + y * y)
            if (tot > maxTotal) maxTotal = tot
            trail.add(x.toFloat()); trail.add(y.toFloat())
            if (trail.size > 120) { trail.removeAt(0); trail.removeAt(0) }
        }
    }
    val x = gx - zx
    val y = gy - zy
    val full = listOf(0.5, 1.0, 2.0)[range]
    ToolScaffold("G-FORCE\nMETER", unit, onUnit, onBack) {
        Segmented(listOf("0.5 g", "1 g", "2 g"), range, Modifier.fillMaxWidth()) { range = it }
        Gap(6)
        BigValue("NOW", plain(sqrt(x * x + y * y), 2) + " g")
        Canvas(Modifier.fillMaxWidth().height(280.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = minOf(size.width, size.height) / 2f - 8.dp.toPx()
            for (i in 1..3) drawCircle(BenchColors.line2, r * i / 3f, c, style = Stroke(if (i == 3) 1.5f else 1f))
            drawLine(BenchColors.line2, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1f)
            drawLine(BenchColors.line2, Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1f)
            fun pt(px: Float, py: Float): Offset {
                var dx = px / full.toFloat() * r
                var dy = -py / full.toFloat() * r
                val l = sqrt(dx * dx + dy * dy)
                if (l > r) { dx *= r / l; dy *= r / l }
                return Offset(c.x + dx, c.y + dy)
            }
            val n = trail.size / 2
            for (i in 0 until n) {
                val a = (i + 1f) / n
                drawCircle(BenchColors.accent.copy(alpha = a * 0.35f), 3.dp.toPx(), pt(trail[2 * i], trail[2 * i + 1]))
            }
            drawCircle(BenchColors.accent, 9.dp.toPx(), pt(x.toFloat(), y.toFloat()))
        }
        Gap(8)
        Row(Modifier.fillMaxWidth()) {
            ActionButton("ZERO", Modifier.weight(1f), primary = true) { zx = gx; zy = gy }
            Spacer(Modifier.width(8.dp))
            ActionButton("RESET PEAKS", Modifier.weight(1f)) {
                maxFwd = 0.0; maxBrake = 0.0; maxLeft = 0.0; maxRight = 0.0; maxTotal = 0.0
            }
        }
        Gap(8)
        Readout(
            listOf(
                ReadoutRow("↑", "Peak acceleration", listOf(plain(maxFwd, 2)), "g"),
                ReadoutRow("↓", "Peak braking", listOf(plain(maxBrake, 2)), "g"),
                ReadoutRow("←", "Peak left", listOf(plain(maxLeft, 2)), "g"),
                ReadoutRow("→", "Peak right", listOf(plain(maxRight, 2)), "g"),
                ReadoutRow("Σ", "Peak total", listOf(plain(maxTotal, 2)), "g")
            )
        )
        Note("LAY THE PHONE FLAT AND SECURE, TOP EDGE FORWARD. PRESS ZERO WHILE STANDING STILL. NEVER OPERATE IT WHILE DRIVING.")
    }
}
