#version 150

in vec4 vertexColor;
in vec3 localPos;

uniform float Time;
uniform vec2 Resolution;
uniform float OverlayAlpha;
uniform float RainVisibility;

out vec4 OutColor;

// ── noise primitives ───────────────────────────────────────────────────────
float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 74.51);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0); // quintic
    return mix(
        mix(hash(i),             hash(i + vec2(1,0)), u.x),
        mix(hash(i+vec2(0,1)),   hash(i + vec2(1,1)), u.x),
        u.y
    );
}

// 4-octave fbm — sweet spot between Ethereal and cost
float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    mat2 rot = mat2(0.8660, 0.5, -0.5, 0.8660); // 30° rotation
    for (int i = 0; i < 4; i++) {
        v += a * vnoise(p);
        p  = rot * p * 2.03 + vec2(31.41, 17.26);
        a *= 0.48;
    }
    return v;
}

// single-pass domain warp: cheaper than double-warp, still swirly
float fbmWarp(vec2 p) {
    vec2 q = vec2(
        vnoise(p + vec2(0.0, 0.0)),
        vnoise(p + vec2(5.2, 1.3))
    );
    return fbm(p + 2.2 * q);
}

// ── colour palette ─────────────────────────────────────────────────────────
vec3 auroraColor(float g) {
    g = fract(g);
    vec3 emerald  = vec3(0.00, 0.95, 0.45);
    vec3 cyanCore = vec3(0.30, 1.40, 0.85);
    vec3 violet   = vec3(0.55, 0.05, 1.10);
    vec3 magenta  = vec3(1.20, 0.10, 0.60);
    vec3 gold     = vec3(1.35, 0.80, 0.10);

    if (g < 0.35) return mix(emerald,  cyanCore, g / 0.35);
    if (g < 0.62) return mix(cyanCore, violet,  (g - 0.35) / 0.27);
    if (g < 0.82) return mix(violet,   magenta, (g - 0.62) / 0.20);
                  return mix(magenta,  gold,    (g - 0.82) / 0.18);
}

void main() {
    float lateral = vertexColor.r;
    float height  = vertexColor.g;
    float band    = vertexColor.b;

    // sky mask
    float hFade   = smoothstep(0.0, 0.13, lateral) * smoothstep(1.0, 0.87, lateral);
    float topFade = smoothstep(1.0, 0.68, height);
    float botFade = smoothstep(0.0, 0.10, height);
    float skyMask = hFade * topFade * botFade;

    float t = Time * 0.55;

    vec3  color = vec3(0.0);
    float alpha = 0.0;

    // ── 6 curtain layers ──────────────────────────────────────────────────
    // Cost breakdown per layer:
    //   fbmWarp = 2x vnoise (warp) + 4-oct fbm = 6 vnoise calls
    //   streaks = 1x vnoise
    //   Total: 7 vnoise/layer × 6 layers = 42 vnoise calls  (was ~192)
    for (int i = 0; i < 6; i++) {
        float layer = float(i);
        float depth = layer / 5.0;

        float drift = t * (0.42 + depth * 0.52) + band * 6.2832;

        vec2 uv = vec2(lateral * (2.5 + depth * 2.2) + drift,
                       depth * 8.0 + t * 0.28 + band * 4.8);
        float wave = fbmWarp(uv * 0.9);

        float center  = mix(0.11, 0.89, depth) + (wave - 0.5) * 0.28;
        float width   = mix(0.028, 0.11, depth + 0.1);
        float curtain = exp(-pow((height - center) / width, 2.0));

        // streaks: single vnoise call instead of full fbm
        float streakNoise = vnoise(vec2(lateral * 10.0 + drift * 1.9 + wave * 1.2,
                                        height * 5.5 + layer * 2.3 + band));
        float streaks = smoothstep(0.18, 0.88, streakNoise);

        // ribs
        float ribFreq = 55.0 + depth * 25.0;
        float ribs = 0.55 + 0.45 * pow(
            0.5 + 0.5 * sin((lateral + wave * 0.6) * ribFreq + t * 11.0 + layer),
            4.0
        );
        ribs = mix(1.0, ribs, topFade * 0.85);

        float lowerGlow = smoothstep(0.03, 0.28, height);
        float pulse     = 0.65 + 0.35 * sin(t * 1.4 + band * 8.5 + lateral * 9.0 + depth * 5.2);
        float weight    = mix(1.0, 0.72, depth);
        float bright    = curtain * streaks * ribs * lowerGlow * pulse * weight;

        float colGrad = depth + 0.18 * sin(t * 0.22 + band * 3.1 + layer * 0.7);
        color += auroraColor(colGrad) * bright * 0.72;
        alpha += bright * 0.105;
    }

    // ── horizon glow — 1x fbmWarp ─────────────────────────────────────────
    float glow = fbmWarp(vec2(lateral * 1.3 + t * 0.38 + band * 2.9,
                              height  * 1.8 - t * 0.16));
    float glowMask = smoothstep(0.04, 0.40, height) * topFade;
    color += auroraColor(0.20 + glow * 0.60) * glow * glowMask * 0.20 * skyMask;

    // ── top shimmer — cheap: just vnoise, no fbm ──────────────────────────
    float shimmer = vnoise(vec2(lateral * 18.0 + t * 3.5, height * 7.0 + t * 1.8 + band * 6.0));
    float shimmerMask = smoothstep(0.45, 0.90, height) * smoothstep(1.0, 0.60, height);
    color += auroraColor(0.65 + shimmer * 0.3) * shimmer * shimmerMask * 0.10 * hFade;

    // ── final ─────────────────────────────────────────────────────────────
    color *= skyMask;

    float lum = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(vec3(lum), color, 1.55);

    alpha = clamp(alpha * skyMask * OverlayAlpha * RainVisibility * vertexColor.a, 0.0, 0.72);

    if (alpha <= 0.003) discard;

    OutColor = vec4(color, alpha);
}
