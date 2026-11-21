package com.bench.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ReadoutRow(
    val sym: String,
    val label: String,
    val cells: List<String>,
    val unit: String = "",
    val faint: Boolean = false,
    /** If set, the value cells can be scrubbed by dragging sideways: (column, pixels dragged). */
    val onScrub: ((Int, Float) -> Unit)? = null
)

/** DRO-style results table: symbol, name, one or more value columns, optional unit. */
@Composable
fun Readout(
    rows: List<ReadoutRow>,
    columns: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    cellW: Dp = 72.dp,
    dividers: Boolean = false,
    fade: Boolean = false,
    rowPad: Dp = 11.dp
) {
    val slots = maxOf(columns.size, rows.maxOfOrNull { it.cells.size } ?: 1)
    val hasUnit = rows.any { it.unit.isNotEmpty() }
    val unitW = 34.dp
    Column(modifier.fillMaxWidth()) {
        if (columns.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Spacer(Modifier.weight(1f))
                columns.forEach { BText(it, BenchType.label, Modifier.width(cellW), align = TextAlign.End) }
                if (hasUnit) Spacer(Modifier.width(unitW))
            }
        }
        rows.forEachIndexed { i, r ->
            val a = if (!fade || i <= 2) 1f else (1f - 0.3f * (i - 2)).coerceAtLeast(0.28f)
            ReadoutLine(r, slots, cellW, unitW, hasUnit, dividers, rowPad, Modifier.alpha(a))
        }
    }
}

@Composable
private fun ReadoutLine(
    r: ReadoutRow, slots: Int, cellW: Dp, unitW: Dp, hasUnit: Boolean,
    dividers: Boolean, rowPad: Dp, modifier: Modifier
) {
    val scrub by rememberUpdatedState(r.onScrub)
    val valueStyle = BenchType.readout.copy(fontWeight = FontWeight.Bold)
    val tint = if (r.faint) BenchColors.line2 else BenchColors.dim
    Row(
        modifier.fillMaxWidth()
            .drawBehind { if (dividers) drawLine(BenchColors.line, Offset(0f, 0f), Offset(size.width, 0f), 1f) }
            .padding(vertical = rowPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BText(r.sym, BenchType.readout, Modifier.width(34.dp), color = tint)
        BText(r.label, BenchType.readout, Modifier.weight(1f), color = tint)
        for (i in 0 until slots) {
            val cellMod = if (r.onScrub != null) {
                Modifier.width(cellW).pointerInput(i) {
                    detectHorizontalDragGestures { change, d ->
                        change.consume()
                        scrub?.invoke(i, d)
                    }
                }
            } else Modifier.width(cellW)
            BText(
                r.cells.getOrElse(i) { "" }, valueStyle, cellMod,
                color = if (r.faint) BenchColors.dim else BenchColors.ink, align = TextAlign.End
            )
        }
        if (hasUnit) BText(r.unit, BenchType.readout, Modifier.width(unitW), color = BenchColors.dim, align = TextAlign.End)
    }
}
