package com.bench.ui

import android.graphics.RuntimeShader
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ShaderBrush

private const val BRUSHED = """
uniform float2 size;
float h(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
float n(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = h(i);
    float b = h(i + float2(1.0, 0.0));
    float c = h(i + float2(0.0, 1.0));
    float d = h(i + float2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}
half4 main(float2 fc) {
    float streak = n(float2(fc.x / 240.0, fc.y * 0.55));
    float fine = n(float2(fc.x / 45.0, fc.y * 1.7));
    float grain = h(fc) * 0.02;
    float t = (fc.x / size.x) * 0.75 + (1.0 - fc.y / size.y) * 0.45;
    float sweep = smoothstep(0.0, 1.0, 1.0 - abs(t - 0.7) * 2.2);
    float v = 0.085 + streak * 0.05 + fine * 0.035 + sweep * 0.085 + grain;
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
