package com.bench.ui

import android.graphics.RuntimeShader
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ShaderBrush

internal const val BRUSHED_SRC = """
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
    float2 p = float2(fc.x * 0.970 + fc.y * 0.242, fc.y * 0.970 - fc.x * 0.242);
    float streak = n(float2(p.x / 260.0, p.y * 0.50));
    float fine = n(float2(p.x / 60.0, p.y * 1.60));
    float grain = h(fc) * 0.03;
    float t = (fc.x / size.x) * 0.8 + (1.0 - fc.y / size.y) * 0.5;
    float sweep = smoothstep(0.0, 1.0, 1.0 - abs(t - 0.75) * 1.8);
    float v = 0.25 + streak * 0.12 + fine * 0.07 + sweep * 0.24 + grain;
    v = v * (0.55 + 0.45 * smoothstep(0.0, 0.6, (1.0 - fc.y / size.y) + 0.25));
    return half4(v * 0.97, v * 0.99, v * 1.03, 1.0);
}
"""

/** A shader brush sized for a canvas of [w] x [h] pixels. */
fun brushedBrush(shader: RuntimeShader, w: Float, h: Float): ShaderBrush {
    shader.setFloatUniform("size", w, h)
    return ShaderBrush(shader)
}

/** Brushed-metal backdrop drawn by an AGSL shader (Android 13+). */
fun Modifier.brushedMetal(): Modifier = this.drawWithCache {
    val shader = RuntimeShader(BRUSHED_SRC)
    val brush = ShaderBrush(shader)
    onDrawBehind {
        shader.setFloatUniform("size", size.width, size.height)
        drawRect(brush)
    }
}
