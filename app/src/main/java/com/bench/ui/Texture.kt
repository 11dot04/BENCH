package com.bench.ui

import android.graphics.RuntimeShader
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ShaderBrush

private const val BRUSHED = """
uniform float2 size;
float h(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
half4 main(float2 fc) {
    float row = floor(fc.y * 0.9);
    float seg = floor(fc.x / 90.0);
    float streak = h(float2(row, seg)) * 0.6 + h(float2(row, 3.0)) * 0.4;
    float grain = h(fc * 0.5) * 0.03;
    float fade = 1.0 - 0.6 * (fc.x / size.x);
    float v = 0.075 + streak * 0.07 * fade + grain;
    return half4(v, v + 0.004, v + 0.012, 1.0);
}
"""

/** Brushed-metal backdrop drawn by an AGSL shader (Android 13+). */
fun Modifier.brushedMetal(): Modifier = this.drawWithCache {
    val shader = RuntimeShader(BRUSHED)
    val brush = ShaderBrush(shader)
    onDrawBehind {
        shader.setFloatUniform("size", size.width, size.height)
        drawRect(brush)
    }
}
