#version 150

#ifndef OVERLAY_ENTITY_PASS
#define OVERLAY_ENTITY_PASS 0
#endif

#if OVERLAY_ENTITY_PASS == 1
#moj_import <minecraft:fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
#endif

in vec4 vertexColor;

uniform float Time;
uniform vec2 Mouse;
uniform vec2 Resolution;
uniform float OverlayAlpha;
uniform float Mode;

out vec4 fragColor;

float randomValue(vec2 pos) {
    return fract(sin(dot(pos, vec2(13.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 pos) {
    vec2 i = floor(pos);
    vec2 f = fract(pos);
    float a = randomValue(i + vec2(0.0, 0.0));
    float b = randomValue(i + vec2(1.0, 0.0));
    float c = randomValue(i + vec2(0.0, 1.0));
    float d = randomValue(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 pos) {
    float v = 0.0;
    float a = 0.5;
    vec2 shift = vec2(100.0);
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < 6; i++) {
        float dir = mod(float(i), 2.0) > 0.5 ? 1.0 : -1.0;
        v += a * noise(pos - 0.05 * dir * Time);
        pos = rot * pos * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

vec3 effectClassic(vec2 p) {
    vec3 color1 = vec3(0.0, 0.3, 0.5);
    vec3 color2 = vec3(0.5, 0.0, 0.3);

    float f = 0.0;
    float g = 0.0;
    float h = 0.0;
    const float PI = 3.14159265;
    float maxIterations = floor(clamp(Mouse.x, 0.0, 1.0) * 41.0);

    for (int i = 0; i < 40; i++) {
        if (float(i) > maxIterations) break;

        float s = sin(Time + float(i) * PI / 2.0) * 0.8;
        float c = cos(Time + float(i) * PI / 2.0) * 0.8;
        float d = max(abs(p.x + c), 0.0001);
        float e = max(abs(p.y + s), 0.0001);
        f += 0.001 / d;
        g += 0.001 / e;
        h += 0.00003 / (d * e);
    }

    return clamp(f * color1 + g * color2 + vec3(h), 0.0, 1.0);
}

vec3 effectNebula(vec2 p) {
    vec2 q = vec2(0.0);
    q.x = fbm(p);
    q.y = fbm(p + vec2(1.0));

    vec2 r = vec2(0.0);
    r.x = fbm(p + q + vec2(1.7, 1.2) + 0.15 * Time);
    r.y = fbm(p + q + vec2(8.3, 2.8) + 0.126 * Time);

    float f = fbm(p + r);
    vec3 color = mix(
        vec3(1.0, 1.0, 2.0),
        vec3(1.0),
        clamp((f * f) * 5.5, 0.0, 1.0)
    );
    color = mix(color, vec3(1.0), clamp(length(q), 0.0, 1.0));
    color = mix(color, vec3(0.3, 0.2, 1.0), clamp(abs(r.x), 0.0, 1.0));
    return clamp((f * f * f + 0.9 * f) * color, 0.0, 1.0);
}

vec3 effectCosmic(vec2 p) {
    vec3 dir = vec3(p * 1.4, 1.0);
    mat2 rot = mat2(cos(0.0), sin(0.0), -sin(0.0), cos(0.0));
    dir.xz *= rot;
    dir.xy *= rot;

    vec3 from = vec3(1.25 * sin(Time), -1.03 * Time, -2.0);
    from.xz *= rot;
    from.xy *= rot;

    float s = 0.1;
    float fade = 0.07;
    vec3 v = vec3(0.4);

    for (int r = 0; r < 12; r++) {
        vec3 pos = from + s * dir * 1.5;
        pos = abs(vec3(0.750) - mod(pos, vec3(1.5)));
        pos.x += float(r * r) * 0.01;
        pos.y += float(r) * 0.02;

        float prevLength = 0.0;
        float accumulator = 0.0;
        for (int i = 0; i < 15; i++) {
            pos = abs(pos) / dot(pos, pos) - 0.340;
            float currentLength = length(pos);
            accumulator += abs(currentLength - prevLength * 0.2);
            prevLength = currentLength;
        }

        accumulator *= accumulator * accumulator * 2.0;
        v += vec3(s, s * s, s * s * s * s) * accumulator * 0.0017 * fade;
        fade *= 0.960;
        s += 0.110;
    }

    return mix(vec3(length(v)), v, 0.8) * 0.01;
}

vec3 overlayEffectColor(vec2 p, vec2 safeResolution) {
    if (Mode < 0.5) {
        return effectClassic(p);
    }

    if (Mode < 1.5) {
        vec2 nebulaPos = (gl_FragCoord.xy * 3.0 - safeResolution.xy) / min(safeResolution.x, safeResolution.y);
        nebulaPos -= vec2(12.0, 0.0);
        return effectNebula(nebulaPos);
    }

    vec2 cosmicUv = gl_FragCoord.xy / safeResolution.xy - 0.5;
    cosmicUv.y *= safeResolution.y / safeResolution.x;
    return effectCosmic(cosmicUv);
}

void main() {
#if OVERLAY_ENTITY_PASS == 1
    vec4 baseColor = texture(Sampler0, texCoord0);
#ifdef ALPHA_CUTOUT
    if (baseColor.a < ALPHA_CUTOUT) {
        discard;
    }
#endif
    baseColor *= vertexColor * ColorModulator;
#ifndef NO_OVERLAY
    baseColor.rgb = mix(overlayColor.rgb, baseColor.rgb, overlayColor.a);
#endif
    baseColor *= lightMapColor;

    vec3 baseRgb = baseColor.rgb;
    float baseAlpha = baseColor.a;
#else
    vec3 baseRgb = vertexColor.rgb;
    float baseAlpha = vertexColor.a;
#endif

    vec2 safeResolution = max(Resolution, vec2(1.0));
    vec2 p = (gl_FragCoord.xy * 2.0 - safeResolution) / min(safeResolution.x, safeResolution.y);

    vec3 fxColor = overlayEffectColor(p, safeResolution);
    float alphaFactor = clamp(OverlayAlpha, 0.0, 1.0);
    float luma = dot(baseRgb, vec3(0.299, 0.587, 0.114));

#if OVERLAY_ENTITY_PASS == 1
    vec3 effectColor = baseRgb * fxColor;
    vec3 mixedColor = mix(baseRgb, effectColor, alphaFactor);
    vec4 result = vec4(mixedColor, baseAlpha);
    fragColor = linear_fog(result, vertexDistance, FogStart, FogEnd, FogColor);
#else
    vec3 mixedColor;
    float outAlpha;

    if (Mode < 0.5) {
        // Original BlockOverlay classic shader behavior.
        mixedColor = effectClassic(p) * baseRgb;
        outAlpha = clamp(alphaFactor * baseAlpha, 0.0, 1.0);
    } else if (Mode < 1.5) {
        // Original BlockOverlay nebula shader behavior.
        vec2 nebulaPos = (gl_FragCoord.xy * 3.0 - safeResolution.xy) / min(safeResolution.x, safeResolution.y);
        nebulaPos -= vec2(12.0, 0.0);
        mixedColor = effectNebula(nebulaPos) * baseRgb;

        vec2 uv = gl_FragCoord.xy / safeResolution.xy;
        float sideFade = 50.0 - max(
            pow(100.0 * distance(uv.x, -1.0), 0.0),
            pow(2.0 * distance(uv.y, 0.5), 5.0)
        );
        float baseNebulaAlpha = clamp(sideFade / 50.0, 0.0, 1.0);
        outAlpha = clamp(baseNebulaAlpha * mixedColor.r * alphaFactor * baseAlpha, 0.0, 1.0);
    } else {
        // Original BlockOverlay cosmic shader behavior.
        vec2 cosmicUv = gl_FragCoord.xy / safeResolution.xy - 0.5;
        cosmicUv.y *= safeResolution.y / safeResolution.x;
        mixedColor = effectCosmic(cosmicUv) * baseRgb;
        outAlpha = clamp(alphaFactor * baseAlpha, 0.0, 1.0);
    }

    if (outAlpha <= 0.0) {
        discard;
    }
    fragColor = vec4(mixedColor, outAlpha);
#endif
}
