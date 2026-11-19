package com.bench.engine

import java.util.Locale

enum class LenUnit(val label: String, val perMm: Double, val decimals: Int, val dialStep: Double) {
    MM("MM", 1.0, 2, 0.5),
    IN("IN", 1.0 / 25.4, 3, 0.02);

    fun show(mm: Double): String {
        val s = String.format(Locale.US, "%." + decimals + "f", mm * perMm)
        return if (s.startsWith("-") && s.all { it == '-' || it == '0' || it == '.' }) s.substring(1) else s
    }
    /** Long distances: metres in MM mode, feet in IN mode. */
    val bigLabel: String get() = if (this == MM) "m" else "ft"
    val perMetre: Double get() = if (this == MM) 1.0 else 3.280839895
    fun showBig(metres: Double, decimals: Int = 2): String = plain(metres * perMetre, decimals)
    fun toDisplay(mm: Double): Double = mm * perMm
    fun fromDisplay(v: Double): Double = v / perMm
}

fun degText(d: Double): String = String.format(Locale.US, "%.1f°", d)
fun plain(d: Double, decimals: Int): String = String.format(Locale.US, "%." + decimals + "f", d)
