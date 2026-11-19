package com.bench.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bench.engine.*
import com.bench.ui.*
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/** Microphone: level, dominant pitch as a note, shaft-speed equivalent and mains-hum detection. */
@Composable
fun SoundScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var db by remember { mutableStateOf(-90.0) }
    var hz by remember { mutableStateOf(0.0) }
    var spectrum by remember { mutableStateOf<List<Float>>(emptyList()) }

    LaunchedEffect(granted) {
        if (granted) {
            val n = 16384
            val ring = DoubleArray(n)
            var pos = 0
            var filled = 0
            AudioTap.frames().collect { buf ->
                var sum = 0.0
                for (v in buf) {
                    val d = v / 32768.0
                    ring[pos] = d
                    pos = (pos + 1) % n
                    sum += d * d
                }
                filled = minOf(n, filled + buf.size)
                db = 20.0 * log10(max(sqrt(sum / buf.size), 1e-6))
                if (filled >= n) {
                    val ordered = DoubleArray(n) { ring[(pos + it) % n] }
                    val mag = Fft.magnitude(ordered)
                    val bin = AudioTap.RATE.toDouble() / n
                    hz = Fft.peak(mag, bin, 40.0, 2000.0).first
                    val k0 = (20.0 / bin).toInt()
                    val k1 = (1000.0 / bin).toInt()
                    spectrum = (k0..k1).map { mag[it].toFloat() }
                }
            }
        }
    }
    val loud = db > -55.0
    val note = Pitch.note(hz)
    var hum = "—"
    for (base in listOf(50.0, 60.0)) for (h in 1..3) if (abs(hz - base * h) < 1.5) hum = plain(base, 0) + " Hz mains"

    ToolScaffold("SOUND\nLAB", unit, onUnit, onBack) {
        if (!granted) {
            Note("THE MICROPHONE IS ONLY USED WHILE THIS TOOL IS OPEN, AND NOTHING IS RECORDED OR SENT ANYWHERE.")
            ActionButton("ALLOW MICROPHONE", Modifier.fillMaxWidth(), primary = true) { launcher.launch(Manifest.permission.RECORD_AUDIO) }
        } else {
            BigValue("LEVEL", plain(db, 1) + " dBFS")
            LineChart(spectrum, Modifier.fillMaxWidth().height(140.dp), lo = 0f)
            Note("SPECTRUM 20 – 1000 HZ")
            Readout(
                listOf(
                    ReadoutRow("f", "Dominant pitch", listOf(if (loud) plain(hz, 1) else "—"), "Hz"),
                    ReadoutRow("♪", "Note", listOf(if (loud) note.first + (if (note.second >= 0) " +" else " ") + note.second + "¢" else "—")),
                    ReadoutRow("n", "As shaft speed", listOf(if (loud) plain(hz * 60.0, 0) else "—"), "rpm"),
                    ReadoutRow("~", "Mains hum", listOf(if (loud) hum else "—"))
                )
            )
            Note("DBFS IS RELATIVE TO THE MIC'S FULL SCALE, NOT CALIBRATED SOUND PRESSURE. HOLD NEAR A MOTOR OR FAN FOR ITS SPEED.")
        }
    }
}
