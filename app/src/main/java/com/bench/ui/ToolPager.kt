package com.bench.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.bench.engine.LenUnit
import kotlinx.coroutines.launch

/** Two vertical pages per tool: Parameters (inputs) and Calculations (full readout). */
@Composable
fun ToolPager(
    unit: LenUnit,
    onUnit: (LenUnit) -> Unit,
    onBack: () -> Unit,
    params: @Composable ColumnScope.() -> Unit,
    calcs: @Composable ColumnScope.() -> Unit
) {
    val state = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    VerticalPager(state = state, modifier = Modifier.fillMaxSize()) { page ->
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 8.dp)) {
            BenchHeader("Tools", unit, onUnit, onCrumb = onBack)
            Spacer(Modifier.height(10.dp))
            if (page == 0) {
                Column(Modifier.weight(1f).fillMaxWidth(), content = params)
            } else {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), content = calcs)
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BText(
                    if (page == 0) "Parameters" else "Calculations", BenchType.label,
                    Modifier.tap { scope.launch { state.animateScrollToPage(1 - page) } }.padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Canvas(Modifier.size(width = 18.dp, height = 14.dp)) {
                    val active = if (page == 0) 0 else 2
                    for (i in 0..2) {
                        val x = i * 7.dp.toPx() + 2f
                        val h = if (i == active) size.height else size.height * 0.55f
                        drawLine(
                            if (i == active) BenchColors.ink else BenchColors.dim,
                            Offset(x, size.height), Offset(x, size.height - h), 2.5f, StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}
