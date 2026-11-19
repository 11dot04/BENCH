package com.bench.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/** A static tile: it never reads data. Tools only touch sensors once opened. */
data class TileSpec(
    val id: String,
    val title: String,
    val w: Int,
    val h: Int,
    val glyph: Glyph,
    val ready: Boolean,
    val shizuku: Boolean = false
)

data class Placed(val spec: TileSpec, val col: Int, val row: Int)

/** First-fit packing on an invisible 4-column grid (1x1, 2x1 and 2x2 tiles mix freely). */
fun packTiles(specs: List<TileSpec>, cols: Int = 4): Pair<List<Placed>, Int> {
    val grid = ArrayList<BooleanArray>()
    val out = ArrayList<Placed>()
    for (t in specs) {
        var r = 0
        var done = false
        while (!done) {
            while (grid.size < r + t.h) grid.add(BooleanArray(cols))
            for (c in 0..cols - t.w) {
                var free = true
                for (dr in 0 until t.h) for (dc in 0 until t.w) if (grid[r + dr][c + dc]) free = false
                if (free) {
                    for (dr in 0 until t.h) for (dc in 0 until t.w) grid[r + dr][c + dc] = true
                    out.add(Placed(t, c, r))
                    done = true
                    break
                }
            }
            if (!done) r++
        }
    }
    val rows = grid.indexOfLast { row -> row.any { it } } + 1
    return Pair(out, rows)
}

@Composable
fun TileSection(specs: List<TileSpec>, onOpen: (String) -> Unit) {
    val packed = remember(specs) { packTiles(specs) }
    val placed = packed.first
    val rows = packed.second
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 8.dp
        val u = (maxWidth - gap * 3) / 4
        Box(Modifier.fillMaxWidth().height(u * rows + gap * (rows - 1))) {
            placed.forEach { p ->
                Tile(
                    p.spec,
                    Modifier
                        .offset(x = (u + gap) * p.col, y = (u + gap) * p.row)
                        .size(u * p.spec.w + gap * (p.spec.w - 1), u * p.spec.h + gap * (p.spec.h - 1)),
                    onOpen
                )
            }
        }
    }
}

@Composable
private fun Tile(spec: TileSpec, modifier: Modifier, onOpen: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val ink = if (spec.ready) BenchColors.ink else BenchColors.dim
    PressBox(
        modifier = modifier,
        color = if (spec.shizuku) BenchColors.ground2 else BenchColors.surface,
        shape = RoundedCornerShape(16.dp),
        enabled = spec.ready,
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onOpen(spec.id) }
    ) {
        if (spec.shizuku) {
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(
                    BenchColors.line2,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                )
            }
        }
        Canvas(Modifier.align(Alignment.BottomStart).padding(10.dp).fillMaxWidth().fillMaxHeight(0.62f)) {
            drawGlyph(spec.glyph, ink)
        }
        BText(spec.title, BenchType.label, Modifier.padding(10.dp), color = if (spec.ready) BenchColors.ink else BenchColors.dim)
        if (!spec.ready && spec.w >= 2) {
            BText("SOON", BenchType.label, Modifier.align(Alignment.TopEnd).padding(10.dp))
        }
    }
}
