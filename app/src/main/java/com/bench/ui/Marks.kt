package com.bench.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

/** Target-style point marker: ringed dot with two filled quadrants. */
fun DrawScope.targetMarker(at: Offset, rad: Float, tint: Color = BenchColors.ink) {
    drawCircle(BenchColors.ground, rad, at)
    drawCircle(tint, rad, at, style = Stroke(1.5f))
    val tl = Offset(at.x - rad, at.y - rad)
    val sz = Size(2 * rad, 2 * rad)
    drawArc(tint, 0f, 90f, true, tl, sz, style = Fill)
    drawArc(tint, 180f, 90f, true, tl, sz, style = Fill)
}
