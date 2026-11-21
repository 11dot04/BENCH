package com.bench.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.bench.engine.*
import com.bench.ui.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/** Depth of field, field of view and hyperfocal distance for a lens on a given sensor. */
@Composable
fun LensScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    var sensorIdx by rememberSaveable { mutableStateOf(0) }
    var focal by rememberSaveable { mutableStateOf(50.0) }
    var apIdx by rememberSaveable { mutableStateOf(9.0) }
    var distM by rememberSaveable { mutableStateOf(3.0) }
    var param by rememberSaveable { mutableStateOf(0) }
    val sensor = sensorSizes[sensorIdx]
    val n = Optics.fNumber(apIdx)
    val r = remember(sensorIdx, focal, apIdx, distM) { Optics.solve(sensor, focal, n, distM) }
    val modes = listOf(
        DialMode(ModeGlyph.GAUGE, "FOCAL", plain(focal, 0) + " mm"),
        DialMode(ModeGlyph.SPOKES, "APERTURE", Optics.fText(n)),
        DialMode(ModeGlyph.DIAMETER, "DISTANCE", unit.showBig(distM, 1) + " " + unit.bigLabel)
    )
    fun far(v: Double) = if (v.isInfinite()) "∞" else unit.showBig(v, 2)

    ToolPager(
        unit, onUnit, onBack,
        params = {
            TitleBlock("LENS\nCALCULATOR")
            Gap(12)
            Segmented(sensorSizes.map { it.name }, sensorIdx, Modifier.fillMaxWidth()) { sensorIdx = it }
            Gap(10)
            Box(Modifier.weight(1f).fillMaxWidth().heightIn(min = 140.dp)) {
                LensDiagram(r, distM, unit, Modifier.fillMaxSize())
            }
            when (param) {
                0 -> ModeDial(modes, 0, { param = it }, focal, 1.0, 8.0, 600.0, { focal = it }, 10.0)
                1 -> ModeDial(modes, 1, { param = it }, apIdx, 1.0, 0.0, 30.0, { apIdx = it }, 3.0, false)
                else -> ModeDial(
                    modes, 2, { param = it },
                    distM * unit.perMetre, 0.1, 0.3 * unit.perMetre, 200.0 * unit.perMetre,
                    { distM = it / unit.perMetre }, 10.0
                )
            }
        },
        calcs = {
            TitleBlock("DEPTH &\nFIELD")
            Gap(14)
            Readout(
                listOf(
                    ReadoutRow("↤", "Near limit", listOf(far(r.nearM)), unit.bigLabel),
                    ReadoutRow("↦", "Far limit", listOf(far(r.farM)), unit.bigLabel),
                    ReadoutRow("↔", "Total depth", listOf(if (r.farM.isInfinite()) "∞" else unit.showBig(r.farM - r.nearM, 2)), unit.bigLabel),
                    ReadoutRow("H", "Hyperfocal", listOf(far(r.hyperM)), unit.bigLabel),
                    ReadoutRow("∠", "Field of view, wide", listOf(degText(r.hFov))),
                    ReadoutRow("∠", "Field of view, tall", listOf(degText(r.vFov))),
                    ReadoutRow("∠", "Field of view, diagonal", listOf(degText(r.dFov))),
                    ReadoutRow("▭", "Frame at subject", listOf(unit.showBig(r.frameWM, 2) + " × " + unit.showBig(r.frameHM, 2)), unit.bigLabel),
                    ReadoutRow("35", "Full-frame equivalent", listOf(plain(r.equivMm, 0)), "mm"),
                    ReadoutRow("c", "Circle of confusion", listOf(plain(r.cocMm * 1000.0, 1)), "µm", faint = true)
                ),
                cellW = 110.dp
            )
            Gap(18)
            BText("HYPERFOCAL TABLE · " + plain(focal, 0) + " MM · " + sensor.name, BenchType.label)
            Gap(6)
            val rows = ArrayList<ReadoutRow>()
            for (a in listOf(1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0)) {
                val h = Optics.hyperfocalM(sensor, focal, a)
                rows.add(ReadoutRow(Optics.fText(a), "sharp from " + unit.showBig(h / 2.0, 1) + " " + unit.bigLabel, listOf(unit.showBig(h, 1)), unit.bigLabel))
            }
            Readout(rows, columns = listOf("FOCUS AT"), cellW = 96.dp, dividers = false)
            Gap(24)
        }
    )
}

@Composable
private fun LensDiagram(r: OpticsResult, subjectM: Double, unit: LenUnit, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val pad = 24.dp.toPx()
        val camX = pad + 10.dp.toPx()
        val midY = size.height / 2f
        val maxD = if (r.farM.isInfinite()) subjectM * 2.6 else max(r.farM * 1.12, subjectM * 1.4)
        val sx = ((size.width - camX - pad) / maxD).toFloat()
        val half = Math.toRadians(r.hFov / 2.0)
        val t = tan(half)
        val ys = min(sx.toDouble(), (midY - pad) / (t * maxD)).toFloat()
        fun px(d: Double) = camX + (d * sx).toFloat()
        fun wy(d: Double) = (t * d * ys).toFloat()

        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        drawLine(BenchColors.dim.copy(alpha = 0.4f), Offset(camX, midY), Offset(size.width - pad, midY), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 8f)))
        // the wedge
        drawLine(BenchColors.dim, Offset(camX, midY), Offset(px(maxD), midY - wy(maxD)), 2f)
        drawLine(BenchColors.dim, Offset(camX, midY), Offset(px(maxD), midY + wy(maxD)), 2f)
        // depth of field band
        val dn = r.nearM
        val df = if (r.farM.isInfinite()) maxD else min(r.farM, maxD)
        val band = Path().apply {
            moveTo(px(dn), midY - wy(dn)); lineTo(px(df), midY - wy(df))
            lineTo(px(df), midY + wy(df)); lineTo(px(dn), midY + wy(dn)); close()
        }
        drawPath(band, BenchColors.accent.copy(alpha = 0.22f), style = Fill)
        drawLine(BenchColors.accent.copy(alpha = 0.7f), Offset(px(dn), midY - wy(dn)), Offset(px(dn), midY + wy(dn)), 1.5f, pathEffect = dash)
        drawLine(BenchColors.accent.copy(alpha = 0.7f), Offset(px(df), midY - wy(df)), Offset(px(df), midY + wy(df)), 1.5f, pathEffect = dash)
        // subject plane
        drawLine(BenchColors.ink, Offset(px(subjectM), midY - wy(subjectM)), Offset(px(subjectM), midY + wy(subjectM)), 3.dp.toPx())
        targetMarker(Offset(px(subjectM), midY), 7.dp.toPx(), BenchColors.accent)
        // camera
        drawRect(BenchColors.ink, Offset(camX - 12.dp.toPx(), midY - 8.dp.toPx()), androidx.compose.ui.geometry.Size(14.dp.toPx(), 16.dp.toPx()), style = Fill)
        // labels
        fun label(text: String, x: Float, y: Float, color: androidx.compose.ui.graphics.Color) {
            val m = tm.measure(text, BenchType.label)
            drawText(m, color, Offset((x - m.size.width / 2f).coerceIn(4f, size.width - m.size.width - 4f), y))
        }
        label(degText(r.hFov), camX + 40.dp.toPx(), midY - 24.dp.toPx(), BenchColors.dim)
        label("NEAR " + unit.showBig(r.nearM, 1), px(dn), size.height - 24.dp.toPx(), BenchColors.accent)
        label("FAR " + (if (r.farM.isInfinite()) "∞" else unit.showBig(r.farM, 1)), min(px(df), size.width - 36.dp.toPx()), 10.dp.toPx(), BenchColors.accent)
        label(unit.showBig(subjectM, 1) + " " + unit.bigLabel, px(subjectM), midY + wy(subjectM) + 6.dp.toPx(), BenchColors.ink)
    }
}
