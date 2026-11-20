package com.bench.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.bench.engine.*
import com.bench.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val CMD =
    "for f in cycle_count charge_full charge_full_design capacity temp time_to_full_now; do " +
        "echo \"\$f=\$(cat /sys/class/power_supply/battery/\$f 2>/dev/null)\"; done; dumpsys battery"

private fun parse(out: String): Map<String, String> {
    val m = HashMap<String, String>()
    for (raw in out.lines()) {
        val line = raw.trim()
        if (line.isEmpty()) continue
        val eq = line.indexOf('=')
        val colon = line.indexOf(": ")
        if (eq > 0 && (colon < 0 || eq < colon)) m[line.substring(0, eq).lowercase()] = line.substring(eq + 1).trim()
        else if (colon > 0) m[line.substring(0, colon).lowercase()] = line.substring(colon + 2).trim()
    }
    return m
}

private fun mah(v: Double): Double = if (v > 100000.0) v / 1000.0 else v

/** True battery health from the kernel's own full and design capacity, plus cycle count. Needs Shizuku. */
@Composable
fun BatteryScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    ToolScaffold("BATTERY\nHEALTH", unit, onUnit, onBack) { ShizukuGate { BatteryBody() } }
}

@Composable
private fun BatteryBody() {
    var info by remember { mutableStateOf<Map<String, String>?>(null) }
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(tick) {
        while (true) {
            val out = withContext(Dispatchers.IO) { ShizukuShell.run(CMD) }
            if (out != null) info = parse(out)
            delay(5000)
        }
    }
    val m = info
    if (m == null) {
        Note("READING…")
        return
    }
    val full = m["charge_full"]?.toDoubleOrNull()
    val design = m["charge_full_design"]?.toDoubleOrNull()
    val healthPct = if (full != null && design != null && design > 0.0 && full > 0.0) full / design * 100.0 else null
    val cycles = m["cycle_count"]?.toIntOrNull()
    val temp = (m["temperature"] ?: m["temp"])?.toDoubleOrNull()
    val level = m["level"]
    val health = when (m["health"]) {
        "2" -> "GOOD"; "3" -> "OVERHEAT"; "4" -> "DEAD"; "5" -> "OVER VOLTAGE"; "6" -> "FAILURE"; "7" -> "COLD"
        else -> "—"
    }
    val status = when (m["status"]) {
        "2" -> "CHARGING"; "3" -> "DISCHARGING"; "4" -> "NOT CHARGING"; "5" -> "FULL"
        else -> "—"
    }
    BigValue("CAPACITY HEALTH", if (healthPct == null) "—" else plain(healthPct, 0) + " %")
    Readout(
        listOf(
            ReadoutRow("%", "Capacity vs new", listOf(if (healthPct == null) "—" else plain(healthPct, 1)), "%"),
            ReadoutRow("Q", "Full capacity now", listOf(if (full == null) "—" else plain(mah(full), 0)), "mAh"),
            ReadoutRow("Q₀", "Design capacity", listOf(if (design == null) "—" else plain(mah(design), 0)), "mAh"),
            ReadoutRow("↻", "Charge cycles", listOf(cycles?.toString() ?: "—")),
            ReadoutRow("T", "Temperature", listOf(if (temp == null) "—" else plain(temp / 10.0, 1)), "°C"),
            ReadoutRow("L", "Level", listOf(level ?: "—"), "%"),
            ReadoutRow("s", "State", listOf(status)),
            ReadoutRow("h", "Reported health", listOf(health))
        )
    )
    Gap(8)
    ActionButton("REFRESH NOW", Modifier.fillMaxWidth()) { tick++ }
    Note("WHICH LINES APPEAR DEPENDS ON YOUR PHONE'S MAKER. A BLANK MEANS THIS PHONE DOES NOT EXPOSE IT.")
}
