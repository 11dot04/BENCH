package com.bench.screens

import android.graphics.RuntimeShader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.bench.engine.FilletMath
import com.bench.engine.FilletResult
import com.bench.engine.LenUnit
import com.bench.engine.Pt
import com.bench.ui.BText
import com.bench.ui.BenchColors
import com.bench.ui.BenchType
import com.bench.ui.DialMode
import com.bench.ui.ModeDial
import com.bench.ui.ModeGlyph
import com.bench.ui.PressBox
import com.bench.ui.Readout
import com.bench.ui.ReadoutRow
import com.bench.ui.TitleBlock
import com.bench.ui.ToolPager
import com.bench.ui.axisTicks
import com.bench.ui.brushedBrush
import com.bench.ui.BRUSHED_SRC
import com.bench.ui.gridStepMm
import com.bench.ui.niceFit
import com.bench.ui.targetMarker
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private const val DRO_STAGE_DP = 300

@Composable
fun FilletScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    var radiusMm by rememberSaveable { mutableStateOf(12.5) }
    var ori by rememberSaveable { mutableStateOf(45.0) }
    var ang by rememberSaveable { mutableStateOf(50.0) }
    var param by rememberSaveable { mutableStateOf(0) }
    var offX by rememberSaveable { mutableStateOf(0.0) }
    var offY by rememberSaveable { mutableStateOf(0.0) }
    var flash by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val res = remember(radiusMm, ori, ang) { FilletMath.solve(radiusMm, ori, ang) }
    val modes = listOf(
        DialMode(ModeGlyph.GAUGE, "R", unit.show(radiusMm)),
        DialMode(ModeGlyph.SPOKES, "ORI", plainDeg(ori)),
        DialMode(ModeGlyph.ANGLE, "ANG", plainDeg(ang))
    )
    LaunchedEffect(flash) {
        if (flash) {
            delay(1500)
            flash = false
        }
    }
    fun xy(p: Pt) = unit.show(p.x - offX) + ", " + unit.show(p.y - offY)

    ToolPager(
        unit, onUnit, onBack,
        fullBleed = true,
        paramsBackdrop = { ParamsBackdrop(res, radiusMm, unit, Modifier.fillMaxSize()) },
        calcsBackdrop = { DroBackdrop(res, radiusMm, unit, offX, offY, Modifier.fillMaxSize()) },
        params = {
            TitleBlock("FILLET\nCALCULATOR")
            Spacer(Modifier.weight(1f))
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
            Spacer(Modifier.height(DRO_STAGE_DP.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { TitleBlock("PROFILE\n& DRO") }
                SyncKnob(flash) {
                    clipboard.setText(
                        AnnotatedString(
                            "C  " + xy(res.center) + "\nP1 " + xy(res.p1) + "\nP2 " + xy(res.p2) +
                                "\nR  " + unit.show(radiusMm) + " " + unit.label.lowercase()
                        )
                    )
                    flash = true
                }
            }
            Spacer(Modifier.height(16.dp))
            val dash = "–"
            Readout(
                listOf(
                    ReadoutRow("C", "Circle Center", listOf(unit.show(res.center.x - offX), unit.show(res.center.y - offY))),
                    ReadoutRow("P₁", "Tangent 1", listOf(unit.show(res.p1.x - offX), unit.show(res.p1.y - offY))),
                    ReadoutRow("P₂", "Tangent 2", listOf(unit.show(res.p2.x - offX), unit.show(res.p2.y - offY))),
                    ReadoutRow("O", "Origin", listOf(dash, dash), faint = true),
                    ReadoutRow(
                        "O₁", "Origin Adj", listOf(unit.show(offX), unit.show(offY)),
                        onScrub = { col, d ->
                            val step = d * 0.05
                            if (col == 0) offX = Math.round((offX + step) * 20.0) / 20.0
                            else offY = Math.round((offY + step) * 20.0) / 20.0
                        }
                    ),
                    ReadoutRow("⌒", "Arc Length", listOf(unit.show(res.arcLen))),
                    ReadoutRow("↔", "Chord", listOf(unit.show(res.chord)))
                ),
                columns = listOf("X", "Y"), cellW = 96.dp, dividers = false, fade = true, rowPad = 12.dp
            )
            Spacer(Modifier.height(16.dp))
        }
    )
}

private fun plainDeg(d: Double): String = com.bench.engine.degText(d)

/** The knob beside the DRO title. For now it copies the coordinates to the clipboard. */
@Composable
private fun SyncKnob(flash: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BText(if (flash) "COPIED" else "SYNC", BenchType.value, color = BenchColors.accent)
        Spacer(Modifier.width(14.dp))
        PressBox(
            modifier = Modifier.size(64.dp).shadow(12.dp, CircleShape, clip = false, ambientColor = Color.Black, spotColor = Color.Black),
            color = Color(0xFF1B1E23), shape = CircleShape, onClick = onClick
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(
                    BenchColors.accent, -50f, 100f, false,
                    Offset(size.width * 0.28f, size.height * 0.2f), Size(size.width * 0.56f, size.height * 0.6f),
                    style = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

// ---------------------------------------------------------------- geometry shared by both backdrops

private class FView(
    val o: Offset, val c: Offset, val a: Offset, val b: Offset, val rPx: Float,
    val dir1: Offset, val dir2: Offset, val sc: Float, val stepMm: Double
)

private fun unitVec(v: Offset): Offset {
    val l = max(hypot(v.x, v.y), 1e-6f)
    return Offset(v.x / l, v.y / l)
}

private fun normDeg(d: Float): Float {
    var x = d
    while (x > 180f) x -= 360f
    while (x <= -180f) x += 360f
    return x
}

private fun buildView(r: FilletResult, radius: Double, unit: LenUnit, stage: Rect, pad: Float): FView {
    val e1 = Pt(r.p1.x * 1.35, r.p1.y * 1.35)
    val e2 = Pt(r.p2.x * 1.35, r.p2.y * 1.35)
    val minX = minOf(0.0, e1.x, e2.x, r.center.x - radius)
    val maxX = maxOf(0.0, e1.x, e2.x, r.center.x + radius)
    val minY = minOf(0.0, e1.y, e2.y, r.center.y - radius)
    val maxY = maxOf(0.0, e1.y, e2.y, r.center.y + radius)
    val spanX = max(maxX - minX, 1e-6)
    val spanY = max(maxY - minY, 1e-6)
    val availW = stage.width - 2 * pad
    val availH = stage.height - 2 * pad
    val sc = niceFit(spanX, spanY, availW, availH, unit.perMm)
    val left = stage.left + pad + (availW - (spanX * sc).toFloat()) / 2f
    val bottom = stage.bottom - pad - (availH - (spanY * sc).toFloat()) / 2f
    fun sx(x: Double) = left + ((x - minX) * sc).toFloat()
    fun sy(y: Double) = bottom - ((y - minY) * sc).toFloat()
    val o = Offset(sx(0.0), sy(0.0))
    val a = Offset(sx(r.p1.x), sy(r.p1.y))
    val b = Offset(sx(r.p2.x), sy(r.p2.y))
    return FView(
        o, Offset(sx(r.center.x), sy(r.center.y)), a, b, (radius * sc).toFloat(),
        unitVec(Offset(a.x - o.x, a.y - o.y)), unitVec(Offset(b.x - o.x, b.y - o.y)), sc,
        gridStepMm(max(availW, availH), sc, unit.perMm)
    )
}

private fun arcAngles(v: FView): Pair<Float, Float> {
    val s1 = Math.toDegrees(atan2((v.a.y - v.c.y).toDouble(), (v.a.x - v.c.x).toDouble())).toFloat()
    val s2 = Math.toDegrees(atan2((v.b.y - v.c.y).toDouble(), (v.b.x - v.c.x).toDouble())).toFloat()
    return Pair(s1, normDeg(s2 - s1))
}

private val dashed = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
private val dotted = PathEffect.dashPathEffect(floatArrayOf(3f, 8f))

private fun DrawScope.drawText1(tm: TextMeasurer, text: String, at: Offset, color: Color) {
    drawText(tm.measure(text, BenchType.label), color, at)
}

// ---------------------------------------------------------------- Parameters page: the part itself

@Composable
private fun ParamsBackdrop(res: FilletResult, radius: Double, unit: LenUnit, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    val shader = remember { RuntimeShader(BRUSHED_SRC) }
    Canvas(modifier) {
        val top = WindowInsets.systemBars.getTop(this)
        val bottom = WindowInsets.systemBars.getBottom(this)
        val stage = Rect(
            28.dp.toPx(), top + 156.dp.toPx(),
            size.width - 28.dp.toPx(), size.height - bottom - 360.dp.toPx()
        )
        val v = buildView(res, radius, unit, stage, 20.dp.toPx())
        val (s1, sweep12) = arcAngles(v)
        val s2 = s1 + sweep12
        val arcTL = Offset(v.c.x - v.rPx, v.c.y - v.rPx)
        val arcSz = Size(2 * v.rPx, 2 * v.rPx)
        val far = 4f * max(size.width, size.height)
        val far1 = v.o + v.dir1 * far
        val far2 = v.o + v.dir2 * far
        val farB = v.o + unitVec(v.dir1 + v.dir2) * far

        // Ground, with a warm tint in the empty space around the part.
        drawRect(BenchColors.ground)
        drawRect(Brush.verticalGradient(listOf(Color(0xFF2A1C19), Color(0x002A1C19)), 0f, size.height * 0.8f))

        // The part: the wedge between the two lines, corner rounded by the fillet, in brushed metal.
        val slab = Path().apply {
            moveTo(far2.x, far2.y)
            lineTo(v.b.x, v.b.y)
            arcTo(Rect(arcTL, arcSz), s2, -sweep12, false)
            lineTo(far1.x, far1.y)
            lineTo(farB.x, farB.y)
            close()
        }
        val brush = brushedBrush(shader, size.width, size.height)
        clipPath(slab) { drawRect(brush) }
        drawPath(slab, Color.White.copy(alpha = 0.22f), style = Stroke(1.5f))

        // Axes and a halo around the fillet.
        drawLine(BenchColors.dim.copy(alpha = 0.55f), Offset(0f, v.o.y), Offset(size.width, v.o.y), 1.5f, pathEffect = dotted)
        drawLine(BenchColors.dim.copy(alpha = 0.55f), Offset(v.o.x, 0f), Offset(v.o.x, size.height), 1.5f, pathEffect = dotted)
        axisTicks(v.o, (v.stepMm * v.sc).toFloat())
        drawArc(Color.White.copy(alpha = 0.05f), s1, sweep12, false, arcTL, arcSz, style = Stroke(58.dp.toPx(), cap = StrokeCap.Round))
        drawArc(Color.White.copy(alpha = 0.10f), s1, sweep12, false, arcTL, arcSz, style = Stroke(40.dp.toPx(), cap = StrokeCap.Round))

        // Circle, bisector and radius line.
        drawCircle(Color.White.copy(alpha = 0.6f), v.rPx, v.c, style = Stroke(1.5f, pathEffect = dashed))
        val away = unitVec(Offset(v.c.x - v.o.x, v.c.y - v.o.y))
        drawLine(Color.White.copy(alpha = 0.85f), v.c, Offset(v.c.x + away.x * v.rPx * 1.55f, v.c.y + away.y * v.rPx * 1.55f), 1.5f, pathEffect = dashed)
        drawLine(BenchColors.accent, v.c, Offset(v.c.x - away.x * v.rPx, v.c.y - away.y * v.rPx), 1.5f)
        drawArc(BenchColors.accent, s1, sweep12, false, arcTL, arcSz, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))

        targetMarker(v.a, 9.dp.toPx())
        targetMarker(v.b, 9.dp.toPx())
        targetMarker(v.c, 9.dp.toPx())
        targetMarker(v.o, 5.dp.toPx())

        val g = 16.dp.toPx()
        val ob = unitVec(Offset(v.b.x - v.c.x, v.b.y - v.c.y))
        val oa = unitVec(Offset(v.a.x - v.c.x, v.a.y - v.c.y))
        drawText1(tm, "P₂", Offset(v.b.x + ob.x * g - 6.dp.toPx(), v.b.y + ob.y * g - 12.dp.toPx()), BenchColors.dim)
        drawText1(tm, "P₁", Offset(v.a.x + oa.x * g, v.a.y + oa.y * g), BenchColors.dim)
        drawText1(tm, "C", Offset(v.c.x - 22.dp.toPx(), v.c.y - 24.dp.toPx()), BenchColors.dim)
        drawText1(tm, "O", Offset(v.o.x + 8.dp.toPx(), v.o.y + 8.dp.toPx()), BenchColors.dim)
    }
}

// ---------------------------------------------------------------- Calculations page: the blueprint

@Composable
private fun DroBackdrop(res: FilletResult, radius: Double, unit: LenUnit, offX: Double, offY: Double, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val top = WindowInsets.systemBars.getTop(this) + 72.dp.toPx()
        val stage = Rect(28.dp.toPx(), top, size.width - 28.dp.toPx(), top + (DRO_STAGE_DP - 36).dp.toPx())
        val v = buildView(res, radius, unit, stage, 22.dp.toPx())
        val (s1, sweep12) = arcAngles(v)
        val arcTL = Offset(v.c.x - v.rPx, v.c.y - v.rPx)
        val arcSz = Size(2 * v.rPx, 2 * v.rPx)
        val far = 4f * max(size.width, size.height)

        drawRect(BenchColors.ground)
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFF6B2E1D).copy(alpha = 0.55f), Color.Transparent),
                Offset(size.width * 0.62f, stage.top + stage.height * 0.3f), size.width * 0.85f
            )
        )

        drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(0f, v.o.y), Offset(size.width, v.o.y), 1.5f, pathEffect = dotted)
        drawLine(BenchColors.dim.copy(alpha = 0.5f), Offset(v.o.x, 0f), Offset(v.o.x, stage.bottom + 40.dp.toPx()), 1.5f, pathEffect = dotted)
        axisTicks(v.o, (v.stepMm * v.sc).toFloat())

        // Legs to the sharp corner stay thin; the real edges beyond the tangent points are thick.
        drawLine(BenchColors.dim.copy(alpha = 0.6f), v.o, v.a, 1.3f)
        drawLine(BenchColors.dim.copy(alpha = 0.6f), v.o, v.b, 1.3f)
        for (pr in listOf(Pair(v.a, v.dir1), Pair(v.b, v.dir2))) {
            val end = pr.first + pr.second * far
            drawLine(Color.White.copy(alpha = 0.10f), pr.first, end, 14.dp.toPx())
            drawLine(
                Brush.linearGradient(listOf(Color(0xFFEDEDEF), Color(0xFF6E7076)), pr.first, pr.first + pr.second * (size.width * 0.8f)),
                pr.first, end, 4.dp.toPx()
            )
        }

        drawCircle(Color.White.copy(alpha = 0.5f), v.rPx, v.c, style = Stroke(1.5f, pathEffect = dashed))
        drawArc(Color.Black.copy(alpha = 0.35f), s1, sweep12, false, arcTL, arcSz, style = Stroke(26.dp.toPx(), cap = StrokeCap.Round))
        drawArc(BenchColors.ink, s1, sweep12, false, arcTL, arcSz, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
        val toO = unitVec(Offset(v.o.x - v.c.x, v.o.y - v.c.y))
        drawLine(BenchColors.accent, v.c, Offset(v.c.x + toO.x * v.rPx, v.c.y + toO.y * v.rPx), 1.5f, pathEffect = dashed)
        drawCircle(BenchColors.accent, 5.dp.toPx(), v.c)

        // Angle between the X axis and the upper line, dashed orange at the origin.
        val aUp = Math.toDegrees(atan2(v.dir2.y.toDouble(), v.dir2.x.toDouble())).toFloat()
        val aLo = Math.toDegrees(atan2(v.dir1.y.toDouble(), v.dir1.x.toDouble())).toFloat()
        val ar = 52.dp.toPx()
        drawArc(BenchColors.accent, aUp, -aUp, false, Offset(v.o.x - ar, v.o.y - ar), Size(2 * ar, 2 * ar), style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
        for (ang in listOf(aLo, aUp)) {
            val rad = Math.toRadians(ang.toDouble())
            val cx = kotlin.math.cos(rad).toFloat()
            val sy = kotlin.math.sin(rad).toFloat()
            drawLine(BenchColors.accent, Offset(v.o.x + cx * (ar - 5.dp.toPx()), v.o.y + sy * (ar - 5.dp.toPx())), Offset(v.o.x + cx * (ar + 5.dp.toPx()), v.o.y + sy * (ar + 5.dp.toPx())), 1.5f)
        }

        targetMarker(v.a, 9.dp.toPx())
        targetMarker(v.b, 9.dp.toPx())
        targetMarker(v.o, 5.dp.toPx())

        // Stacked labels: orange letter over white coordinates.
        fun xy(p: Pt) = "(" + unit.show(p.x - offX) + ", " + unit.show(p.y - offY) + ")"
        fun label(letter: String, coords: String, at: Offset, out: Offset) {
            val l1 = tm.measure(letter, BenchType.label)
            val l2 = tm.measure(coords, BenchType.label)
            val w = max(l1.size.width, l2.size.width).toFloat()
            val h = (l1.size.height + l2.size.height).toFloat()
            val gap = 14.dp.toPx()
            var x = if (out.x >= 0f) at.x + out.x * gap else at.x + out.x * gap - w
            var y = at.y + out.y * gap - h / 2f
            x = x.coerceIn(8f, size.width - w - 8f)
            y = y.coerceIn(8f, size.height - h - 8f)
            val right = out.x < 0f
            drawText(l1, BenchColors.accent, Offset(if (right) x + w - l1.size.width else x, y))
            drawText(l2, BenchColors.ink, Offset(if (right) x + w - l2.size.width else x, y + l1.size.height))
        }
        label("C", xy(res.center), v.c, unitVec(Offset(0.8f, -0.6f)))
        label("P₂", xy(res.p2), v.b, unitVec(Offset(v.b.x - v.c.x, v.b.y - v.c.y)))
        label("P₁", xy(res.p1), v.a, unitVec(Offset(v.a.x - v.c.x, v.a.y - v.c.y)))
        label("O", "(" + unit.show(-offX) + ", " + unit.show(-offY) + ")", v.o, Offset(0.5f, 1.1f))
    }
}
