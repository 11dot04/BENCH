package com.bench.screens

import android.graphics.RuntimeShader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.bench.engine.BoltMath
import com.bench.engine.BoltResult
import com.bench.engine.LenUnit
import com.bench.engine.Pt
import com.bench.engine.degText
import com.bench.engine.plain
import com.bench.ui.BRUSHED_SRC
import com.bench.ui.BenchColors
import com.bench.ui.BenchType
import com.bench.ui.DialMode
import com.bench.ui.ModeDial
import com.bench.ui.ModeGlyph
import com.bench.ui.Readout
import com.bench.ui.ReadoutRow
import com.bench.ui.SyncKnob
import com.bench.ui.TitleBlock
import com.bench.ui.ToolPager
import com.bench.ui.brushedBrush
import com.bench.ui.fadingTicks
import com.bench.ui.gridCaption
import com.bench.ui.rememberZoomMm
import com.bench.ui.targetMarker
import com.bench.ui.unitVec
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

private const val BOLT_STAGE_DP = 300

@Composable
fun BoltScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    var pcdMm by rememberSaveable { mutableStateOf(60.0) }
    var holes by rememberSaveable { mutableStateOf(6.0) }
    var start by rememberSaveable { mutableStateOf(0.0) }
    var param by rememberSaveable { mutableStateOf(0) }
    var offX by rememberSaveable { mutableStateOf(0.0) }
    var offY by rememberSaveable { mutableStateOf(0.0) }
    var flash by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val n = holes.toInt()
    val res = remember(pcdMm, n, start) { BoltMath.solve(pcdMm, n, start) }
    val window = rememberZoomMm(pcdMm * 1.4, unit.perMm)
    val modes = listOf(
        DialMode(ModeGlyph.DIAMETER, "PCD", unit.show(pcdMm)),
        DialMode(ModeGlyph.COUNT, "HOLES", n.toString()),
        DialMode(ModeGlyph.ANGLE, "START", degText(start))
    )
    LaunchedEffect(flash) {
        if (flash) {
            delay(1500)
            flash = false
        }
    }

    ToolPager(
        unit, onUnit, onBack,
        fullBleed = true,
        paramsBackdrop = { BoltParamsBackdrop(res, pcdMm, unit, window, start, Modifier.fillMaxSize()) },
        calcsBackdrop = { BoltDroBackdrop(res, pcdMm, unit, window, start, offX, offY, Modifier.fillMaxSize()) },
        params = {
            TitleBlock("BOLT\nCIRCLE")
            Spacer(Modifier.weight(1f))
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
            Spacer(Modifier.height(BOLT_STAGE_DP.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { TitleBlock("HOLE\nPOSITIONS") }
                SyncKnob(flash) {
                    val sb = StringBuilder("PCD " + unit.show(pcdMm) + " " + unit.label.lowercase() + ", " + n + " holes")
                    res.holes.forEachIndexed { i, p ->
                        sb.append("\n").append(i + 1).append("  ").append(unit.show(p.x - offX)).append(", ").append(unit.show(p.y - offY))
                    }
                    clipboard.setText(AnnotatedString(sb.toString()))
                    flash = true
                }
            }
            Spacer(Modifier.height(16.dp))
            val rows = ArrayList<ReadoutRow>()
            rows.add(ReadoutRow("⌀", "Pitch Circle", listOf(unit.show(pcdMm))))
            rows.add(ReadoutRow("∠", "Pitch Angle", listOf(degText(res.pitchDeg))))
            rows.add(ReadoutRow("↔", "Hole Spacing", listOf(unit.show(res.chord))))
            rows.add(
                ReadoutRow(
                    "O₁", "Origin Adj", listOf(unit.show(offX), unit.show(offY)),
                    onScrub = { col, d ->
                        val step = d * 0.05
                        if (col == 0) offX = Math.round((offX + step) * 20.0) / 20.0
                        else offY = Math.round((offY + step) * 20.0) / 20.0
                    }
                )
            )
            res.holes.forEachIndexed { i, p ->
                rows.add(ReadoutRow((i + 1).toString(), "Hole " + (i + 1), listOf(unit.show(p.x - offX), unit.show(p.y - offY))))
            }
            Readout(rows, columns = listOf("X", "Y"), cellW = 96.dp, rowPad = 12.dp)
            Spacer(Modifier.height(16.dp))
        }
    )
}

private val dashed = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
private val dotted = PathEffect.dashPathEffect(floatArrayOf(3f, 8f))

// ---------------------------------------------------------------- Parameters page: the flange

@Composable
private fun BoltParamsBackdrop(res: BoltResult, pcd: Double, unit: LenUnit, windowMm: Float, startDeg: Double, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    val shader = remember { RuntimeShader(BRUSHED_SRC) }
    val density = LocalDensity.current
    val top = with(density) { WindowInsets.systemBars.getTop(this).toFloat() }
    val bottom = with(density) { WindowInsets.systemBars.getBottom(this).toFloat() }
    Canvas(modifier) {
        val stage = Rect(28.dp.toPx(), top + 156.dp.toPx(), size.width - 28.dp.toPx(), size.height - bottom - 360.dp.toPx())
        val inner = Rect(stage.left + 20.dp.toPx(), stage.top + 20.dp.toPx(), stage.right - 20.dp.toPx(), stage.bottom - 20.dp.toPx())
        val sc = min(inner.width, inner.height) / windowMm
        val c = inner.center
        val rPx = (pcd / 2.0 * sc).toFloat()
        fun pt(p: Pt) = Offset(c.x + (p.x * sc).toFloat(), c.y - (p.y * sc).toFloat())

        drawRect(BenchColors.ground)
        drawRect(Brush.verticalGradient(listOf(Color(0xFF2A1C19), Color(0x002A1C19)), 0f, size.height * 0.8f))

        // The part: a round flange of brushed metal, a little larger than the pitch circle.
        val flangeR = rPx * 1.3f
        val flange = Path().apply { addOval(Rect(c, flangeR)) }
        val brush = brushedBrush(shader, size.width, size.height)
        clipPath(flange) { drawRect(brush) }
        drawCircle(Color.White.copy(alpha = 0.22f), flangeR, c, style = Stroke(1.5f))

        val axisEnd = c.y + flangeR + 36.dp.toPx()
        drawLine(BenchColors.dim.copy(alpha = 0.55f), Offset(0f, c.y), Offset(size.width, c.y), 1.5f, pathEffect = dotted)
        drawLine(BenchColors.dim.copy(alpha = 0.55f), Offset(c.x, 0f), Offset(c.x, axisEnd), 1.5f, pathEffect = dotted)
        val finest = fadingTicks(c, sc, unit.perMm, Rect(0f, 0f, size.width, axisEnd))
        drawText(tm.measure(gridCaption(finest, unit.label), BenchType.label), BenchColors.dim, Offset(stage.left, stage.bottom + 6.dp.toPx()))

        drawCircle(Color.White.copy(alpha = 0.6f), rPx, c, style = Stroke(1.5f, pathEffect = dashed))
        val h0 = pt(res.holes[0])
        drawLine(BenchColors.accent, c, h0, 1.5f)
        res.holes.forEachIndexed { i, p ->
            val o = pt(p)
            val first = i == 0
            drawCircle(Color(0xFF0E0F12), 8.dp.toPx(), o)
            drawCircle(if (first) BenchColors.accent else Color.White.copy(alpha = 0.55f), 8.dp.toPx(), o, style = Stroke(if (first) 3f else 1.5f))
            drawCircle(if (first) BenchColors.accent else BenchColors.ink, 2.dp.toPx(), o)
            val d = unitVec(Offset(o.x - c.x, o.y - c.y))
            val m = tm.measure((i + 1).toString(), BenchType.label)
            val at = Offset(o.x + d.x * 22.dp.toPx() - m.size.width / 2f, o.y + d.y * 22.dp.toPx() - m.size.height / 2f)
            drawText(m, if (first) BenchColors.accent else BenchColors.dim, at)
        }
        targetMarker(c, 6.dp.toPx())
    }
}

// ---------------------------------------------------------------- Calculations page: the blueprint

@Composable
private fun BoltDroBackdrop(res: BoltResult, pcd: Double, unit: LenUnit, windowMm: Float, startDeg: Double, offX: Double, offY: Double, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    val density = LocalDensity.current
    val topInset = with(density) { WindowInsets.systemBars.getTop(this).toFloat() }
    Canvas(modifier) {
        val top = topInset + 72.dp.toPx()
        val stage = Rect(28.dp.toPx(), top, size.width - 28.dp.toPx(), top + (BOLT_STAGE_DP - 30).dp.toPx())
        val inner = Rect(stage.left + 22.dp.toPx(), stage.top + 14.dp.toPx(), stage.right - 22.dp.toPx(), stage.bottom - 40.dp.toPx())
        val sc = min(inner.width, inner.height) / windowMm
        val c = inner.center
        val rPx = (pcd / 2.0 * sc).toFloat()
        fun pt(p: Pt) = Offset(c.x + (p.x * sc).toFloat(), c.y - (p.y * sc).toFloat())

        drawRect(BenchColors.ground)
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFF6B2E1D).copy(alpha = 0.55f), Color.Transparent),
                Offset(size.width * 0.62f, stage.top + stage.height * 0.3f), size.width * 0.85f
            )
        )
        val axisEnd = stage.bottom
        drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(0f, c.y), Offset(size.width, c.y), 1.5f, pathEffect = dotted)
        drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(c.x, 0f), Offset(c.x, axisEnd), 1.5f, pathEffect = dotted)
        val finest = fadingTicks(c, sc, unit.perMm, Rect(0f, 0f, size.width, axisEnd))
        drawText(tm.measure(gridCaption(finest, unit.label), BenchType.label), BenchColors.dim, Offset(stage.left, stage.bottom - 12.dp.toPx()))

        drawCircle(Color.White.copy(alpha = 0.5f), rPx, c, style = Stroke(1.5f, pathEffect = dashed))
        val count = res.holes.size
        for (i in 0 until count) {
            drawLine(BenchColors.dim.copy(alpha = 0.5f), pt(res.holes[i]), pt(res.holes[(i + 1) % count]), 1.5f)
            drawLine(BenchColors.dim.copy(alpha = 0.2f), c, pt(res.holes[i]), 1.2f)
        }
        // Start angle from the X axis to hole 1, dashed orange at the centre.
        val ar = min(46.dp.toPx(), rPx * 0.8f)
        if (startDeg > 0.5) {
            drawArc(
                BenchColors.accent, (-startDeg).toFloat(), startDeg.toFloat(), false, Offset(c.x - ar, c.y - ar), Size(2 * ar, 2 * ar),
                style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            )
        }
        drawLine(BenchColors.accent, c, pt(res.holes[0]), 1.5f, pathEffect = dashed)
        res.holes.forEachIndexed { i, p ->
            val o = pt(p)
            targetMarker(o, 7.dp.toPx(), if (i == 0) BenchColors.accent else BenchColors.ink)
            val d = unitVec(Offset(o.x - c.x, o.y - c.y))
            val m = tm.measure((i + 1).toString(), BenchType.label)
            drawText(m, BenchColors.accent, Offset(o.x + d.x * 20.dp.toPx() - m.size.width / 2f, o.y + d.y * 20.dp.toPx() - m.size.height / 2f))
        }
        targetMarker(c, 5.dp.toPx())

        // Stacked label for hole 1: orange letter over white coordinates.
        val p1 = res.holes[0]
        val coords = "(" + unit.show(p1.x - offX) + ", " + unit.show(p1.y - offY) + ")"
        val l1 = tm.measure("1", BenchType.label)
        val l2 = tm.measure(coords, BenchType.label)
        val o1 = pt(p1)
        val d1 = unitVec(Offset(o1.x - c.x, o1.y - c.y))
        val w = max(l1.size.width, l2.size.width).toFloat()
        val h = (l1.size.height + l2.size.height).toFloat()
        var x = if (d1.x >= 0f) o1.x + d1.x * 34.dp.toPx() else o1.x + d1.x * 34.dp.toPx() - w
        var y = o1.y + d1.y * 34.dp.toPx() - h / 2f
        x = x.coerceIn(8f, size.width - w - 8f)
        y = y.coerceIn(stage.top, stage.bottom - h)
        drawText(l1, BenchColors.accent, Offset(x + (if (d1.x < 0f) w - l1.size.width else 0f), y))
        drawText(l2, BenchColors.ink, Offset(x + (if (d1.x < 0f) w - l2.size.width else 0f), y + l1.size.height))
        val o = tm.measure("O  (" + unit.show(-offX) + ", " + unit.show(-offY) + ")", BenchType.label)
        drawText(o, BenchColors.dim, Offset(c.x + 8.dp.toPx(), c.y + 10.dp.toPx()))
    }
}
