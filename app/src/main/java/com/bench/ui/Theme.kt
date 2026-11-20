package com.bench.ui

import android.content.res.AssetManager
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.bench.R

object BenchColors {
    val ground = Color(0xFF15171B)
    val ground2 = Color(0xFF181B1F)
    val surface = Color(0xFF1D2025)
    val surfaceHigh = Color(0xFF252930)
    val line = Color(0xFF2C3037)
    val line2 = Color(0xFF3A3F48)
    val ink = Color(0xFFE8E9EA)
    val dim = Color(0xFF8B919B)
    val accent = Color(0xFFFF6A2B)
}

/** Geist Mono lives in assets (variable font); call [init] once from the activity before any UI. */
object BenchFonts {
    var assets: AssetManager? = null
    fun init(am: AssetManager) {
        assets = am
    }
}

/** Mono for titles, labels, dial text and big numerals; Montserrat for tables and navigation. One size per kind of data. */
object BenchType {
    private fun geist(am: AssetManager, w: Int, fw: FontWeight) =
        Font("font/geist_mono.ttf", am, fw, FontStyle.Normal, FontVariation.Settings(FontVariation.weight(w)))

    val mono: FontFamily = BenchFonts.assets?.let { am ->
        FontFamily(geist(am, 400, FontWeight.Normal), geist(am, 600, FontWeight.SemiBold))
    } ?: FontFamily.Monospace
    val sans: FontFamily = FontFamily(
        Font(R.font.montserrat_medium, FontWeight.Medium),
        Font(R.font.montserrat_bold, FontWeight.Bold)
    )
    val family: FontFamily get() = mono

    val label = TextStyle(fontFamily = mono, fontSize = 10.sp, letterSpacing = 0.8.sp, color = BenchColors.dim)
    val value = TextStyle(fontFamily = mono, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BenchColors.ink)
    val dial = TextStyle(fontFamily = mono, fontSize = 13.sp, color = BenchColors.ink)
    val readout = TextStyle(fontFamily = sans, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BenchColors.ink)
    val title = TextStyle(fontFamily = mono, fontSize = 28.sp, lineHeight = 31.sp, fontWeight = FontWeight.Normal, color = BenchColors.ink)
    val numeral = TextStyle(fontFamily = mono, fontSize = 44.sp, fontWeight = FontWeight.SemiBold, color = BenchColors.accent)
}

@Composable
fun BText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    align: TextAlign? = null
) {
    var s = style
    if (color != Color.Unspecified) s = s.copy(color = color)
    if (align != null) s = s.copy(textAlign = align)
    BasicText(text = text, modifier = modifier, style = s)
}
