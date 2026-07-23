#version 150

uniform vec3 color;
uniform vec3 color2;
uniform float time;
uniform float speed;
uniform float edgeWidth;
uniform float alpha;

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
    for (int i = 0; i < 5; i++) {
        v += noise(p) * a;
        p = p * 2.03 + vec2(8.4, 5.7);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = TexCoord;
    float t = time * max(speed, 0.001);

    float centerDist = abs(uv.y - 0.5) * 2.0;
    float edgeMask = 1.0 - centerDist;
    edgeMask = pow(edgeMask, 0.8); // менее агрессивное затухание к краям

    vec2 flow = vec2(uv.x * 4.0 + t * 0.5, uv.y * 2.0);

    float energy = 0.0;
    vec2 euv = flow;
    float amp = 1.5; // увеличена амплитуда энергии
    for (int i = 0; i < 4; i++) {
        float n = fbm(euv + vec2(t * 0.1, t * 0.07));
        float line = pow(max(0.0, 1.0 - abs(n - 0.5) * 3.5), 4.0);
        energy += line * amp;
        euv = euv * 1.7 + vec2(3.1, 9.2);
        amp *= 0.55;
    }

    float flicker = 0.7 + 0.3 * pow(1.0 - abs(fbm(uv * 8.0 + vec2(t * 1.5, t * 1.2)) - 0.5) * 2.0, 2.5);
    energy *= flicker;

    float pulse = 0.8 + 0.2 * sin(t * 2.0 + uv.x * 6.28);
    energy *= pulse;

    float core = pow(1.0 - centerDist, 2.5);
    float glow = pow(1.0 - centerDist, 1.2) * 0.8;

    float intensity = clamp(energy * edgeMask * 2.5, 0.0, 1.0);

    vec3 brightCol = vec3(1.0, 1.0, 1.0); // чистый белый для максимальной яркости
    vec3 edgeCol = mix(color, brightCol, core * intensity);
    edgeCol = mix(edgeCol, color2, glow * 0.4);

    float outAlpha = clamp(alpha * (intensity * 1.0 + glow * 0.5) * edgeMask, 0.0, 1.0);
    if (outAlpha <= 0.003) discard;
    OutColor = vec4(edgeCol, outAlpha);
}
