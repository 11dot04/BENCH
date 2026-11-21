package com.bench.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Smooth, always-to-scale zoom. The window (how many mm the drawing area spans) stays fixed while the
 * figure changes size, so growing a radius visibly grows the part. Only when the figure outgrows or
 * rattles around in the window does it re-zoom, and then it glides there instead of jumping.
 * Returns the animated window span in mm.
 */
@Composable
fun rememberZoomMm(neededMm: Double, perMm: Double): Float {
    var window by remember { mutableStateOf(0.0) }
    var lastPer by remember { mutableStateOf(perMm) }
    LaunchedEffect(neededMm, perMm) {
        val want = NiceScale.ceil(neededMm * perMm * 1.18) / perMm
        if (window <= 0.0 || lastPer != perMm || neededMm > window * 0.94 || neededMm < window * 0.42) {
            window = want
            lastPer = perMm
        }
    }
    val target = if (window > 0.0) window else NiceScale.ceil(neededMm * perMm * 1.18) / perMm
    return animateFloatAsState(target.toFloat(), spring(dampingRatio = 1f, stiffness = 140f), label = "zoom").value
}
