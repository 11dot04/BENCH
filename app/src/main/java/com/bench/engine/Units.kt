package com.bench.engine

import java.util.Locale

enum class LenUnit(val label: String, val perMm: Double, val decimals: Int, val dialStep: Double) {
    MM("MM", 1.0, 2, 0.1),
    IN("IN", 1.0 / 25.4, 3, 0.005);

    fun show(mm: Double): String = String.format(Locale.US, "%." + decimals + "f", mm * perMm)
    fun toDisplay(mm: Double): Double = mm * perMm
    fun fromDisplay(v: Double): Double = v / perMm
}

fun degText(d: Double): String = String.format(Locale.US, "%.1f°", d)
fun plain(d: Double, decimals: Int): String = String.format(Locale.US, "%." + decimals + "f", d)
