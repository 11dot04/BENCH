package com.bench.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bench.engine.*
import com.bench.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val CMD =
    "for z in /sys/class/thermal/thermal_zone*; do echo \"Z \$(cat \$z/type 2>/dev/null)|\$(cat \$z/temp 2>/dev/null)\"; done; " +
        "dumpsys thermalservice 2>/dev/null | grep -i -m4 'status'"

private class Zone(val name: String, val tempC: Double)

/** Every thermal sensor the kernel exposes, hottest first, plus the system's throttling state. */
@Composable
fun ThermalScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    ToolScaffold("THERMAL\nMAP", unit, onUnit, onBack) { ShizukuGate { ThermalBody() } }
}

@Composable
private fun ThermalBody() {
    var zones by remember { mutableStateOf<List<Zone>>(emptyList()) }
    var status by remember { mutableStateOf("—") }
    val trace = remember { mutableStateListOf<Float>() }
    LaunchedEffect(Unit) {
        while (true) {
            val out = withContext(Dispatchers.IO) { ShizukuShell.run(CMD) }
            if (out != null) {
                val list = ArrayList<Zone>()
                var st = "—"
                for (line in out.lines()) {
                    if (line.startsWith("Z ")) {
                        val p = line.substring(2).split("|")
                        if (p.size >= 2) {
                            val raw = p[1].trim().toDoubleOrNull() ?: continue
                            val c = if (abs(raw) > 1000.0) raw / 1000.0 else raw
                            if (c in 5.0..130.0) list.add(Zone(p[0].trim().ifEmpty { "zone" }, c))
                        }
                    } else if (st == "—") {
                        val d = Regex("(?i)status\\D*(-?\\d)").find(line)?.groupValues?.get(1)?.toIntOrNull()
                        if (d != null) st = when (d) {
                            0 -> "NONE"; 1 -> "LIGHT"; 2 -> "MODERATE"; 3 -> "SEVERE"
                            4 -> "CRITICAL"; 5 -> "EMERGENCY"; 6 -> "SHUTDOWN"; else -> "—"
                        }
                    }
                }
                list.sortByDescending { it.tempC }
                zones = list
                status = st
                if (list.isNotEmpty()) trace.add(list[0].tempC.toFloat())
                if (trace.size > 90) trace.removeAt(0)
            }
            delay(2000)
        }
    }
    val hot = zones.firstOrNull()
    BigValue("HOTTEST", if (hot == null) "—" else plain(hot.tempC, 1) + " °C")
    LineChart(trace, Modifier.fillMaxWidth().height(110.dp))
    Gap(8)
    val rows = ArrayList<ReadoutRow>()
    rows.add(ReadoutRow("!", "Throttling state", listOf(status)))
    if (hot != null) rows.add(ReadoutRow("↑", "Hottest sensor", listOf(hot.name)))
    zones.take(12).forEach { rows.add(ReadoutRow("T", it.name, listOf(plain(it.tempC, 1)), "°C", faint = true)) }
    Readout(rows)
    Note("SENSOR NAMES COME STRAIGHT FROM YOUR PHONE'S KERNEL. EVERY PHONE NAMES ITS ZONES DIFFERENTLY.")
}
