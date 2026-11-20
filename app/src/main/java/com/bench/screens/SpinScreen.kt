package com.bench.screens

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bench.engine.*
import com.bench.ui.*
import kotlin.math.PI
import kotlin.math.sqrt

/** Gyroscope rpm meter: lay the phone on a turntable, platter or fan and read its true speed. */
@Composable
fun SpinScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var target by rememberSaveable { mutableStateOf(0) }
    var rpm by remember { mutableStateOf(0.0) }
    val trace = remember { mutableStateListOf<Float>() }
    LaunchedEffect(Unit) {
        var sm = Double.NaN
        SensorHub.samples(ctx, Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_GAME).collect { s ->
            val w = sqrt((s.x * s.x + s.y * s.y + s.z * s.z).toDouble())
            val r = w * 60.0 / (2.0 * PI)
            sm = if (sm.isNaN()) r else sm + 0.1 * (r - sm)
            rpm = sm
            trace.add(sm.toFloat())
            if (trace.size > 300) trace.removeAt(0)
        }
    }
    val goals = listOf(100.0 / 3.0, 45.0, 78.0)
    val goal: Double? = if (target in 0..2) goals[target] else null
    val spinning = rpm > 1.0 && trace.size > 20
    val mean = if (trace.isEmpty()) 0.0 else trace.average()
    val lo = if (trace.isEmpty()) 0.0 else trace.min().toDouble()
    val hi = if (trace.isEmpty()) 0.0 else trace.max().toDouble()
    val flutter = if (mean > 1.0) (hi - lo) / (2.0 * mean) * 100.0 else 0.0
    ToolScaffold("SPIN\nMETER", unit, onUnit, onBack) {
        Segmented(listOf("33⅓", "45", "78", "FREE"), target, Modifier.fillMaxWidth()) { target = it }
        Gap(6)
        BigValue("SPEED", if (spinning) plain(rpm, 2) + " rpm" else "—")
        LineChart(trace, Modifier.fillMaxWidth().height(130.dp), lo = 0f)
        Gap(8)
        Readout(
            listOf(
                ReadoutRow("n", "Speed now", listOf(if (spinning) plain(rpm, 2) else "—"), "rpm"),
                ReadoutRow("→", "Target", listOf(if (goal == null) "—" else plain(goal, 2)), "rpm"),
                ReadoutRow("Δ", "Off target", listOf(if (goal == null || !spinning) "—" else plain((mean - goal) / goal * 100.0, 2)), "%"),
                ReadoutRow("↓", "Slowest, 6 s", listOf(if (spinning) plain(lo, 2) else "—"), "rpm"),
                ReadoutRow("↑", "Fastest, 6 s", listOf(if (spinning) plain(hi, 2) else "—"), "rpm"),
                ReadoutRow("~", "Speed wobble", listOf(if (spinning) plain(flutter, 2) else "—"), "%")
            )
        )
        Note("LAY THE PHONE FLAT AND CENTRED ON THE PLATTER. SPEED WOBBLE IS PEAK-TO-PEAK OVER THE LAST SIX SECONDS, A ROUGH WOW-AND-FLUTTER GUIDE.")
    }
}
