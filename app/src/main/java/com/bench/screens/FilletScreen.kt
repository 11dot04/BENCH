package com.bench.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.bench.engine.FilletMath
import com.bench.engine.FilletResult
import com.bench.engine.LenUnit
import com.bench.engine.Pt
import com.bench.engine.degText
import com.bench.ui.BenchColors
import com.bench.ui.BenchType
import com.bench.ui.DialMode
import com.bench.ui.ModeDial
import com.bench.ui.ModeGlyph
import com.bench.ui.Readout
import com.bench.ui.ReadoutRow
import com.bench.ui.TitleBlock
import com.bench.ui.ToolPager
import com.bench.ui.axisTicks
import com.bench.ui.brushedMetal
import com.bench.ui.gridStepMm
import com.bench.ui.niceFit
import com.bench.ui.targetMarker
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun FilletScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    var radiusMm by rememberSaveable { mutableStateOf(12.5) }
    var ori by rememberSaveable { mutableStateOf(45.0) }
    var ang by rememberSaveable { mutableStateOf(50.0) }
    var param by rememberSaveable { mutableStateOf(0) }
    val res = remember(radiusMm, ori, ang) { FilletMath.solve(radiusMm, ori, ang) }
    val modes = listOf(
        DialMode(ModeGlyph.GAUGE, "RADIUS", unit.show(radiusMm)),
        DialMode(ModeGlyph.SPOKES, "ORI", degText(ori)),
        DialMode(ModeGlyph.ANGLE, "ANG", degText(ang))
    )

    ToolPager(
        unit, onUnit, onBack,
        params = {
            TitleBlock("FILLET\nCALCULATOR")
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.weight(1f).fillMaxWidth().heightIn(min = 150.dp)
                    .clip(RoundedCornerShape(20.dp)).brushedMetal()
            ) { FilletDiagram(res, radiusMm, unit, true, Modifier.fillMaxSize()) }
            when (param) {
                0 -> ModeDial(
                    modes, 0, { param = it },
                    unit.toDisplay(radiusMm), unit.dialStep, unit.toDisplay(0.1), unit.toDisplay(200.0),
                    { radiusMm = unit.fromDisplay(it) }, 10.0
                )
                1 -> ModeDial(modes, 1, { param = it }, ori, 0.5, 0.0, 90.0, { ori = it }, 10.0)
                else -> ModeDial(modes, 2, { param = it }, ang, 0.5, 10.0, 170.0, { ang = it }, 10.0)
            }
        },
        calcs = {
            TitleBlock("PROFILE\n& DRO")
            Spacer(Modifier.height(14.dp))
            FilletDiagram(
                res, radiusMm, unit, false,
                Modifier.fillMaxWidth().height(320.dp).drawBehind {
                    drawRect(Brush.verticalGradient(listOf(BenchColors.accent.copy(alpha = 0.10f), Color.Transparent)))
                }
            )
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
            Spacer(Modifier.height(24.dp))
        }
    )
}

/** compact = names only (Parameters page); otherwise names with coordinates (Calculations page). */
@Composable
private fun FilletDiagram(r: FilletResult, radius: Double, unit: LenUnit, compact: Boolean, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val pad = (if (compact) 40.dp else 46.dp).toPx()
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
        val sc = niceFit(spanX, spanY, availW, availH, unit.perMm)
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
        val dx = o.x - c.x
        val dy = o.y - c.y
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val toO = Offset(dx / len, dy / len)

        drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(0f, o.y), Offset(size.width, o.y), 1.5f, pathEffect = dotted)
        drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(o.x, 0f), Offset(o.x, size.height), 1.5f, pathEffect = dotted)
        val stepMm = gridStepMm(max(availW, availH), sc, unit.perMm)
        axisTicks(o, (stepMm * sc).toFloat())
        val gridText = tm.measure("GRID " + unit.show(stepMm) + " " + unit.label, BenchType.label)
        drawText(gridText, topLeft = Offset(14.dp.toPx(), size.height - gridText.size.height - 12.dp.toPx()))
        drawLine(BenchColors.dim, o, Offset(sx(e1.x), sy(e1.y)), 2f)
        drawLine(BenchColors.dim, o, Offset(sx(e2.x), sy(e2.y)), 2f)
        drawCircle(BenchColors.dim, rPx, c, style = Stroke(1.5f, pathEffect = dashed))

        val glowC = Offset(c.x + toO.x * rPx * 0.8f, c.y + toO.y * rPx * 0.8f)
        drawCircle(
            Brush.radialGradient(listOf(BenchColors.accent.copy(alpha = 0.28f), Color.Transparent), glowC, rPx * 0.95f),
            rPx * 0.95f, glowC
        )

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
        drawLine(BenchColors.accent, c, Offset(c.x + toO.x * rPx, c.y + toO.y * rPx), 1.5f, pathEffect = dashed)
        drawCircle(BenchColors.accent, 4.dp.toPx(), c)
        targetMarker(a, 7.dp.toPx())
        targetMarker(b, 7.dp.toPx())
        targetMarker(o, 5.dp.toPx())

        // Labels sit outside the circle, pushed away from the point, on a soft backing so lines never cut them.
        fun label(text: String, at: Offset, out: Offset) {
            val m = tm.measure(text, BenchType.label)
            val gap = 14.dp.toPx()
            var x = if (out.x >= 0f) at.x + out.x * gap else at.x + out.x * gap - m.size.width
            var y = at.y + out.y * gap - m.size.height / 2f
            x = x.coerceIn(6f, size.width - m.size.width - 6f)
            y = y.coerceIn(6f, size.height - m.size.height - 6f)
            drawRoundRect(
                BenchColors.ground.copy(alpha = 0.72f), Offset(x - 5f, y - 3f),
                Size(m.size.width + 10f, m.size.height + 6f), CornerRadius(8f)
            )
            drawText(m, topLeft = Offset(x, y))
        }
        fun outward(p: Offset): Offset {
            val vx = p.x - c.x
            val vy = p.y - c.y
            val l = sqrt(vx * vx + vy * vy).coerceAtLeast(1f)
            return Offset(vx / l, vy / l)
        }
        fun coords(p: Pt) = " (" + unit.show(p.x) + ", " + unit.show(p.y) + ")"
        label("P₁" + (if (compact) "" else coords(r.p1)), a, outward(a))
        label("P₂" + (if (compact) "" else coords(r.p2)), b, outward(b))
        label("C" + (if (compact) "" else coords(r.center)), Offset(c.x, c.y - 4.dp.toPx()), Offset(0.7f, -0.7f))
        label("O" + (if (compact) "" else " (0, 0)"), o, Offset(0.7f, 1.2f))
    }
}
