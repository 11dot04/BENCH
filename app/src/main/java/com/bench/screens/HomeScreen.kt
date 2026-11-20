package com.bench.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.bench.engine.LenUnit
import com.bench.engine.SensorHub
import com.bench.ui.BenchHeader
import com.bench.ui.Glyph
import com.bench.ui.SectionLabel
import com.bench.ui.TileSection
import com.bench.ui.TileSpec
import com.bench.ui.TitleBlock

private const val ACC = 1
private const val MAG = 2
private const val GYR = 4
private const val LIGHT = 5
private const val BARO = 6

private val calculate = listOf(
    TileSpec("fillet", "FILLET", 2, 2, Glyph.FILLET, true),
    TileSpec("slope", "SLOPE", 1, 1, Glyph.SLOPE, true, sensor = ACC),
    TileSpec("bolt", "BOLT", 1, 1, Glyph.BOLT, true),
    TileSpec("feeds", "FEEDS · SPEEDS", 2, 1, Glyph.FEEDS, true),
    TileSpec("fraction", "FRACTION", 2, 1, Glyph.FRACTION, true),
    TileSpec("height", "HEIGHT", 2, 1, Glyph.HEIGHT, true, sensor = ACC)
)
private val convert = listOf(
    TileSpec("length", "LENGTH", 2, 1, Glyph.LENGTH, false),
    TileSpec("mass", "MASS", 1, 1, Glyph.MASS, false),
    TileSpec("temp", "TEMP", 1, 1, Glyph.TEMP, false)
)
private val sense = listOf(
    TileSpec("level", "LEVEL", 2, 2, Glyph.LEVEL, true, sensor = ACC),
    TileSpec("field", "FIELD", 1, 1, Glyph.FIELD, true, sensor = MAG),
    TileSpec("sound", "SOUND", 1, 1, Glyph.SOUND, true),
    TileSpec("light", "LIGHT", 2, 1, Glyph.LIGHT, true, sensor = LIGHT),
    TileSpec("shake", "SHAKE · RPM", 2, 1, Glyph.SHAKE, true, sensor = ACC),
    TileSpec("airtime", "AIR", 1, 1, Glyph.AIRTIME, true, sensor = ACC),
    TileSpec("pressure", "PRESS", 1, 1, Glyph.PRESSURE, true, sensor = BARO),
    TileSpec("power", "POWER", 1, 1, Glyph.POWER, true),
    TileSpec("wifi", "WI-FI", 1, 1, Glyph.WIFI, true),
    TileSpec("gforce", "G-FORCE", 1, 1, Glyph.GFORCE, true, sensor = ACC),
    TileSpec("spin", "SPIN", 1, 1, Glyph.SPIN, true, sensor = GYR),
    TileSpec("swing", "SWING", 1, 1, Glyph.SWING, true, sensor = GYR)
)
private val shizuku = listOf(
    TileSpec("battery", "BATTERY", 1, 1, Glyph.BATTERY, false, true),
    TileSpec("cores", "CORES", 1, 1, Glyph.CORES, false, true),
    TileSpec("thermal", "THERMAL", 1, 1, Glyph.TEMP, false, true)
)

/** Greys out tools whose sensor this phone lacks. Only asks the system what exists; reads nothing. */
private fun probe(ctx: Context, list: List<TileSpec>): List<TileSpec> = list.map { t ->
    val type = t.sensor
    if (type != null && t.ready && !SensorHub.available(ctx, type)) t.copy(ready = false, missing = true) else t
}

@Composable
fun HomeScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val calcP = remember { probe(ctx, calculate) }
    val convP = remember { probe(ctx, convert) }
    val senseP = remember { probe(ctx, sense) }
    val shzP = remember { probe(ctx, shizuku) }
    val all = calcP + convP + senseP + shzP
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)
    ) {
        BenchHeader("Tools", unit, onUnit)
        Spacer(Modifier.height(20.dp))
        TitleBlock("BENCH", "${all.size} TOOLS · ${all.count { it.ready }} READY")
        SectionLabel("CALCULATE")
        TileSection(calcP, onOpen)
        SectionLabel("CONVERT")
        TileSection(convP, onOpen)
        SectionLabel("SENSE")
        TileSection(senseP, onOpen)
        SectionLabel("SHIZUKU")
        TileSection(shzP, onOpen)
    }
}
