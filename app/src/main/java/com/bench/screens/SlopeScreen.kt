package com.bench.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.bench.engine.LenUnit
import com.bench.engine.SensorHub
import com.bench.engine.degText
import com.bench.engine.plain
import com.bench.ui.BText
import com.bench.ui.BenchColors
import com.bench.ui.BenchHeader
import com.bench.ui.BenchType
import com.bench.ui.Segmented
import com.bench.ui.Readout
import com.bench.ui.ReadoutRow
import com.bench.ui.RulerDial
import com.bench.ui.TitleBlock
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.tan

@Composable
fun SlopeScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var angle by rememberSaveable { mutableStateOf(12.4) }
    var runMm by rememberSaveable { mutableStateOf(120.0) }
    var tilt by rememberSaveable { mutableStateOf(false) }
    var editAngle by rememberSaveable { mutableStateOf(true) }

    // The accelerometer is registered only while TILT is on and this screen is showing.
    LaunchedEffect(tilt) {
        if (tilt) SensorHub.tiltDegrees(ctx).collect { angle = it.coerceIn(0.0, 89.5) }
    }
    val editingAngle = editAngle && !tilt
    val th = Math.toRadians(angle)
    val rise = runMm * tan(th)
    val hyp = runMm / cos(th)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)
    ) {
        BenchHeader("Tools", unit, onUnit, onCrumb = onBack)
        Spacer(Modifier.height(16.dp))
        TitleBlock("SLOPE\nSOLVER")
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            Segmented(listOf("KEY", "TILT"), if (tilt) 1 else 0, Modifier.weight(1f)) { tilt = it == 1 }
            Spacer(Modifier.width(12.dp))
            Segmented(listOf("ANGLE", "RUN"), if (editingAngle) 0 else 1, Modifier.weight(1f)) {
                editAngle = it == 0
                if (it == 0) tilt = false
            }
        }
        Spacer(Modifier.height(6.dp))
        SlopeDiagram(angle, runMm, unit, Modifier.fillMaxWidth().height(230.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            BText(if (tilt) "ANGLE θ · LIVE" else if (editingAngle) "ANGLE θ" else "RUN", BenchType.label, Modifier.weight(1f).padding(bottom = 8.dp))
            BText(if (editingAngle || tilt) degText(angle) else unit.show(runMm), BenchType.numeral)
        }
        Spacer(Modifier.height(6.dp))
        if (editingAngle) {
            RulerDial(angle, 0.1, 0.0, 89.5, { angle = it }, Modifier.fillMaxWidth().height(56.dp))
        } else {
            RulerDial(
                unit.toDisplay(runMm), unit.dialStep, unit.toDisplay(1.0), unit.toDisplay(1000.0),
                { runMm = unit.fromDisplay(it) }, Modifier.fillMaxWidth().height(56.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        val u = unit.label.lowercase()
        Readout(
            listOf(
                ReadoutRow("Δy", "Rise", listOf(unit.show(rise)), u),
                ReadoutRow("Δx", "Run", listOf(unit.show(runMm)), u),
                ReadoutRow("h", "Hypotenuse", listOf(unit.show(hyp)), u),
                ReadoutRow("↗", "Grade", listOf(plain(100.0 * tan(th), 2)), "%")
            )
        )
    }
}

@Composable
private fun SlopeDiagram(angle: Double, runMm: Double, unit: LenUnit, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val pad = 36.dp.toPx()
        val th = Math.toRadians(angle)
        val tn = tan(th).toFloat().coerceIn(0.001f, 400f)
        val availW = size.width - 2 * pad
        val availH = size.height - 2 * pad
        val len = min(availW, availH / tn)
        val tri = len * tn
        val ox = pad + (availW - len) / 2f
        val oy = (size.height + tri) / 2f
        val o = Offset(ox, oy)
        val a = Offset(ox + len, oy)
        val b = Offset(ox + len, oy - tri)

        val dotted = PathEffect.dashPathEffect(floatArrayOf(3f, 7f))
        drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(0f, oy), Offset(size.width, oy), 1.5f, pathEffect = dotted)
        drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(ox, oy - tri - 24.dp.toPx()), Offset(ox, oy + 24.dp.toPx()), 1.5f, pathEffect = dotted)
        drawLine(BenchColors.dim, o, a, 2f)
        drawLine(BenchColors.dim, a, b, 2f)
        drawLine(BenchColors.ink, o, b, 3f, StrokeCap.Round)
        val rr = min(len * 0.55f, 92.dp.toPx())
        drawArc(
            BenchColors.accent, (-angle).toFloat(), angle.toFloat(), false,
            Offset(o.x - rr, o.y - rr), Size(2 * rr, 2 * rr), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(BenchColors.ink, 5.dp.toPx(), o)
        drawCircle(BenchColors.ink, 5.dp.toPx(), a)
        drawCircle(BenchColors.accent, 5.dp.toPx(), b)
        val thLabel = tm.measure("θ " + degText(angle), BenchType.label)
        drawText(thLabel, BenchColors.accent, Offset(o.x + rr + 8.dp.toPx(), oy - thLabel.size.height - 10.dp.toPx()))

        val runLabel = tm.measure("RUN " + unit.show(runMm), BenchType.label)
        drawText(runLabel, topLeft = Offset(o.x + (len - runLabel.size.width) / 2f, oy + 8.dp.toPx()))
        val riseLabel = tm.measure("RISE " + unit.show(runMm * tan(th)), BenchType.label)
        val rx = (a.x - riseLabel.size.width - 10.dp.toPx()).coerceAtLeast(4f)
        drawText(riseLabel, topLeft = Offset(rx, (a.y + b.y) / 2f - riseLabel.size.height / 2f))
    }
}
