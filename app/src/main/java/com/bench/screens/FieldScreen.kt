package com.bench.screens

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bench.engine.*
import com.bench.ui.*
import kotlin.math.abs
import kotlin.math.sqrt

/** Magnetometer with a zeroable baseline: spots magnets and ferrous metal as a change from "normal". */
@Composable
fun FieldScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var present by remember { mutableStateOf(true) }
    var mag by remember { mutableStateOf(0.0) }
    var vx by remember { mutableStateOf(0f) }
    var vy by remember { mutableStateOf(0f) }
    var vz by remember { mutableStateOf(0f) }
    var base by remember { mutableStateOf<Double?>(null) }
    var peak by remember { mutableStateOf(0.0) }
    val trace = remember { mutableStateListOf<Float>() }
    LaunchedEffect(Unit) {
        present = SensorHub.available(ctx, Sensor.TYPE_MAGNETIC_FIELD)
        var sm = Double.NaN
        SensorHub.samples(ctx, Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_UI).collect { s ->
            val m = sqrt((s.x * s.x + s.y * s.y + s.z * s.z).toDouble())
            sm = if (sm.isNaN()) m else sm + 0.25 * (m - sm)
            mag = sm
            vx = s.x; vy = s.y; vz = s.z
            val b = base
            if (b != null && abs(sm - b) > peak) peak = abs(sm - b)
            trace.add(sm.toFloat())
            if (trace.size > 160) trace.removeAt(0)
        }
    }
    ToolScaffold("FIELD\nFINDER", unit, onUnit, onBack) {
        if (!present) Note("THIS PHONE HAS NO MAGNETOMETER")
        BigValue("FIELD STRENGTH", plain(mag, 1) + " µT")
        LineChart(trace, Modifier.fillMaxWidth().height(150.dp))
        Gap(12)
        Row(Modifier.fillMaxWidth()) {
            ActionButton("ZERO HERE", Modifier.weight(1f), primary = true) { base = mag; peak = 0.0 }
            Spacer(Modifier.width(8.dp))
            ActionButton("RESET PEAK", Modifier.weight(1f)) { peak = 0.0 }
        }
        Gap(12)
        val b = base
        val delta = if (b == null) null else mag - b
        val verdict = if (delta == null) "—" else if (abs(delta) < 3.0) "LOW" else if (abs(delta) < 10.0) "MEDIUM" else "HIGH"
        Readout(
            listOf(
                ReadoutRow("|B|", "Field strength", listOf(plain(mag, 1)), "µT"),
                ReadoutRow("Δ", "From baseline", listOf(if (delta == null) "—" else plain(delta, 1)), "µT"),
                ReadoutRow("↑", "Peak change", listOf(if (b == null) "—" else plain(peak, 1)), "µT"),
                ReadoutRow("!", "Metal or magnet near", listOf(verdict)),
                ReadoutRow("X", "Axis X", listOf(plain(vx.toDouble(), 1)), "µT", faint = true),
                ReadoutRow("Y", "Axis Y", listOf(plain(vy.toDouble(), 1)), "µT", faint = true),
                ReadoutRow("Z", "Axis Z", listOf(plain(vz.toDouble(), 1)), "µT", faint = true)
            )
        )
        Note("PRESS ZERO HERE AWAY FROM METAL, THEN SWEEP. EARTH'S OWN FIELD IS ROUGHLY 25 – 65 µT.")
    }
}
