package com.bench.screens

import android.hardware.Sensor
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bench.engine.*
import com.bench.ui.*

/** Hang time to height: the phone is in free fall while you are airborne, h = g t² / 8. */
@Composable
fun AirtimeScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val attempts = remember { mutableStateListOf<Double>() }
    LaunchedEffect(Unit) {
        val d = AirDetector()
        SensorHub.samples(ctx, Sensor.TYPE_ACCELEROMETER, 5000).collect { s ->
            val t = d.push(s)
            if (t != null) {
                attempts.add(0, t)
                if (attempts.size > 8) attempts.removeAt(attempts.size - 1)
            }
        }
    }
    fun heightM(t: Double) = 9.81 * t * t / 8.0
    fun showH(m: Double) = if (unit == LenUnit.MM) plain(m * 100.0, 1) + " cm" else plain(m * 39.3701, 1) + " in"
    val lastT = attempts.firstOrNull()
    val bestT = attempts.maxOrNull()
    ToolScaffold("AIR\nTIME", unit, onUnit, onBack) {
        BigValue("LAST JUMP", if (lastT == null) "—" else showH(heightM(lastT)))
        Gap(8)
        val rows = ArrayList<ReadoutRow>()
        if (lastT != null) rows.add(ReadoutRow("t", "Airtime", listOf(plain(lastT * 1000.0, 0)), "ms"))
        if (bestT != null) rows.add(ReadoutRow("↑", "Best height", listOf(showH(heightM(bestT)))))
        attempts.forEachIndexed { i, t ->
            rows.add(ReadoutRow((i + 1).toString(), "Attempt", listOf(showH(heightM(t))), "", faint = true))
        }
        if (rows.isNotEmpty()) Readout(rows)
        Gap(12)
        ActionButton("CLEAR", Modifier.fillMaxWidth()) { attempts.clear() }
        Note("HOLD THE PHONE FIRMLY AGAINST YOUR CHEST, THEN JUMP. IT TIMES THE FREE-FALL WINDOW BETWEEN TAKE-OFF AND LANDING.")
    }
}
