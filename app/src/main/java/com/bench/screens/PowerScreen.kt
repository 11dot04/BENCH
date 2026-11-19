package com.bench.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bench.engine.*
import com.bench.ui.*

/** Live battery power (V x A) with charge-time estimates; plain BatteryManager, no Shizuku. */
@Composable
fun PowerScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var now by remember { mutableStateOf<PowerReading?>(null) }
    val trace = remember { mutableStateListOf<Float>() }
    LaunchedEffect(Unit) {
        PowerReader.flow(ctx).collect { r ->
            now = r
            trace.add(r.watts.toFloat())
            if (trace.size > 120) trace.removeAt(0)
        }
    }
    val r = now
    ToolScaffold("POWER\nFLOW", unit, onUnit, onBack) {
        BigValue(if (r == null) "POWER" else if (r.charging) "CHARGING" else "DRAWING", if (r == null) "—" else plain(r.watts, 2) + " W")
        LineChart(trace, Modifier.fillMaxWidth().height(130.dp), lo = 0f)
        Gap(12)
        if (r != null) {
            val eta = if (r.etaHours <= 0.0) "—" else plain(r.etaHours, 1) + " h"
            Readout(
                listOf(
                    ReadoutRow("V", "Voltage", listOf(plain(r.volts, 2)), "V"),
                    ReadoutRow("I", "Current", listOf(plain(r.milliamps, 0)), "mA"),
                    ReadoutRow("P", "Power", listOf(plain(r.watts, 2)), "W"),
                    ReadoutRow("%", "Level", listOf(r.level.toString()), "%"),
                    ReadoutRow("T", "Temperature", listOf(plain(r.tempC, 1)), "°C"),
                    ReadoutRow("Q", "Charge now", listOf(if (r.chargeMah > 0) plain(r.chargeMah, 0) else "—"), "mAh"),
                    ReadoutRow("Qf", "Estimated full", listOf(if (r.fullMah > 0) plain(r.fullMah, 0) else "—"), "mAh"),
                    ReadoutRow("t", if (r.charging) "Time to full" else "Time left", listOf(eta))
                )
            )
        }
        Note("ESTIMATES FROM THE BATTERY'S OWN COUNTERS. NUMBERS SWING WITH LOAD; BATTERY HEALTH AND CYCLE COUNT NEED SHIZUKU, COMING LATER.")
    }
}
