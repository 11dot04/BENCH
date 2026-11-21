package com.bench.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import com.bench.engine.LenUnit
import com.bench.engine.ShizukuShell

/** Plain scrolling page used by the simpler tools: header, title, then the tool's content. */
@Composable
fun ToolScaffold(
    title: String,
    unit: LenUnit,
    onUnit: (LenUnit) -> Unit,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2A1C19).copy(alpha = 0.6f), Color.Transparent), 0f, size.height * 0.45f))
            }
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)
    ) {
        BenchHeader("Tools", unit, onUnit, onCrumb = onBack)
        Spacer(Modifier.height(16.dp))
        TitleBlock(title)
        Spacer(Modifier.height(14.dp))
        content()
    }
}

/** Label on the left, one large accent value on the right. */
@Composable
fun BigValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
        BText(label, BenchType.label, Modifier.weight(1f).padding(bottom = 8.dp))
        BText(value, BenchType.numeral)
    }
}

@Composable
fun ActionButton(text: String, modifier: Modifier = Modifier, primary: Boolean = false, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    PressBox(
        modifier = modifier,
        color = if (primary) BenchColors.accent else BenchColors.surface,
        shape = CircleShape,
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }
    ) {
        BText(
            text, BenchType.label,
            Modifier.padding(horizontal = 16.dp, vertical = 13.dp).align(Alignment.Center),
            color = if (primary) BenchColors.ground else BenchColors.ink,
            align = TextAlign.Center
        )
    }
}

@Composable
fun Note(text: String) {
    BText(text, BenchType.label, Modifier.fillMaxWidth().padding(vertical = 10.dp))
}

/** Simple trace or spectrum plot on a card; auto-ranges unless [lo]/[hi] are given. */
@Composable
fun LineChart(values: List<Float>, modifier: Modifier = Modifier, lo: Float? = null, hi: Float? = null) {
    Canvas(modifier) {
        val pad = 14.dp.toPx()
        for (i in 1..3) {
            val y = size.height * i / 4f
            drawLine(BenchColors.line, Offset(0f, y), Offset(size.width, y), 1f)
        }
        if (values.size < 2) return@Canvas
        val mn = lo ?: values.min()
        val mx = hi ?: values.max()
        val span = if (mx - mn > 1e-6f) mx - mn else 1f
        val path = Path()
        var lastX = 0f
        var lastY = 0f
        values.forEachIndexed { i, v ->
            val x = pad + (size.width - 2 * pad) * i / (values.size - 1)
            val y = size.height - pad - (size.height - 2 * pad) * ((v - mn) / span).coerceIn(0f, 1f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            lastX = x
            lastY = y
        }
        drawPath(path, BenchColors.accent, style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(BenchColors.accent, 4.dp.toPx(), Offset(lastX, lastY))
    }
}

@Composable
fun Gap(h: Int = 12) {
    Spacer(Modifier.height(h.dp))
}

/** Shows [content] only when Shizuku is running and allowed; otherwise explains what to do. */
@Composable
fun ColumnScope.ShizukuGate(content: @Composable ColumnScope.() -> Unit) {
    val st by ShizukuShell.state.collectAsState()
    LaunchedEffect(Unit) { ShizukuShell.update() }
    when (st) {
        ShizukuShell.State.READY -> content()
        ShizukuShell.State.NO_PERMISSION -> {
            Note("SHIZUKU IS RUNNING. ALLOW BENCH TO USE IT TO SEE THESE READINGS.")
            ActionButton("ALLOW SHIZUKU", Modifier.fillMaxWidth(), primary = true) { ShizukuShell.requestPermission() }
        }
        ShizukuShell.State.MISSING -> {
            Note("THIS TOOL READS SYSTEM DATA THROUGH SHIZUKU, WHICH IS NOT RUNNING. START SHIZUKU, THEN PRESS CHECK AGAIN.")
            ActionButton("CHECK AGAIN", Modifier.fillMaxWidth(), primary = true) { ShizukuShell.update() }
        }
    }
}

/** The knob beside a DRO title. For now it copies the coordinates to the clipboard. */
@Composable
fun SyncKnob(flash: Boolean, onClick: () -> Unit) {
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
