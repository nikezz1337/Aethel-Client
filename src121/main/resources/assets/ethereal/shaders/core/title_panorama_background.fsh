#version 150

in vec4 vertexColor;

uniform vec2 resolution;
uniform float time;
uniform vec3 color1;

out vec4 OutColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

mat2 rot2(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, -s, s, c);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 r = rot2(0.5);
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = r * p * 2.0 + vec2(5.2, 1.3);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(resolution, vec2(1.0));
    float t = time * 0.09;

    vec2 q = vec2(
        fbm(uv + t * 0.3),
        fbm(uv + vec2(5.2, 1.3) + t * 0.3)
    );
    vec2 r = vec2(
        fbm(uv + q + vec2(1.7, 9.2) + t * 0.15),
        fbm(uv + q + vec2(8.3, 2.8) + t * 0.15)
    );
    float f = fbm(uv + r + t * 0.05);

    vec3 darkColor = color1 * 0.2;
    vec3 midColor = color1 * 0.6;
    vec3 brightColor = color1;
    vec3 highlightColor = mix(color1, vec3(1.0), 0.4);

    vec3 col = mix(darkColor, midColor, smoothstep(0.2, 0.5, f));
    col = mix(col, brightColor, smoothstep(0.4, 0.7, f));
    col = mix(col, highlightColor, smoothstep(0.6, 0.9, f) * 0.55);

    vec2 vig = uv - 0.5;
    col *= 1.0 - dot(vig, vig) * 1.4;
    col = max(col, vec3(0.0));

    OutColor = vec4(col, vertexColor.a);
}