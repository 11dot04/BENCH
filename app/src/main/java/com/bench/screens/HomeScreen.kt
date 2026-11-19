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
import com.bench.engine.LenUnit
import com.bench.ui.BenchHeader
import com.bench.ui.Glyph
import com.bench.ui.SectionLabel
import com.bench.ui.TileSection
import com.bench.ui.TileSpec
import com.bench.ui.TitleBlock

private val calculate = listOf(
    TileSpec("fillet", "FILLET", 2, 2, Glyph.FILLET, true),
    TileSpec("slope", "SLOPE", 1, 1, Glyph.SLOPE, true),
    TileSpec("bolt", "BOLT", 1, 1, Glyph.BOLT, true),
    TileSpec("feeds", "FEEDS · SPEEDS", 2, 1, Glyph.FEEDS, true),
    TileSpec("fraction", "FRACTION", 2, 1, Glyph.FRACTION, true),
    TileSpec("height", "HEIGHT", 2, 1, Glyph.HEIGHT, true)
)
private val convert = listOf(
    TileSpec("length", "LENGTH", 2, 1, Glyph.LENGTH, false),
    TileSpec("mass", "MASS", 1, 1, Glyph.MASS, false),
    TileSpec("temp", "TEMP", 1, 1, Glyph.TEMP, false)
)
private val sense = listOf(
    TileSpec("level", "LEVEL", 2, 2, Glyph.LEVEL, false),
    TileSpec("field", "FIELD", 1, 1, Glyph.FIELD, true),
    TileSpec("sound", "SOUND", 1, 1, Glyph.SOUND, true),
    TileSpec("light", "LIGHT", 2, 1, Glyph.LIGHT, true),
    TileSpec("shake", "SHAKE · RPM", 2, 1, Glyph.SHAKE, true),
    TileSpec("airtime", "AIR", 1, 1, Glyph.AIRTIME, true),
    TileSpec("pressure", "PRESS", 1, 1, Glyph.PRESSURE, true),
    TileSpec("power", "POWER", 1, 1, Glyph.POWER, true)
)
private val shizuku = listOf(
    TileSpec("battery", "BATTERY", 1, 1, Glyph.BATTERY, false, true),
    TileSpec("wifi", "WI-FI", 1, 1, Glyph.WIFI, false, true),
    TileSpec("cores", "CORES", 1, 1, Glyph.CORES, false, true),
    TileSpec("thermal", "THERMAL", 1, 1, Glyph.TEMP, false, true)
)

@Composable
fun HomeScreen(unit: LenUnit, onUnit: (LenUnit) -> Unit, onOpen: (String) -> Unit) {
    val all = calculate + convert + sense + shizuku
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)
    ) {
        BenchHeader("Tools", unit, onUnit)
        Spacer(Modifier.height(20.dp))
        TitleBlock("BENCH", "${all.size} TOOLS · ${all.count { it.ready }} READY")
        SectionLabel("CALCULATE")
        TileSection(calculate, onOpen)
        SectionLabel("CONVERT")
        TileSection(convert, onOpen)
        SectionLabel("SENSE")
        TileSection(sense, onOpen)
        SectionLabel("SHIZUKU")
        TileSection(shizuku, onOpen)
    }
}
