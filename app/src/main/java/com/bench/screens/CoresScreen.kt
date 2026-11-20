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

private const val CMD =
    "for c in /sys/devices/system/cpu/cpu[0-9]*; do n=\$(basename \$c); " +
        "echo \"F \$n|\$(cat \$c/cpufreq/scaling_cur_freq 2>/dev/null)|\$(cat \$c/cpufreq/cpuinfo_max_freq 2>/dev/null)|\$(cat \$c/online 2>/dev/null)\"; done; " +
        "grep '^cpu[0-9]' /proc/stat"

private class CoreRow(val name: String, val curMhz: Double, val maxMhz: Double, val online: Boolean, val load: Double)

/** Per-core clock speed and load, read from the kernel through Shizuku. */
@Composable
fun CoresScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    ToolScaffold("CORE\nMONITOR", unit, onUnit, onBack) { ShizukuGate { CoresBody() } }
}

@Composable
private fun CoresBody() {
    var rows by remember { mutableStateOf<List<CoreRow>>(emptyList()) }
    val trace = remember { mutableStateListOf<Float>() }
    LaunchedEffect(Unit) {
        var prev = HashMap<String, Pair<Long, Long>>()
        while (true) {
            val out = withContext(Dispatchers.IO) { ShizukuShell.run(CMD) }
            if (out != null) {
                val stat = HashMap<String, Pair<Long, Long>>()
                val freq = LinkedHashMap<String, Triple<Double, Double, Boolean>>()
                for (line in out.lines()) {
                    if (line.startsWith("F ")) {
                        val p = line.substring(2).split("|")
                        if (p.size >= 4) {
                            val cur = (p[1].trim().toDoubleOrNull() ?: 0.0) / 1000.0
                            val max = (p[2].trim().toDoubleOrNull() ?: 0.0) / 1000.0
                            freq[p[0].trim()] = Triple(cur, max, p[3].trim() != "0")
                        }
                    } else if (line.startsWith("cpu")) {
                        val t = line.trim().split(Regex("\\s+"))
                        val nums = t.drop(1).mapNotNull { it.toLongOrNull() }
                        if (nums.size >= 5) stat[t[0]] = Pair(nums.take(8).sum(), nums[3] + nums[4])
                    }
                }
                val list = ArrayList<CoreRow>()
                for ((name, f) in freq.toSortedMap(compareBy<String> { it.removePrefix("cpu").toIntOrNull() ?: 0 })) {
                    val now = stat[name]
                    val was = prev[name]
                    val load = if (now != null && was != null && now.first > was.first) {
                        100.0 * ((now.first - was.first) - (now.second - was.second)) / (now.first - was.first)
                    } else 0.0
                    list.add(CoreRow(name, f.first, f.second, f.third, if (f.third) load.coerceIn(0.0, 100.0) else 0.0))
                }
                prev = stat
                rows = list
                val on = list.filter { it.online }
                if (on.isNotEmpty()) trace.add(on.map { it.load }.average().toFloat())
                if (trace.size > 90) trace.removeAt(0)
            }
            delay(1000)
        }
    }
    val online = rows.filter { it.online }
    val avg = if (online.isEmpty()) 0.0 else online.map { it.load }.average()
    BigValue("TOTAL LOAD", if (rows.isEmpty()) "—" else plain(avg, 0) + " %")
    LineChart(trace, Modifier.fillMaxWidth().height(110.dp), lo = 0f, hi = 100f)
    Gap(8)
    if (rows.isNotEmpty()) {
        Readout(
            rows.map {
                ReadoutRow(
                    it.name.removePrefix("cpu"), if (it.online) "up to " + plain(it.maxMhz, 0) + " MHz" else "offline",
                    listOf(if (it.online) plain(it.load, 0) + " %" else "—", if (it.online) plain(it.curMhz, 0) else "—"),
                    faint = !it.online
                )
            },
            columns = listOf("LOAD", "MHZ")
        )
    }
    Note("READ ONCE A SECOND WHILE THIS SCREEN IS OPEN. SMALL CORES TOP OUT LOWER THAN THE BIG ONES.")
}
