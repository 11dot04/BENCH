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
import kotlin.math.pow

/** Barometer: relative altitude and floor count from a zeroed reference, plus a pressure trace. */
@Composable
fun PressureScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var present by remember { mutableStateOf(true) }
    var hpa by remember { mutableStateOf(0.0) }
    var ref by remember { mutableStateOf<Double?>(null) }
    val trace = remember { mutableStateListOf<Float>() }
    LaunchedEffect(Unit) {
        present = SensorHub.available(ctx, Sensor.TYPE_PRESSURE)
        var sm = Double.NaN
        SensorHub.samples(ctx, Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_UI).collect { s ->
            sm = if (sm.isNaN()) s.x.toDouble() else sm + 0.1 * (s.x - sm)
            hpa = sm
            trace.add(sm.toFloat())
            if (trace.size > 200) trace.removeAt(0)
        }
    }
    val r = ref
    val relM: Double? = if (r == null || hpa <= 0.0) null else 44330.0 * (1.0 - (hpa / r).pow(0.1903))
    val absM = if (hpa <= 0.0) 0.0 else 44330.0 * (1.0 - (hpa / 1013.25).pow(0.1903))
    ToolScaffold("PRESSURE\nALTITUDE", unit, onUnit, onBack) {
        if (!present) Note("THIS PHONE HAS NO BAROMETER")
        BigValue("RELATIVE", if (relM == null) "ZERO IT" else unit.showBig(relM, 1) + " " + unit.bigLabel)
        LineChart(trace, Modifier.fillMaxWidth().height(130.dp))
        Gap(12)
        Row(Modifier.fillMaxWidth()) {
            ActionButton("ZERO HERE", Modifier.weight(1f), primary = true) { ref = hpa }
            Spacer(Modifier.width(8.dp))
            ActionButton("CLEAR", Modifier.weight(1f)) { ref = null }
        }
        Gap(12)
        Readout(
            listOf(
                ReadoutRow("P", "Pressure", listOf(plain(hpa, 2)), "hPa"),
                ReadoutRow("P₀", "Reference", listOf(if (r == null) "—" else plain(r, 2)), "hPa"),
                ReadoutRow("Δh", "Relative altitude", listOf(if (relM == null) "—" else unit.showBig(relM, 1)), unit.bigLabel),
                ReadoutRow("fl", "Floors (3 m each)", listOf(if (relM == null) "—" else plain(relM / 3.0, 1))),
                ReadoutRow("h", "Altitude, standard sea level", listOf(unit.showBig(absM, 0)), unit.bigLabel, faint = true)
            )
        )
        Note("PRESS ZERO HERE, THEN WALK UP OR DOWN. WEATHER MOVES PRESSURE TOO, SO RE-ZERO FOR LONG MEASUREMENTS.")
    }
}
