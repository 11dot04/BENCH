package com.bench.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bench.engine.LenUnit
import com.bench.screens.BoltScreen
import com.bench.screens.AirtimeScreen
import com.bench.screens.FeedsScreen
import com.bench.screens.FieldScreen
import com.bench.screens.FractionScreen
import com.bench.screens.HeightScreen
import com.bench.screens.LightScreen
import com.bench.screens.PowerScreen
import com.bench.screens.PressureScreen
import com.bench.screens.ShakeScreen
import com.bench.screens.SoundScreen
import com.bench.screens.FilletScreen
import com.bench.screens.GForceScreen
import com.bench.screens.LevelScreen
import com.bench.screens.SpinScreen
import com.bench.screens.SwingScreen
import com.bench.screens.WifiScreen
import com.bench.screens.BatteryScreen
import com.bench.screens.CoresScreen
import com.bench.screens.ThermalScreen
import com.bench.screens.HomeScreen
import com.bench.screens.SlopeScreen

@Composable
fun BenchApp() {
    var screen by rememberSaveable { mutableStateOf("home") }
    var inch by rememberSaveable { mutableStateOf(false) }
    val unit = if (inch) LenUnit.IN else LenUnit.MM
    val setUnit: (LenUnit) -> Unit = { inch = (it == LenUnit.IN) }
    BackHandler(enabled = screen != "home") { screen = "home" }
    val bleed = screen == "fillet"
    Box(
        Modifier.fillMaxSize().background(BenchColors.ground)
            .then(if (bleed) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars))
    ) {
        Crossfade(targetState = screen, animationSpec = tween(220), label = "screen") { s ->
            when (s) {
                "fillet" -> FilletScreen(unit, setUnit) { screen = "home" }
                "slope" -> SlopeScreen(unit, setUnit) { screen = "home" }
                "bolt" -> BoltScreen(unit, setUnit) { screen = "home" }
                "feeds" -> FeedsScreen(unit, setUnit) { screen = "home" }
                "fraction" -> FractionScreen(unit, setUnit) { screen = "home" }
                "height" -> HeightScreen(unit, setUnit) { screen = "home" }
                "field" -> FieldScreen(unit, setUnit) { screen = "home" }
                "sound" -> SoundScreen(unit, setUnit) { screen = "home" }
                "light" -> LightScreen(unit, setUnit) { screen = "home" }
                "shake" -> ShakeScreen(unit, setUnit) { screen = "home" }
                "airtime" -> AirtimeScreen(unit, setUnit) { screen = "home" }
                "pressure" -> PressureScreen(unit, setUnit) { screen = "home" }
                "power" -> PowerScreen(unit, setUnit) { screen = "home" }
                "level" -> LevelScreen(unit, setUnit) { screen = "home" }
                "spin" -> SpinScreen(unit, setUnit) { screen = "home" }
                "swing" -> SwingScreen(unit, setUnit) { screen = "home" }
                "gforce" -> GForceScreen(unit, setUnit) { screen = "home" }
                "wifi" -> WifiScreen(unit, setUnit) { screen = "home" }
                "battery" -> BatteryScreen(unit, setUnit) { screen = "home" }
                "cores" -> CoresScreen(unit, setUnit) { screen = "home" }
                "thermal" -> ThermalScreen(unit, setUnit) { screen = "home" }
                else -> HomeScreen(unit, setUnit) { screen = it }
            }
        }
    }
}
