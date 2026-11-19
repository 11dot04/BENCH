package com.bench.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.bench.engine.*
import com.bench.ui.*

/** Milling feeds and speeds: spindle rpm and table feed from material, cutter size and flutes. */
@Composable
fun FeedsScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    var mat by rememberSaveable { mutableStateOf(0) }
    var dMm by rememberSaveable { mutableStateOf(6.0) }
    var flutes by rememberSaveable { mutableStateOf(2.0) }
    var pct by rememberSaveable { mutableStateOf(100.0) }
    var param by rememberSaveable { mutableStateOf(0) }
    val m = Material.entries[mat]
    val res = FeedsMath.solve(m, dMm, flutes.toInt(), pct)
    val modes = listOf(
        DialMode(ModeGlyph.DIAMETER, "CUTTER", unit.show(dMm)),
        DialMode(ModeGlyph.COUNT, "FLUTES", flutes.toInt().toString()),
        DialMode(ModeGlyph.GAUGE, "SPEED", plain(pct, 0) + "%")
    )
    val metric = unit == LenUnit.MM
    ToolScaffold("FEEDS &\nSPEEDS", unit, onUnit, onBack) {
        Segmented(Material.entries.map { it.label }, mat, Modifier.fillMaxWidth()) { mat = it }
        Gap(6)
        BigValue("SPINDLE", plain(res.rpm, 0) + " rpm")
        when (param) {
            0 -> ModeDial(
                modes, 0, { param = it },
                unit.toDisplay(dMm), unit.dialStep, unit.toDisplay(1.0), unit.toDisplay(60.0),
                { dMm = unit.fromDisplay(it) }, 10.0
            )
            1 -> ModeDial(modes, 1, { param = it }, flutes, 1.0, 1.0, 8.0, { flutes = it }, 3.0, false)
            else -> ModeDial(modes, 2, { param = it }, pct, 5.0, 30.0, 150.0, { pct = it }, 5.0, false)
        }
        Gap(8)
        Readout(
            listOf(
                ReadoutRow("n", "Spindle speed", listOf(plain(res.rpm, 0)), "rpm"),
                ReadoutRow("F", "Table feed", listOf(if (metric) plain(res.feedMmMin, 0) else plain(res.feedMmMin / 25.4, 1)), if (metric) "mm/min" else "in/min"),
                ReadoutRow("Vc", "Surface speed", listOf(if (metric) plain(res.surfMMin, 0) else plain(res.surfMMin / 0.3048, 0)), if (metric) "m/min" else "SFM"),
                ReadoutRow("fz", "Chip load", listOf(if (metric) plain(res.chipMm, 3) else plain(res.chipMm / 25.4, 4)), if (metric) "mm" else "in")
            )
        )
        Note("STARTING POINTS FOR END MILLS. CHECK THE TOOL MAKER'S DATA, AND TRIM WITH THE SPEED DIAL.")
    }
}
