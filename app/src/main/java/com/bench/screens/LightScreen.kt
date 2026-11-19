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

private val isos = listOf(50, 100, 200, 400, 800, 1600, 3200)
private val apertures = listOf(1.4, 1.8, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0)

/** Phone as an incident light meter: lux in, shutter speed out for the chosen ISO and aperture. */
@Composable
fun LightScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var lux by remember { mutableStateOf(0.0) }
    val trace = remember { mutableStateListOf<Float>() }
    var isoIdx by rememberSaveable { mutableStateOf(1) }
    var apIdx by rememberSaveable { mutableStateOf(4) }
    var edit by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        SensorHub.samples(ctx, Sensor.TYPE_LIGHT, SensorManager.SENSOR_DELAY_UI).collect { s ->
            lux = s.x.toDouble()
            trace.add(Photo.ev100(lux).toFloat())
            if (trace.size > 150) trace.removeAt(0)
        }
    }
    val iso = isos[isoIdx].toDouble()
    val f = apertures[apIdx]
    val t = Photo.shutter(lux, iso, f)
    val ok = lux >= 1.0
    ToolScaffold("LIGHT\nMETER", unit, onUnit, onBack) {
        BigValue("SHUTTER", if (ok) Photo.text(Photo.nearest(t)) else "—")
        LineChart(trace, Modifier.fillMaxWidth().height(120.dp))
        Gap(12)
        Segmented(listOf("ISO " + isos[isoIdx], "f/" + f), edit, Modifier.fillMaxWidth()) { edit = it }
        Gap(4)
        if (edit == 0) {
            RulerDial(isoIdx.toDouble(), 1.0, 0.0, (isos.size - 1).toDouble(), { isoIdx = it.toInt() }, Modifier.fillMaxWidth().height(56.dp))
        } else {
            RulerDial(apIdx.toDouble(), 1.0, 0.0, (apertures.size - 1).toDouble(), { apIdx = it.toInt() }, Modifier.fillMaxWidth().height(56.dp))
        }
        Gap(12)
        Readout(
            listOf(
                ReadoutRow("E", "Illuminance", listOf(plain(lux, 0)), "lx"),
                ReadoutRow("EV", "Exposure value, ISO 100", listOf(if (ok) plain(Photo.ev100(lux), 1) else "—")),
                ReadoutRow("t", "Exact shutter", listOf(if (ok) Photo.text(t) else "—")),
                ReadoutRow("≈", "Nearest standard", listOf(if (ok) Photo.text(Photo.nearest(t)) else "—"))
            )
        )
        Note("HOLD THE PHONE WHERE THE SUBJECT IS, SCREEN FACING THE LIGHT SOURCE. THE SENSOR SITS BEHIND THE GLASS, SO TREAT IT AS A GOOD GUIDE.")
    }
}
