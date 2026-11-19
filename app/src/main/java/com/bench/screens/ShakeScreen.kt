package com.bench.screens

import android.hardware.Sensor
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bench.engine.*
import com.bench.ui.*

/** Vibration spectrum from the accelerometer: dominant frequency, shaft speed in rpm. */
@Composable
fun ShakeScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var live by rememberSaveable { mutableStateOf(true) }
    var res by remember { mutableStateOf<VibeResult?>(null) }
    LaunchedEffect(live) {
        if (live) {
            val an = VibeAnalyzer()
            SensorHub.samples(ctx, Sensor.TYPE_ACCELEROMETER, 2500).collect { s ->
                val r = an.push(s)
                if (r != null) res = r
            }
        }
    }
    ToolScaffold("SHAKE\nANALYZER", unit, onUnit, onBack) {
        Segmented(listOf("LIVE", "HOLD"), if (live) 0 else 1, Modifier.fillMaxWidth()) { live = it == 0 }
        Gap(8)
        val r = res
        BigValue("SHAFT SPEED", if (r == null) "—" else plain(r.rpm, 0) + " rpm")
        LineChart(r?.spectrum ?: emptyList(), Modifier.fillMaxWidth().height(160.dp), lo = 0f)
        Note(if (r == null) "LAY THE PHONE FLAT ON THE MACHINE AND WAIT A MOMENT" else "SPECTRUM 0 – " + plain(r.fs / 2.0, 0) + " HZ")
        if (r != null) {
            Readout(
                listOf(
                    ReadoutRow("f", "Dominant", listOf(plain(r.hz, 2)), "Hz"),
                    ReadoutRow("n", "Speed", listOf(plain(r.rpm, 0)), "rpm"),
                    ReadoutRow("f₂", "Second peak", listOf(plain(r.secondHz, 2)), "Hz"),
                    ReadoutRow("a", "Vibration RMS", listOf(plain(r.rms, 3)), "m/s²"),
                    ReadoutRow("fs", "Sample rate", listOf(plain(r.fs, 0)), "Hz")
                )
            )
        }
        Note("ONE STRONG PEAK IS USUALLY ONE REVOLUTION PER SECOND. LAY THE PHONE ON A MOTOR, FAN OR COMPRESSOR.")
    }
}
