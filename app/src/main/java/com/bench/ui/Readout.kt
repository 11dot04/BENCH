package com.bench.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class ReadoutRow(
    val sym: String,
    val label: String,
    val cells: List<String>,
    val unit: String = "",
    val faint: Boolean = false
)

/** DRO-style results table: symbol, name, one or more value columns, optional unit. */
@Composable
fun Readout(rows: List<ReadoutRow>, columns: List<String> = emptyList(), modifier: Modifier = Modifier) {
    val slots = maxOf(columns.size, rows.maxOfOrNull { it.cells.size } ?: 1)
    val hasUnit = rows.any { it.unit.isNotEmpty() }
    val cellW = 72.dp
    val unitW = 30.dp
    val valueStyle = BenchType.readout.copy(fontWeight = FontWeight.SemiBold)
    Column(modifier.fillMaxWidth()) {
        if (columns.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Spacer(Modifier.weight(1f))
                columns.forEach { BText(it, BenchType.label, Modifier.width(cellW), align = TextAlign.End) }
                if (hasUnit) Spacer(Modifier.width(unitW))
            }
        }
        rows.forEach { r ->
            Row(
                Modifier.fillMaxWidth()
                    .drawBehind { drawLine(BenchColors.line, Offset(0f, 0f), Offset(size.width, 0f), 1f) }
                    .padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tint = if (r.faint) BenchColors.line2 else BenchColors.dim
                BText(r.sym, BenchType.readout, Modifier.width(28.dp), color = tint)
                BText(r.label, BenchType.readout, Modifier.weight(1f), color = tint)
                for (i in 0 until slots) {
                    BText(
                        r.cells.getOrElse(i) { "" }, valueStyle, Modifier.width(cellW),
                        color = if (r.faint) BenchColors.dim else BenchColors.ink, align = TextAlign.End
                    )
                }
                if (hasUnit) BText(r.unit, BenchType.readout, Modifier.width(unitW), color = BenchColors.dim, align = TextAlign.End)
            }
        }
        Spacer(Modifier.padding(top = 1.dp))
    }
}
