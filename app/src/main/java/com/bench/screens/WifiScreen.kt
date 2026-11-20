package com.bench.screens

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.bench.engine.*
import com.bench.ui.*
import kotlinx.coroutines.delay

private class WifiNow(val rssi: Int, val speed: Int, val freq: Int)

@Suppress("DEPRECATION")
private fun readWifi(ctx: Context): WifiNow? {
    val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
    if (!wm.isWifiEnabled) return null
    val i = wm.connectionInfo ?: return null
    if (i.rssi <= -127) return null
    return WifiNow(i.rssi, i.linkSpeed, i.frequency)
}

/** Wi-Fi signal hunter: live strength with a Geiger-style tick that speeds up as the signal improves. */
@Composable
fun WifiScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var tick by rememberSaveable { mutableStateOf(0) }
    var now by remember { mutableStateOf<WifiNow?>(null) }
    var peak by remember { mutableStateOf(-127) }
    val trace = remember { mutableStateListOf<Float>() }
    LaunchedEffect(tick) {
        while (true) {
            val r = readWifi(ctx)
            now = r
            if (r != null) {
                if (r.rssi > peak) peak = r.rssi
                trace.add(r.rssi.toFloat())
                if (trace.size > 120) trace.removeAt(0)
                if (tick == 0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay((1000 - (r.rssi + 100) * 14).toLong().coerceIn(120L, 1000L))
            } else {
                delay(1000L)
            }
        }
    }
    val r = now
    val quality = if (r == null) 0 else ((r.rssi + 100) * 2).coerceIn(0, 100)
    val band = if (r == null) "—" else if (r.freq < 3000) "2.4 GHz" else if (r.freq < 5900) "5 GHz" else "6 GHz"
    val channel = if (r == null) "—" else (if (r.freq < 3000) (r.freq - 2407) / 5 else if (r.freq < 5900) (r.freq - 5000) / 5 else (r.freq - 5950) / 5).toString()
    ToolScaffold("WI-FI\nHUNTER", unit, onUnit, onBack) {
        Segmented(listOf("TICK ON", "TICK OFF"), tick, Modifier.fillMaxWidth()) { tick = it }
        Gap(6)
        BigValue("SIGNAL", if (r == null) "NOT CONNECTED" else r.rssi.toString() + " dBm")
        LineChart(trace, Modifier.fillMaxWidth().height(130.dp), lo = -100f, hi = -30f)
        Gap(8)
        if (r != null) {
            Readout(
                listOf(
                    ReadoutRow("%", "Quality", listOf(quality.toString()), "%"),
                    ReadoutRow("↑", "Best so far", listOf(peak.toString()), "dBm"),
                    ReadoutRow("v", "Link speed", listOf(r.speed.toString()), "Mb/s"),
                    ReadoutRow("f", "Frequency", listOf(r.freq.toString()), "MHz"),
                    ReadoutRow("B", "Band", listOf(band)),
                    ReadoutRow("#", "Channel", listOf(channel))
                )
            )
        }
        Note("WALK AROUND WITH THE TICK ON: IT BEATS FASTER AS THE SIGNAL GETS STRONGER. CLOSER TO ZERO DBM IS BETTER; AROUND -50 IS EXCELLENT, BELOW -80 IS POOR.")
    }
}
