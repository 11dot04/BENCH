package com.bench.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.bench.engine.*
import com.bench.ui.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/** Resistor divider, RC filter response and E-series component finder, with part values picked from real series. */
@Composable
fun CircuitScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf(0) }
    ToolScaffold("CIRCUIT\nBENCH", unit, onUnit, onBack) {
        Segmented(listOf("DIVIDER", "FILTER", "RESISTOR"), mode, Modifier.fillMaxWidth()) { mode = it }
        Gap(12)
        when (mode) {
            0 -> DividerTool()
            1 -> FilterTool()
            else -> ResistorTool()
        }
    }
}

// ------------------------------------------------------------------ divider

@Composable
private fun DividerTool() {
    var i1 by rememberSaveable { mutableStateOf(96.0) }
    var i2 by rememberSaveable { mutableStateOf(96.0) }
    var vin by rememberSaveable { mutableStateOf(5.0) }
    var edit by rememberSaveable { mutableStateOf(0) }
    val r1 = Series.at(Series.e24, i1.toInt())
    val r2 = Series.at(Series.e24, i2.toInt())
    val vout = vin * r2 / (r1 + r2)
    val cur = vin / (r1 + r2)
    BigValue("OUTPUT", plain(vout, 3) + " V")
    DividerSketch(r1, r2, vin, vout, Modifier.fillMaxWidth().height(220.dp))
    Gap(10)
    Segmented(listOf("R1 " + Fmt.ohms(r1), "R2 " + Fmt.ohms(r2), "VIN " + plain(vin, 1)), edit, Modifier.fillMaxWidth()) { edit = it }
    Gap(4)
    when (edit) {
        0 -> RulerDial(i1, 1.0, 0.0, 167.0, { i1 = it }, Modifier.fillMaxWidth().height(56.dp))
        1 -> RulerDial(i2, 1.0, 0.0, 167.0, { i2 = it }, Modifier.fillMaxWidth().height(56.dp))
        else -> RulerDial(vin, 0.1, 0.1, 60.0, { vin = it }, Modifier.fillMaxWidth().height(56.dp))
    }
    Gap(10)
    Readout(
        listOf(
            ReadoutRow("Vo", "Output voltage", listOf(plain(vout, 3)), "V"),
            ReadoutRow("k", "Ratio", listOf(plain(r2 / (r1 + r2), 4))),
            ReadoutRow("I", "Current", listOf(plain(cur * 1000.0, 3)), "mA"),
            ReadoutRow("P₁", "Power in R1", listOf(plain(cur * cur * r1 * 1000.0, 3)), "mW"),
            ReadoutRow("P₂", "Power in R2", listOf(plain(cur * cur * r2 * 1000.0, 3)), "mW"),
            ReadoutRow("Rt", "Source resistance seen", listOf(Fmt.ohms(r1 * r2 / (r1 + r2))))
        ),
        cellW = 110.dp
    )
    Note("PARTS STEP THROUGH THE E24 SERIES, SO EVERY VALUE IS ONE YOU CAN BUY.")
}

@Composable
private fun DividerSketch(r1: Double, r2: Double, vin: Double, vout: Double, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier.clip(RoundedCornerShape(20.dp)).background(BenchColors.surface)) {
        val x = size.width * 0.38f
        val top = 26.dp.toPx()
        val bot = size.height - 26.dp.toPx()
        val mid = size.height / 2f
        val w = 2.5f
        drawLine(BenchColors.ink, Offset(x - 40.dp.toPx(), top), Offset(x, top), w)
        drawLine(BenchColors.ink, Offset(x, top), Offset(x, mid - 58.dp.toPx()), w)
        zigzag(x, mid - 58.dp.toPx(), mid - 10.dp.toPx(), BenchColors.ink)
        drawLine(BenchColors.ink, Offset(x, mid - 10.dp.toPx()), Offset(x, mid + 10.dp.toPx()), w)
        zigzag(x, mid + 10.dp.toPx(), mid + 58.dp.toPx(), BenchColors.ink)
        drawLine(BenchColors.ink, Offset(x, mid + 58.dp.toPx()), Offset(x, bot), w)
        drawLine(BenchColors.ink, Offset(x - 14.dp.toPx(), bot), Offset(x + 14.dp.toPx(), bot), w)
        drawLine(BenchColors.dim, Offset(x - 8.dp.toPx(), bot + 6.dp.toPx()), Offset(x + 8.dp.toPx(), bot + 6.dp.toPx()), w)
        drawLine(BenchColors.accent, Offset(x, mid), Offset(size.width - 70.dp.toPx(), mid), w)
        drawCircle(BenchColors.accent, 5.dp.toPx(), Offset(x, mid))
        fun t(text: String, at: Offset, c: Color) = drawText(tm.measure(text, BenchType.label), c, at)
        t("VIN " + plain(vin, 1) + " V", Offset(x - 40.dp.toPx(), top - 18.dp.toPx()), BenchColors.dim)
        t("R1", Offset(x + 24.dp.toPx(), mid - 44.dp.toPx()), BenchColors.dim)
        t(Fmt.ohms(r1), Offset(x + 24.dp.toPx(), mid - 30.dp.toPx()), BenchColors.ink)
        t("R2", Offset(x + 24.dp.toPx(), mid + 22.dp.toPx()), BenchColors.dim)
        t(Fmt.ohms(r2), Offset(x + 24.dp.toPx(), mid + 36.dp.toPx()), BenchColors.ink)
        t("VOUT", Offset(size.width - 66.dp.toPx(), mid - 22.dp.toPx()), BenchColors.dim)
        t(plain(vout, 2) + " V", Offset(size.width - 66.dp.toPx(), mid - 8.dp.toPx()), BenchColors.accent)
    }
}

private fun DrawScope.zigzag(x: Float, y0: Float, y1: Float, color: Color) {
    val p = Path()
    p.moveTo(x, y0)
    val n = 6
    for (i in 1..n) {
        val y = y0 + (y1 - y0) * i / (n + 1)
        p.lineTo(x + (if (i % 2 == 1) 9.dp.toPx() else -9.dp.toPx()), y)
    }
    p.lineTo(x, y1)
    drawPath(p, color, style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// ------------------------------------------------------------------ filter

@Composable
private fun FilterTool() {
    var high by rememberSaveable { mutableStateOf(0) }
    var ir by rememberSaveable { mutableStateOf(96.0) }
    var ic by rememberSaveable { mutableStateOf(36.0) }
    var edit by rememberSaveable { mutableStateOf(0) }
    val r = Series.at(Series.e24, ir.toInt())
    val cPf = Series.at(Series.e12, ic.toInt())
    val c = cPf * 1e-12
    val fc = 1.0 / (2.0 * PI * r * c)
    BigValue("CUTOFF", Fmt.hertz(fc))
    BodePlot(fc, high == 1, Modifier.fillMaxWidth().height(200.dp))
    Gap(10)
    Segmented(listOf("LOW-PASS", "HIGH-PASS"), high, Modifier.fillMaxWidth()) { high = it }
    Gap(8)
    Segmented(listOf("R " + Fmt.ohms(r), "C " + Fmt.farads(cPf)), edit, Modifier.fillMaxWidth()) { edit = it }
    Gap(4)
    if (edit == 0) RulerDial(ir, 1.0, 0.0, 167.0, { ir = it }, Modifier.fillMaxWidth().height(56.dp))
    else RulerDial(ic, 1.0, 0.0, 107.0, { ic = it }, Modifier.fillMaxWidth().height(56.dp))
    Gap(10)
    Readout(
        listOf(
            ReadoutRow("fc", "Cutoff, −3 dB", listOf(Fmt.hertz(fc))),
            ReadoutRow("τ", "Time constant", listOf(Fmt.seconds(r * c))),
            ReadoutRow("φ", "Phase at cutoff", listOf(if (high == 1) "+45°" else "−45°")),
            ReadoutRow("dB", "Roll-off", listOf("20 dB / decade")),
            ReadoutRow("t", "Rise time, 10–90%", listOf(Fmt.seconds(2.2 * r * c)), faint = true)
        ),
        cellW = 130.dp
    )
    Note("FIRST-ORDER RC FILTER. R AND C STEP THROUGH E24 AND E12 PREFERRED VALUES.")
}

@Composable
private fun BodePlot(fc: Double, highPass: Boolean, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier.clip(RoundedCornerShape(20.dp)).background(BenchColors.surface)) {
        val padL = 38.dp.toPx()
        val padR = 14.dp.toPx()
        val padT = 14.dp.toPx()
        val padB = 26.dp.toPx()
        val w = size.width - padL - padR
        val h = size.height - padT - padB
        val dbTop = 3.0
        val dbBot = -60.0
        fun fx(f: Double) = padL + (w * (log10(f) - 0.0) / 7.0).toFloat()
        fun dy(db: Double) = padT + (h * (dbTop - db) / (dbTop - dbBot)).toFloat()
        for (d in 0..7) {
            drawLine(BenchColors.line, Offset(fx(10.0.pow(d)), padT), Offset(fx(10.0.pow(d)), padT + h), 1f)
            if (d % 2 == 0) {
                val lbl = tm.measure(Fmt.hertz(10.0.pow(d)).replace(" ", ""), BenchType.label)
                drawText(lbl, BenchColors.dim, Offset(fx(10.0.pow(d)) - lbl.size.width / 2f, padT + h + 6.dp.toPx()))
            }
        }
        for (db in listOf(0, -20, -40, -60)) {
            drawLine(BenchColors.line, Offset(padL, dy(db.toDouble())), Offset(padL + w, dy(db.toDouble())), 1f)
            val lbl = tm.measure(db.toString(), BenchType.label)
            drawText(lbl, BenchColors.dim, Offset(padL - lbl.size.width - 6.dp.toPx(), dy(db.toDouble()) - lbl.size.height / 2f))
        }
        val path = Path()
        var started = false
        var f = 1.0
        while (f <= 1e7) {
            val ratio = if (highPass) fc / f else f / fc
            val db = -10.0 * log10(1.0 + ratio * ratio)
            val y = dy(db.coerceAtLeast(dbBot))
            if (!started) {
                path.moveTo(fx(f), y)
                started = true
            } else path.lineTo(fx(f), y)
            f *= 1.05
        }
        drawPath(path, BenchColors.accent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val fcClamped = fc.coerceIn(1.0, 1e7)
        drawLine(BenchColors.ink.copy(alpha = 0.7f), Offset(fx(fcClamped), padT), Offset(fx(fcClamped), padT + h), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        targetMarker(Offset(fx(fcClamped), dy(-3.0103)), 6.dp.toPx(), BenchColors.accent)
    }
}

// ------------------------------------------------------------------ resistor finder

@Composable
private fun ResistorTool() {
    var x by rememberSaveable { mutableStateOf(4.0) } // log10(ohms)
    val target = 10.0.pow(x)
    val e24 = Series.nearest(Series.e24, target)
    val e12 = Series.nearest(Series.e12, target)
    val e96 = Series.nearest(Series.e96, target)
    fun err(v: Double) = (v / target - 1.0) * 100.0
    BigValue("TARGET", Fmt.ohms(target))
    ResistorDrawing(e24, Modifier.fillMaxWidth().height(110.dp))
    Gap(8)
    RulerDial(x, 0.01, 0.0, 7.0, { x = it }, Modifier.fillMaxWidth().height(56.dp))
    Gap(10)
    Readout(
        listOf(
            ReadoutRow("E12", "Nearest 10% part", listOf(Fmt.ohms(e12), plain(err(e12), 1) + " %")),
            ReadoutRow("E24", "Nearest 5% part", listOf(Fmt.ohms(e24), plain(err(e24), 1) + " %")),
            ReadoutRow("E96", "Nearest 1% part", listOf(Fmt.ohms(e96), plain(err(e96), 2) + " %"))
        ),
        columns = listOf("VALUE", "OFF BY"), cellW = 96.dp
    )
    Gap(6)
    val b = ColorCode.bands(e24)
    val mult = if (b.third >= 0) ColorCode.names[b.third.coerceAtMost(9)] else if (b.third == -1) "GOLD" else "SILVER"
    Readout(
        listOf(
            ReadoutRow("1", "First band", listOf(ColorCode.names[b.first])),
            ReadoutRow("2", "Second band", listOf(ColorCode.names[b.second])),
            ReadoutRow("×", "Multiplier", listOf(mult)),
            ReadoutRow("±", "Tolerance", listOf("GOLD 5%"))
        ),
        cellW = 110.dp, dividers = false
    )
    Note("SLIDE THE DIAL TO ANY VALUE. THE BANDS SHOWN ARE FOR THE NEAREST E24 PART.")
}

@Composable
private fun ResistorDrawing(ohms: Double, modifier: Modifier) {
    val b = ColorCode.bands(ohms)
    fun band(code: Int): Color = when {
        code >= 0 -> Color(ColorCode.argb[code.coerceAtMost(9)])
        code == -1 -> Color(ColorCode.GOLD)
        else -> Color(ColorCode.SILVER)
    }
    Canvas(modifier.clip(RoundedCornerShape(20.dp)).background(BenchColors.surface)) {
        val midY = size.height / 2f
        val bodyW = size.width * 0.5f
        val bodyH = 46.dp.toPx()
        val left = (size.width - bodyW) / 2f
        drawLine(BenchColors.dim, Offset(16.dp.toPx(), midY), Offset(size.width - 16.dp.toPx(), midY), 3.dp.toPx(), StrokeCap.Round)
        drawRoundRect(Color(0xFFD9C7A0), Offset(left, midY - bodyH / 2f), Size(bodyW, bodyH), androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
        val bw = 9.dp.toPx()
        val xs = listOf(0.17f, 0.31f, 0.45f, 0.78f)
        val cols = listOf(band(b.first), band(b.second), band(b.third), Color(ColorCode.GOLD))
        for (i in 0..3) drawRect(cols[i], Offset(left + bodyW * xs[i], midY - bodyH / 2f + 1f), Size(bw, bodyH - 2f))
    }
}
