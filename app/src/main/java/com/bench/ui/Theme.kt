package com.bench.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

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

/** One size per kind of data, everywhere. Swap [family] for the real typeface later. */
object BenchType {
    val family: FontFamily = FontFamily.Monospace
    val label = TextStyle(fontFamily = family, fontSize = 10.sp, letterSpacing = 0.8.sp, color = BenchColors.dim)
    val value = TextStyle(fontFamily = family, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BenchColors.ink)
    val readout = TextStyle(fontFamily = family, fontSize = 13.sp, color = BenchColors.ink)
    val title = TextStyle(fontFamily = family, fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, color = BenchColors.ink)
    val numeral = TextStyle(fontFamily = family, fontSize = 44.sp, fontWeight = FontWeight.SemiBold, color = BenchColors.accent)
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
