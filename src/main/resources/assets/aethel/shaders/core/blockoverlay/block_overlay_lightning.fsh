#version 150

uniform sampler2D Sampler0;
uniform vec2 texelSize;
uniform vec3 color;
uniform vec3 color2;
uniform float time;
uniform float speed;
uniform float scale;
uniform float outline;
uniform float glow;
uniform float fill;
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
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        value += noise(p) * amplitude;
        p = p * 2.02 + vec2(8.4, 5.7);
        amplitude *= 0.5;
    }
    return value;
}

float getEdge(vec2 uv, float s) {
    float a0 = texture(Sampler0, uv).a;
    float ax1 = texture(Sampler0, uv + vec2(texelSize.x * s, 0.0)).a;
    float ax2 = texture(Sampler0, uv - vec2(texelSize.x * s, 0.0)).a;
    float ay1 = texture(Sampler0, uv + vec2(0.0, texelSize.y * s)).a;
    float ay2 = texture(Sampler0, uv - vec2(0.0, texelSize.y * s)).a;
    float edge = abs(a0 - ax1) + abs(a0 - ax2) + abs(a0 - ay1) + abs(a0 - ay2);
    float diag1 = texture(Sampler0, uv + vec2(texelSize.x * s, texelSize.y * s)).a;
    float diag2 = texture(Sampler0, uv + vec2(-texelSize.x * s, texelSize.y * s)).a;
    float diag3 = texture(Sampler0, uv + vec2(texelSize.x * s, -texelSize.y * s)).a;
    float diag4 = texture(Sampler0, uv + vec2(-texelSize.x * s, -texelSize.y * s)).a;
    edge += 0.7 * (abs(a0 - diag1) + abs(a0 - diag2) + abs(a0 - diag3) + abs(a0 - diag4));
    return clamp(edge * 1.9, 0.0, 1.0);
}

void main() {
    vec2 uv = TexCoord;
    float mask = texture(Sampler0, uv).a;
    if (mask <= 0.0) discard;

    float edge = getEdge(uv, outline);
    float edgeBand = smoothstep(0.02, 0.42, edge);

    float t = time * max(speed, 0.001);
    vec2 flow = uv * mix(1.2, 3.2, clamp(scale / 3.0, 0.0, 1.0));
    vec2 drift = vec2(t * 0.20, -t * 0.15);

    float lightningMask = 0.0;
    vec2 luv = edge > 0.1 ? (uv * 6.0 + drift) : (uv * 5.0);
    float amp = 1.0;
    for (int i = 0; i < 3; i++) {
        float n = fbm(luv + vec2(time * 0.2, time * 0.15));
        float line = pow(max(0.0, 1.0 - abs(n - 0.5) * 4.0), 4.0);
        lightningMask += line * amp;
        luv = luv * 1.5 + vec2(3.7, 9.2);
        amp *= 0.6;
    }

    float flicker = 0.5 + 0.5 * pow(1.0 - abs(fbm(uv * 10.0 + vec2(time * 2.0, time * 1.5)) - 0.5) * 2.0, 2.0);
    lightningMask = lightningMask * flicker * edgeBand * 1.5;

    float edgeGlow = edgeBand * (0.3 + glow * 0.1);
    float boltStrength = clamp(lightningMask * fill, 0.0, 1.0);

    vec3 boltColor = mix(color, vec3(1.0), 0.6);
    vec3 col = mix(color, boltColor, boltStrength);
    col += color2 * edgeGlow * 0.3;

    float outAlpha = clamp(alpha * (boltStrength * 0.9 + edgeGlow * 0.5) * mask, 0.0, 1.0);
    if (outAlpha <= 0.001) discard;
    OutColor = vec4(col, outAlpha);
}
