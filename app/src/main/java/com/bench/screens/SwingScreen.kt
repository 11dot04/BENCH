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

/** Pendulum: time the swing, get the length it implies, or the local gravity for a length you measured. */
@Composable
fun SwingScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var period by remember { mutableStateOf<Double?>(null) }
    var lengthM by rememberSaveable { mutableStateOf(1.0) }
    LaunchedEffect(Unit) {
        val m = SwingMeter()
        SensorHub.samples(ctx, Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_GAME).collect { s ->
            val t = m.push(s)
            if (t != null) period = t
        }
    }
    val t = period
    val k = 4.0 * PI * PI
    ToolScaffold("SWING\nMETER", unit, onUnit, onBack) {
        BigValue("PERIOD", if (t == null) "—" else plain(t, 3) + " s")
        Gap(4)
        RulerDial(
            lengthM * unit.perMetre, 0.01, 0.05 * unit.perMetre, 10.0 * unit.perMetre,
            { lengthM = it / unit.perMetre }, Modifier.fillMaxWidth().height(56.dp)
        )
        BigValue("YOUR LENGTH", unit.showBig(lengthM) + " " + unit.bigLabel)
        Gap(6)
        Readout(
            listOf(
                ReadoutRow("T", "Swing period", listOf(if (t == null) "—" else plain(t, 3)), "s"),
                ReadoutRow("f", "Frequency", listOf(if (t == null) "—" else plain(1.0 / t, 3)), "Hz"),
                ReadoutRow("L", "Length implied", listOf(if (t == null) "—" else unit.showBig(9.80665 * t * t / k, 3)), unit.bigLabel),
                ReadoutRow("g", "Gravity from your length", listOf(if (t == null) "—" else plain(k * lengthM / (t * t), 3)), "m/s²")
            )
        )
        Note("HANG THE PHONE FROM A STRING, SWING IT WITH A SMALL ANGLE AND LET IT RUN FOR A FEW SWINGS. LENGTH IS PIVOT TO THE PHONE'S CENTRE. THE PHONE IS NOT A POINT MASS, SO EXPECT A FEW PERCENT OF ERROR.")
    }
}
