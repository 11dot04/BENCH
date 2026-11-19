package com.bench.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.bench.engine.FilletMath
import com.bench.engine.FilletResult
import com.bench.engine.LenUnit
import com.bench.engine.Pt
import com.bench.engine.degText
import com.bench.ui.BText
import com.bench.ui.BenchColors
import com.bench.ui.BenchHeader
import com.bench.ui.BenchType
import com.bench.ui.ParamTile
import com.bench.ui.Readout
import com.bench.ui.ReadoutRow
import com.bench.ui.RulerDial
import com.bench.ui.TitleBlock
import com.bench.ui.brushedMetal
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

@Composable
fun FilletScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    var radiusMm by rememberSaveable { mutableStateOf(12.5) }
    var ori by rememberSaveable { mutableStateOf(45.0) }
    var ang by rememberSaveable { mutableStateOf(50.0) }
    var param by rememberSaveable { mutableStateOf(0) }
    val res = remember(radiusMm, ori, ang) { FilletMath.solve(radiusMm, ori, ang) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)
    ) {
        BenchHeader("Tools", unit, onUnit, onCrumb = onBack)
        Spacer(Modifier.height(16.dp))
        TitleBlock("FILLET\nCALCULATOR")
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(20.dp)).brushedMetal()
        ) { FilletDiagram(res, radiusMm, unit, Modifier.fillMaxSize()) }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ParamTile("RADIUS", unit.show(radiusMm), param == 0, Modifier.weight(1f).height(64.dp)) { param = 0 }
            ParamTile("ORI", degText(ori), param == 1, Modifier.weight(1f).height(64.dp)) { param = 1 }
            ParamTile("ANG", degText(ang), param == 2, Modifier.weight(1f).height(64.dp)) { param = 2 }
        }
        Spacer(Modifier.height(14.dp))
        val bigLabel = when (param) { 0 -> "RADIUS"; 1 -> "ORIENTATION"; else -> "ANGLE" }
        val bigValue = when (param) { 0 -> unit.show(radiusMm); 1 -> degText(ori); else -> degText(ang) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            BText(bigLabel, BenchType.label, Modifier.weight(1f).padding(bottom = 8.dp))
            BText(bigValue, BenchType.numeral)
        }
        Spacer(Modifier.height(6.dp))
        when (param) {
            0 -> RulerDial(
                unit.toDisplay(radiusMm), unit.dialStep, unit.toDisplay(0.1), unit.toDisplay(200.0),
                { radiusMm = unit.fromDisplay(it) }, Modifier.fillMaxWidth().height(56.dp)
            )
            1 -> RulerDial(ori, 0.5, 0.0, 90.0, { ori = it }, Modifier.fillMaxWidth().height(56.dp))
            else -> RulerDial(ang, 0.5, 10.0, 170.0, { ang = it }, Modifier.fillMaxWidth().height(56.dp))
        }
        Spacer(Modifier.height(18.dp))
        Readout(
            listOf(
                ReadoutRow("C", "Circle Center", listOf(unit.show(res.center.x), unit.show(res.center.y))),
                ReadoutRow("P₁", "Tangent 1", listOf(unit.show(res.p1.x), unit.show(res.p1.y))),
                ReadoutRow("P₂", "Tangent 2", listOf(unit.show(res.p2.x), unit.show(res.p2.y))),
                ReadoutRow("O", "Origin", listOf(unit.show(0.0), unit.show(0.0)), faint = true),
                ReadoutRow("⌒", "Arc Length", listOf(unit.show(res.arcLen))),
                ReadoutRow("↔", "Chord", listOf(unit.show(res.chord)))
            ),
            columns = listOf("X", "Y")
        )
    }
}

@Composable
private fun FilletDiagram(r: FilletResult, radius: Double, unit: LenUnit, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val pad = 38.dp.toPx()
        val e1 = Pt(r.p1.x * 1.35, r.p1.y * 1.35)
        val e2 = Pt(r.p2.x * 1.35, r.p2.y * 1.35)
        val minX = minOf(0.0, e1.x, e2.x, r.center.x - radius)
        val maxX = maxOf(0.0, e1.x, e2.x, r.center.x + radius)
        val minY = minOf(0.0, e1.y, e2.y, r.center.y - radius)
        val maxY = maxOf(0.0, e1.y, e2.y, r.center.y + radius)
        val spanX = max(maxX - minX, 1e-6)
        val spanY = max(maxY - minY, 1e-6)
        val availW = size.width - 2 * pad
        val availH = size.height - 2 * pad
        val sc = min(availW / spanX, availH / spanY).toFloat()
        val offX = pad + (availW - (spanX * sc).toFloat()) / 2f
        val offY = pad + (availH - (spanY * sc).toFloat()) / 2f
        fun sx(x: Double) = offX + ((x - minX) * sc).toFloat()
        fun sy(y: Double) = size.height - offY - ((y - minY) * sc).toFloat()

        val dotted = PathEffect.dashPathEffect(floatArrayOf(3f, 7f))
        val dashed = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
        val o = Offset(sx(0.0), sy(0.0))
        val c = Offset(sx(r.center.x), sy(r.center.y))
        val a = Offset(sx(r.p1.x), sy(r.p1.y))
        val b = Offset(sx(r.p2.x), sy(r.p2.y))
        val rPx = (radius * sc).toFloat()

        drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(0f, o.y), Offset(size.width, o.y), 1.5f, pathEffect = dotted)
        drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(o.x, 0f), Offset(o.x, size.height), 1.5f, pathEffect = dotted)
        drawLine(BenchColors.dim, o, Offset(sx(e1.x), sy(e1.y)), 2f)
        drawLine(BenchColors.dim, o, Offset(sx(e2.x), sy(e2.y)), 2f)
        drawCircle(BenchColors.dim, rPx, c, style = Stroke(1.5f, pathEffect = dashed))

        val a1 = Math.toDegrees(atan2(r.p1.y - r.center.y, r.p1.x - r.center.x))
        val a2 = Math.toDegrees(atan2(r.p2.y - r.center.y, r.p2.x - r.center.x))
        var delta = a2 - a1
        while (delta > 180.0) delta -= 360.0
        while (delta <= -180.0) delta += 360.0
        drawArc(
            BenchColors.accent, (-a1).toFloat(), (-delta).toFloat(), false,
            Offset(c.x - rPx, c.y - rPx), Size(2 * rPx, 2 * rPx),
            style = Stroke(5.dp.toPx(), cap = StrokeCap.Round)
        )
        val dx = o.x - c.x
        val dy = o.y - c.y
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        drawLine(
            BenchColors.accent, c, Offset(c.x + dx / len * rPx, c.y + dy / len * rPx),
            1.5f, pathEffect = dashed
        )
        drawCircle(BenchColors.accent, 4.dp.toPx(), c)
        marker(a)
        marker(b)
        marker(o, 5.dp.toPx())

        fun label(text: String, at: Offset, right: Boolean) {
            val m = tm.measure(text, BenchType.label)
            val x = (if (right) at.x + 10.dp.toPx() else at.x - m.size.width - 10.dp.toPx())
                .coerceIn(6f, size.width - m.size.width - 6f)
            drawText(m, topLeft = Offset(x, at.y - m.size.height / 2f - 14.dp.toPx()))
        }
        label("P₁ (" + unit.show(r.p1.x) + ", " + unit.show(r.p1.y) + ")", a, true)
        label("P₂ (" + unit.show(r.p2.x) + ", " + unit.show(r.p2.y) + ")", b, false)
        label("C (" + unit.show(r.center.x) + ", " + unit.show(r.center.y) + ")", c, true)
        label("O", Offset(o.x, o.y + 28.dp.toPx()), true)
    }
}

/** Target-style tangent marker: ringed dot with two filled quadrants. */
private fun DrawScope.marker(at: Offset, rad: Float = 7.dp.toPx()) {
    drawCircle(BenchColors.ground, rad, at)
    drawCircle(BenchColors.ink, rad, at, style = Stroke(1.5f))
    val tl = Offset(at.x - rad, at.y - rad)
    val sz = Size(2 * rad, 2 * rad)
    drawArc(BenchColors.ink, 0f, 90f, true, tl, sz, style = Fill)
    drawArc(BenchColors.ink, 180f, 90f, true, tl, sz, style = Fill)
}
