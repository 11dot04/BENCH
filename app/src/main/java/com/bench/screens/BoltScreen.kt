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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.bench.engine.BoltMath
import com.bench.engine.BoltResult
import com.bench.engine.LenUnit
import com.bench.engine.Pt
import com.bench.engine.degText
import com.bench.engine.plain
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
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun BoltScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    var pcdMm by rememberSaveable { mutableStateOf(60.0) }
    var holes by rememberSaveable { mutableStateOf(6.0) }
    var start by rememberSaveable { mutableStateOf(0.0) }
    var param by rememberSaveable { mutableStateOf(0) }
    val n = holes.toInt()
    val res = remember(pcdMm, n, start) { BoltMath.solve(pcdMm, n, start) }
    val modes = listOf(
        DialMode(ModeGlyph.DIAMETER, "PCD", unit.show(pcdMm)),
        DialMode(ModeGlyph.COUNT, "HOLES", n.toString()),
        DialMode(ModeGlyph.ANGLE, "START", degText(start))
    )

    ToolPager(
        unit, onUnit, onBack,
        params = {
            TitleBlock("BOLT\nCIRCLE")
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.weight(1f).fillMaxWidth().heightIn(min = 150.dp)
                    .clip(RoundedCornerShape(20.dp)).brushedMetal()
            ) { BoltDiagram(res, pcdMm, unit, Modifier.fillMaxSize()) }
            when (param) {
                0 -> ModeDial(
                    modes, 0, { param = it },
                    unit.toDisplay(pcdMm), unit.dialStep, unit.toDisplay(5.0), unit.toDisplay(500.0),
                    { pcdMm = unit.fromDisplay(it) }, 10.0
                )
                1 -> ModeDial(modes, 1, { param = it }, holes, 1.0, 2.0, 24.0, { holes = it }, 3.0, false)
                else -> ModeDial(modes, 2, { param = it }, start, 0.5, 0.0, 359.5, { start = it }, 10.0)
            }
        },
        calcs = {
            TitleBlock("HOLE\nPOSITIONS")
            Spacer(Modifier.height(14.dp))
            BoltDiagram(
                res, pcdMm, unit,
                Modifier.fillMaxWidth().height(300.dp).drawBehind {
                    drawRect(Brush.verticalGradient(listOf(BenchColors.accent.copy(alpha = 0.10f), Color.Transparent)))
                }
            )
            Spacer(Modifier.height(18.dp))
            val rows = ArrayList<ReadoutRow>()
            rows.add(ReadoutRow("⌀", "Pitch Circle", listOf(unit.show(pcdMm))))
            rows.add(ReadoutRow("∠", "Pitch Angle", listOf(degText(res.pitchDeg))))
            rows.add(ReadoutRow("↔", "Hole Spacing", listOf(unit.show(res.chord))))
            res.holes.forEachIndexed { i, p ->
                rows.add(ReadoutRow((i + 1).toString(), "Hole " + (i + 1), listOf(unit.show(p.x), unit.show(p.y))))
            }
            Readout(rows, columns = listOf("X", "Y"))
            Spacer(Modifier.height(24.dp))
        }
    )
}

@Composable
private fun BoltDiagram(res: BoltResult, pcd: Double, unit: LenUnit, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val pad = 44.dp.toPx()
        val avail = min(size.width, size.height) - 2 * pad
        val sc = niceFit(pcd, pcd, avail, avail, unit.perMm)
        val c = Offset(size.width / 2f, size.height / 2f)
        val rPx = (pcd / 2.0 * sc).toFloat()
        fun pt(p: Pt) = Offset(c.x + (p.x * sc).toFloat(), c.y - (p.y * sc).toFloat())

        val dotted = PathEffect.dashPathEffect(floatArrayOf(3f, 7f))
        val dashed = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
        drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(0f, c.y), Offset(size.width, c.y), 1.5f, pathEffect = dotted)
        drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(c.x, 0f), Offset(c.x, size.height), 1.5f, pathEffect = dotted)
        drawCircle(BenchColors.dim, rPx, c, style = Stroke(1.5f, pathEffect = dashed))
        val stepMm = gridStepMm(avail, sc, unit.perMm)
        axisTicks(c, (stepMm * sc).toFloat())
        val gridText = tm.measure("GRID " + unit.show(stepMm) + " " + unit.label, BenchType.label)
        drawText(gridText, topLeft = Offset(14.dp.toPx(), size.height - gridText.size.height - 12.dp.toPx()))

        val count = res.holes.size
        for (i in 0 until count) {
            drawLine(BenchColors.line2, pt(res.holes[i]), pt(res.holes[(i + 1) % count]), 1.5f)
        }
        drawLine(BenchColors.accent, c, pt(res.holes[0]), 1.5f, pathEffect = dashed)
        drawCircle(BenchColors.accent, 4.dp.toPx(), c)
        for (i in 0 until count) {
            val p = pt(res.holes[i])
            targetMarker(p, 7.dp.toPx(), if (i == 0) BenchColors.accent else BenchColors.ink)
            val vx = p.x - c.x
            val vy = p.y - c.y
            val l = sqrt(vx * vx + vy * vy).coerceAtLeast(1f)
            val m = tm.measure((i + 1).toString(), BenchType.label)
            val at = Offset(p.x + vx / l * 20.dp.toPx(), p.y + vy / l * 20.dp.toPx())
            drawText(
                m, if (i == 0) BenchColors.accent else BenchColors.dim,
                Offset(at.x - m.size.width / 2f, at.y - m.size.height / 2f)
            )
        }
    }
}
