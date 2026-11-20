package com.bench.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import com.bench.engine.LenUnit

@Composable
fun Modifier.tap(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)

/** A surface that squashes and springs back when pressed. */
@Composable
fun PressBox(
    modifier: Modifier = Modifier,
    color: Color = BenchColors.surface,
    shape: Shape = RoundedCornerShape(16.dp),
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "press"
    )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(color)
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick),
        content = content
    )
}

/** Chunky six-lobed cog, as in the concept. */
@Composable
fun Cog(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val s = size.minDimension
        drawCircle(BenchColors.ink, s * 0.30f, c)
        for (i in 0 until 6) {
            val a = Math.toRadians(i * 60.0 - 90.0)
            drawCircle(BenchColors.ink, s * 0.15f, Offset(c.x + s * 0.32f * cos(a).toFloat(), c.y + s * 0.32f * sin(a).toFloat()))
        }
        drawCircle(BenchColors.ground, s * 0.14f, c)
    }
}

@Composable
fun BenchHeader(crumb: String, unit: LenUnit, onUnit: (LenUnit) -> Unit, onCrumb: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Cog(Modifier.size(34.dp))
        Row(Modifier.weight(1f).padding(start = 14.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            val mod = if (onCrumb != null) Modifier.tap(onCrumb) else Modifier
            Row(mod.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(width = 36.dp, height = 16.dp)) {
                    val y = size.height / 2f
                    val tipX = size.width - 8.dp.toPx()
                    drawLine(BenchColors.dim, Offset(0f, y), Offset(tipX, y), 1.5f)
                    drawLine(BenchColors.dim, Offset(tipX, y), Offset(size.width, y - 7.dp.toPx()), 1.5f)
                    drawLine(BenchColors.dim, Offset(tipX, y), Offset(size.width, y + 7.dp.toPx()), 1.5f)
                }
                Spacer(Modifier.width(6.dp))
                BText(crumb, BenchType.readout, color = BenchColors.dim)
            }
        }
        // Stacked unit switch; the lit one carries an orange bar that runs out to the screen edge.
        Column(Modifier.offset(x = 20.dp), horizontalAlignment = Alignment.End) {
            listOf(LenUnit.IN, LenUnit.MM).forEach { u ->
                val on = u == unit
                Row(Modifier.tap { onUnit(u) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    BText(u.label, BenchType.readout, color = if (on) BenchColors.ink else BenchColors.dim)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.width(30.dp).height(3.dp).background(if (on) BenchColors.accent else Color.Transparent))
                }
            }
        }
    }
}

@Composable
fun Hatch(modifier: Modifier = Modifier) {
    Canvas(modifier.clipToBounds()) {
        var x = -size.height
        while (x < size.width) {
            drawLine(BenchColors.line2, Offset(x, size.height), Offset(x + size.height, 0f), 2f)
            x += 12f
        }
    }
}

@Composable
fun TitleBlock(title: String, sub: String? = null) {
    Box {
        Hatch(Modifier.size(width = 40.dp, height = 56.dp).align(Alignment.BottomStart))
        Column(Modifier.padding(start = 30.dp)) {
            if (sub != null) BText(sub, BenchType.label)
            BText(title, BenchType.title)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BText(text, BenchType.label)
        Box(Modifier.weight(1f).padding(start = 8.dp).height(1.dp).background(BenchColors.line))
    }
}

@Composable
fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    PressBox(
        modifier = modifier,
        color = if (selected) BenchColors.accent else BenchColors.surface,
        shape = CircleShape,
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }
    ) {
        BText(
            text, BenchType.label,
            Modifier.padding(horizontal = 14.dp, vertical = 11.dp).align(Alignment.Center),
            color = if (selected) BenchColors.ground else BenchColors.dim,
            align = TextAlign.Center
        )
    }
}

/** Parameter selector tile: label on top, current value below. */
@Composable
fun ParamTile(label: String, value: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    PressBox(
        modifier = modifier,
        color = if (selected) BenchColors.surfaceHigh else BenchColors.surface,
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }
    ) {
        if (selected) Box(Modifier.align(Alignment.TopStart).padding(12.dp).size(width = 14.dp, height = 3.dp).background(BenchColors.accent))
        Column(Modifier.padding(12.dp).align(Alignment.BottomStart)) {
            BText(label, BenchType.label)
            BText(value, BenchType.value)
        }
    }
}

/** Two- or three-way switch: one pill, the chosen segment lit. */
@Composable
fun Segmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(modifier.clip(CircleShape).background(BenchColors.surface).padding(3.dp)) {
        options.forEachIndexed { i, o ->
            Box(
                Modifier.weight(1f).clip(CircleShape)
                    .background(if (i == selected) BenchColors.accent else Color.Transparent)
                    .tap { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                BText(o, BenchType.label, color = if (i == selected) BenchColors.ground else BenchColors.dim, align = TextAlign.Center)
            }
        }
    }
}
