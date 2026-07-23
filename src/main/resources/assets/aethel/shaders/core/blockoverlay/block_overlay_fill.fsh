#version 150

uniform vec3 color;
uniform vec3 color2;
uniform float time;
uniform float speed;
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
    for (int i = 0; i < 4; i++) {
        v += noise(p) * a;
        p = p * 2.03 + vec2(8.4, 5.7);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = TexCoord;
    float t = time * max(speed, 0.001);

    vec2 flow = uv * 3.0;
    vec2 drift = vec2(t * 0.15, -t * 0.1);

    vec2 warp = vec2(
        fbm(flow * 0.8 + drift * 0.6 + vec2(0.0, 4.1)),
        fbm(flow * 0.7 - drift * 0.4 + vec2(3.7, 1.8))
    );
    vec2 q = flow + (warp - 0.5) * 1.5;

    float mist = fbm(q * 0.6 - drift * 0.2 + vec2(4.2, 8.1));
    float veins = 1.0 - abs(noise(q * 1.5 + vec2(mist * 2.0, mist * 1.2) - drift * 0.4) * 2.0 - 1.0);
    veins = pow(clamp(veins, 0.0, 1.0), 2.0);

    float stripe = 1.0 - abs(sin((q.x * 0.8 + q.y * 0.3) * 2.0 + mist * 3.0));
    stripe = pow(clamp(stripe, 0.0, 1.0), 4.0);

    float energy = clamp(mist * 0.3 + veins * 0.6 + stripe * 0.4, 0.0, 1.0);
    float core = smoothstep(0.2, 0.9, energy);

    vec3 mixColor = mix(color, color2, clamp(core * 0.7 + stripe * 0.3, 0.0, 1.0));

    float edgeDist = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float vignette = smoothstep(0.0, 0.15, edgeDist);

    float outAlpha = alpha * (0.3 + core * 0.7) * vignette;
    if (outAlpha <= 0.003) discard;
    OutColor = vec4(mixColor, outAlpha);
}
