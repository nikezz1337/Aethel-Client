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

float edgeDist(vec2 uv) {
    vec2 texel = 1.0 / max(texSize, vec2(1.0));
    float m = sampleMask(uv);
    float minDist = 1.0;

    for (float ix = -6.0; ix <= 6.0; ix += 1.0) {
        for (float iy = -6.0; iy <= 6.0; iy += 1.0) {
            vec2 off = vec2(ix, iy) * texel * radius;
            float s = sampleMask(uv + off);
            if (s > 0.5 && m < 0.5) {
                float d = length(vec2(ix, iy)) / 6.0;
                minDist = min(minDist, d);
            }
        }
    }

    return minDist;
}

void main() {
    vec2 uv = TexCoord;
    vec2 texel = 1.0 / max(texSize, vec2(1.0));
    float mask = sampleMask(uv);

    float edge = 0.0;
    float r = radius * 0.5;
    for (float i = 1.0; i <= 8.0; i++) {
        float angle = i * 0.785398;
        vec2 off = vec2(cos(angle), sin(angle)) * texel * r;
        edge += sampleMask(uv + off);
        edge += sampleMask(uv - off);
    }
    edge /= 16.0;

    float glow = edge * (1.0 - mask * 0.6);

    float outerSoft = 0.0;
    float r2 = radius * 1.2;
    for (float i = 1.0; i <= 8.0; i++) {
        float angle = i * 0.785398 + 0.392699;
        vec2 off = vec2(cos(angle), sin(angle)) * texel * r2;
        outerSoft += sampleMask(uv + off);
        outerSoft += sampleMask(uv - off);
    }
    outerSoft /= 16.0;
    glow += outerSoft * 0.4 * (1.0 - mask);

    float n1 = fbm(uv * 5.0 + vec2(time * 0.12, -time * 0.08));
    float haze = glow * (0.7 + n1 * 0.6);

    float sparkle = pow(noise(uv * 35.0 + vec2(time * 0.7, time * 0.4)), 14.0);
    float sparkleGlow = sparkle * glow * 2.0;

    float pulse = 0.92 + 0.08 * sin(time * 2.5);
    float breathe = 0.94 + 0.06 * sin(time * 1.3 + n1 * 6.28);

    float alpha = (haze + sparkleGlow) * intensity * pulse * breathe;
    alpha = clamp(alpha, 0.0, 1.0);
    if (alpha < 0.002) discard;

    float t = clamp(glow * 2.0 + n1 * 0.3, 0.0, 1.0);
    vec3 col = mix(glowColor.rgb, glowColor2.rgb, t);
    col += sparkle * glow * glowColor2.rgb * 1.2;
    col *= pulse * breathe;

    OutColor = vec4(col * alpha, alpha);
}
