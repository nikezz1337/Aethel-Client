#version 150

uniform sampler2D Sampler0;

uniform vec2 texSize;
uniform float time;
uniform float intensity;
uniform float radius;
uniform vec4 glowColor;
uniform vec4 glowColor2;

in vec2 TexCoord;
out vec4 OutColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += noise(p) * a;
        p = p * 2.04 + vec2(8.3, 2.1);
        a *= 0.5;
    }
    return v;
}

float sampleMask(vec2 uv) {
    return texture(Sampler0, clamp(uv, vec2(0.0), vec2(1.0))).r;
}

void main() {
    vec2 uv = TexCoord;
    vec2 texel = 1.0 / max(texSize, vec2(1.0));
    float mask = sampleMask(uv);

    float edge = 0.0;
    float r = radius * 0.4;
    for (float i = 1.0; i <= 8.0; i++) {
        float angle = i * 0.785398;
        vec2 off = vec2(cos(angle), sin(angle)) * texel * r;
        edge += sampleMask(uv + off);
        edge += sampleMask(uv - off);
    }
    edge /= 16.0;

    float edgeFactor = edge * (1.0 - mask * 0.6);
    if (edgeFactor < 0.005) discard;

    float lightningMask = 0.0;
    vec2 luv = uv * 5.0;
    float amp = 1.0;
    for (int i = 0; i < 3; i++) {
        float n = fbm(luv + vec2(time * 0.2, time * 0.15));
        float line = pow(max(0.0, 1.0 - abs(n - 0.5) * 4.0), 4.0);
        lightningMask += line * amp;
        luv = luv * 1.5 + vec2(3.7, 9.2);
        amp *= 0.6;
    }

    float flicker = 0.5 + 0.5 * pow(1.0 - abs(fbm(uv * 12.0 + vec2(time * 2.0, time * 1.5)) - 0.5) * 2.0, 2.0);
    lightningMask = lightningMask * flicker * edgeFactor * 1.5;

    float outerGlow = 0.0;
    float r2 = radius * 1.5;
    for (float i = 1.0; i <= 8.0; i++) {
        float angle = i * 0.785398 + 0.392699;
        vec2 off = vec2(cos(angle), sin(angle)) * texel * r2;
        outerGlow += sampleMask(uv + off);
        outerGlow += sampleMask(uv - off);
    }
    outerGlow /= 16.0;
    outerGlow *= 0.5 * (1.0 - mask);

    float boltIntensity = clamp(lightningMask * intensity, 0.0, 1.0);
    float glowIntensity = clamp(outerGlow * intensity, 0.0, 1.0);

    float alpha = max(boltIntensity, glowIntensity);
    if (alpha < 0.002) discard;

    vec3 themeCol = glowColor.rgb;
    vec3 themeCol2 = glowColor2.rgb;

    float bright = max(themeCol.r, max(themeCol.g, themeCol.b));
    vec3 whiteCore = vec3(min(bright * 0.5 + 0.5, 1.0));

    float mixT = clamp(lightningMask * 1.5, 0.0, 1.0);
    vec3 col = mix(themeCol, whiteCore, mixT);
    col = mix(col, themeCol2, glowIntensity * 0.3);

    float coreGlow = 1.0 - abs(boltIntensity - 0.5) * 2.0;
    coreGlow = clamp(coreGlow * 2.0, 0.0, 1.0);
    col += whiteCore * coreGlow * 0.3;

    OutColor = vec4(col, alpha);
}
