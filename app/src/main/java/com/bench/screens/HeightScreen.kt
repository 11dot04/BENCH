package com.bench.screens

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bench.engine.*
import com.bench.ui.*
import kotlin.math.asin
import kotlin.math.sqrt
import kotlin.math.tan

/** Clinometer: aim the phone's top edge at the top (and base) of something, get its height. */
@Composable
fun HeightScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var angle by remember { mutableStateOf(0.0) }
    var top by remember { mutableStateOf<Double?>(null) }
    var base by remember { mutableStateOf<Double?>(null) }
    var distM by rememberSaveable { mutableStateOf(10.0) }
    var eyeM by rememberSaveable { mutableStateOf(1.6) }
    var edit by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        var sm = Double.NaN
        SensorHub.samples(ctx, Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_UI).collect { s ->
            val g = sqrt((s.x * s.x + s.y * s.y + s.z * s.z).toDouble())
            if (g > 1.0) {
                val a = Math.toDegrees(asin((s.y / g).toDouble().coerceIn(-1.0, 1.0)))
                sm = if (sm.isNaN()) a else sm + 0.2 * (a - sm)
                angle = sm
            }
        }
    }
    val t = top
    val b = base
    val heightM: Double? = if (t == null) null else {
        val tt = tan(Math.toRadians(t))
        if (b != null) distM * (tt - tan(Math.toRadians(b))) else distM * tt + eyeM
    }
    ToolScaffold("HEIGHT\nFINDER", unit, onUnit, onBack) {
        BigValue("HEIGHT", if (heightM == null) "—" else unit.showBig(heightM) + " " + unit.bigLabel)
        Segmented(listOf("DISTANCE", "EYE HEIGHT"), edit, Modifier.fillMaxWidth()) { edit = it }
        Gap(4)
        if (edit == 0) {
            RulerDial(distM * unit.perMetre, 0.1, 0.5 * unit.perMetre, 500.0 * unit.perMetre, { distM = it / unit.perMetre }, Modifier.fillMaxWidth().height(56.dp))
        } else {
            RulerDial(eyeM * unit.perMetre, 0.05, 0.3 * unit.perMetre, 2.5 * unit.perMetre, { eyeM = it / unit.perMetre }, Modifier.fillMaxWidth().height(56.dp))
        }
        Gap(12)
        Row(Modifier.fillMaxWidth()) {
            ActionButton("TOP", Modifier.weight(1f), primary = true) { top = angle }
            Spacer(Modifier.width(8.dp))
            ActionButton("BASE", Modifier.weight(1f)) { base = angle }
            Spacer(Modifier.width(8.dp))
            ActionButton("RESET", Modifier.weight(1f)) { top = null; base = null }
        }
        Gap(12)
        Readout(
            listOf(
                ReadoutRow("θ", "Aim now", listOf(degText(angle))),
                ReadoutRow("↗", "Top angle", listOf(if (t == null) "—" else degText(t))),
                ReadoutRow("↘", "Base angle", listOf(if (b == null) "—" else degText(b))),
                ReadoutRow("d", "Distance", listOf(unit.showBig(distM)), unit.bigLabel),
                ReadoutRow("e", "Eye height", listOf(unit.showBig(eyeM)), unit.bigLabel, faint = b != null)
            )
        )
        Note("STAND AT THE MEASURED DISTANCE. AIM THE PHONE'S TOP EDGE AT THE TOP, TAP TOP. FOR GROUND-LEVEL ACCURACY AIM AT THE BASE AND TAP BASE TOO, OTHERWISE EYE HEIGHT IS ADDED.")
    }
}
