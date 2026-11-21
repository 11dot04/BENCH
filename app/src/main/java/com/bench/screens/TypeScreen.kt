package com.bench.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bench.engine.*
import com.bench.ui.*
import kotlin.math.pow

private val ratioNames = listOf("MINOR 2ND", "MAJOR 2ND", "MINOR 3RD", "MAJOR 3RD", "PERFECT 4TH", "AUG 4TH", "PERFECT 5TH", "GOLDEN")
private val ratioValues = listOf(1.067, 1.125, 1.2, 1.25, 1.333, 1.414, 1.5, 1.618)
private val stepNames = mapOf(-2 to "Micro", -1 to "Caption", 0 to "Body", 1 to "Lead", 2 to "H4", 3 to "H3", 4 to "H2", 5 to "H1", 6 to "Display")

/** Modular type scale with leading and line-length, shown as a live specimen. */
@Composable
fun TypeScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    var base by rememberSaveable { mutableStateOf(16.0) }
    var ratioIdx by rememberSaveable { mutableStateOf(3.0) }
    var leading by rememberSaveable { mutableStateOf(1.4) }
    var param by rememberSaveable { mutableStateOf(0) }
    var face by rememberSaveable { mutableStateOf(0) }
    val ratio = ratioValues[ratioIdx.toInt()]
    fun size(k: Int) = base * ratio.pow(k)
    val modes = listOf(
        DialMode(ModeGlyph.GAUGE, "BASE", plain(base, 1) + " sp"),
        DialMode(ModeGlyph.SPOKES, "RATIO", plain(ratio, 3)),
        DialMode(ModeGlyph.DIAMETER, "LEADING", "×" + plain(leading, 2))
    )
    val family = if (face == 0) BenchType.sans else BenchType.mono

    ToolPager(
        unit, onUnit, onBack,
        params = {
            TitleBlock("TYPE\nSCALE")
            Gap(12)
            Segmented(listOf("SANS", "MONO"), face, Modifier.fillMaxWidth()) { face = it }
            Gap(10)
            Column(
                Modifier.weight(1f).fillMaxWidth().heightIn(min = 140.dp)
                    .clip(RoundedCornerShape(20.dp)).background(BenchColors.surface)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                for (k in 4 downTo -1) {
                    val s = size(k)
                    BText((stepNames[k] ?: "").uppercase() + " · " + plain(s, 1), BenchType.label)
                    BasicText(
                        "Bench Aa Qg 0123", maxLines = 1, overflow = TextOverflow.Clip,
                        style = TextStyle(fontFamily = family, fontSize = s.sp, lineHeight = (s * leading).sp, color = BenchColors.ink)
                    )
                    Gap(6)
                }
            }
            when (param) {
                0 -> ModeDial(modes, 0, { param = it }, base, 0.5, 8.0, 32.0, { base = it }, 4.0)
                1 -> ModeDial(modes, 1, { param = it }, ratioIdx, 1.0, 0.0, 7.0, { ratioIdx = it }, 2.0, false)
                else -> ModeDial(modes, 2, { param = it }, leading, 0.05, 1.0, 2.0, { leading = it }, 4.0)
            }
        },
        calcs = {
            TitleBlock("SCALE\nTABLE")
            Gap(6)
            BText(ratioNames[ratioIdx.toInt()] + " · " + plain(ratio, 3) + " · LEADING ×" + plain(leading, 2), BenchType.label)
            Gap(10)
            val rows = ArrayList<ReadoutRow>()
            for (k in 6 downTo -2) {
                val s = size(k)
                rows.add(ReadoutRow((if (k > 0) "+" else "") + k, stepNames[k] ?: "", listOf(plain(s, 1), plain(s * leading, 1)), faint = k == 0 && false))
            }
            Readout(rows, columns = listOf("SIZE", "LINE"), cellW = 84.dp, dividers = false)
            Gap(18)
            BText("COMFORTABLE LINE LENGTH AT BODY SIZE", BenchType.label)
            Gap(6)
            Readout(
                listOf(
                    ReadoutRow("45", "Short end, 45 characters", listOf(plain(45.0 * base * 0.55, 0)), "sp"),
                    ReadoutRow("65", "Ideal, 65 characters", listOf(plain(65.0 * base * 0.55, 0)), "sp"),
                    ReadoutRow("75", "Long end, 75 characters", listOf(plain(75.0 * base * 0.55, 0)), "sp")
                ),
                cellW = 70.dp, dividers = false
            )
            Gap(24)
        }
    )
}
