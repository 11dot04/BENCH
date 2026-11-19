package com.bench.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bench.engine.*
import com.bench.ui.*
import kotlin.math.abs

/** Any measurement to the nearest fraction of an inch, with the error for each denominator. */
@Composable
fun FractionScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    var mm by rememberSaveable { mutableStateOf(12.7) }
    val inch = mm / 25.4
    val best = FractionMath.nearest(inch, 64)
    val step = if (unit == LenUnit.MM) 0.1 else 0.001
    ToolScaffold("FRACTION\nFINDER", unit, onUnit, onBack) {
        BigValue("NEAREST 1/64", best.first + "\"")
        RulerDial(
            unit.toDisplay(mm), step, unit.toDisplay(0.1), unit.toDisplay(300.0),
            { mm = unit.fromDisplay(it) }, Modifier.fillMaxWidth().height(56.dp)
        )
        BigValue("MEASURED", unit.show(mm) + " " + unit.label.lowercase())
        Gap(6)
        val rows = ArrayList<ReadoutRow>()
        rows.add(ReadoutRow("in", "Decimal inch", listOf(plain(inch, 4)), "in"))
        for (den in listOf(2, 4, 8, 16, 32, 64)) {
            val f = FractionMath.nearest(inch, den)
            val errMm = abs(f.second * 25.4)
            rows.add(ReadoutRow("1/" + den, f.first + "\"", listOf(unit.show(errMm)), "±" + unit.label.lowercase()))
        }
        Readout(rows)
        Note("THE LAST COLUMN IS HOW FAR EACH FRACTION SITS FROM YOUR MEASUREMENT.")
    }
}
