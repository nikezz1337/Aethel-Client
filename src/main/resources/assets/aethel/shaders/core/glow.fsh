#version 150

#moj_import <aethel:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform vec4 Radius;
uniform float GlowRadius;
uniform float Softness;
uniform float Intensity;

out vec4 OutColor;

float sdRoundRect(vec2 p, vec2 halfSize, float r) {
    vec2 d = abs(p) - (halfSize - vec2(r));
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - r;
}

void main() {
    vec2 outer = Size;

    vec2 p = FragCoord * outer - outer * 0.5;

    vec2 innerHalf = outer * 0.5 - vec2(GlowRadius);
    innerHalf = max(innerHalf, vec2(0.0));

    float r;
    if (p.x < 0.0) {
        r = (p.y < 0.0) ? Radius.x : Radius.w;
    } else {
        r = (p.y < 0.0) ? Radius.y : Radius.z;
    }
    r = clamp(r, 0.0, min(innerHalf.x, innerHalf.y));

    float dist = sdRoundRect(p, innerHalf, r);

    float edge = smoothstep(-Softness, 0.0, dist);

    float outside = max(dist, 0.0);
    float falloff = 1.0 - smoothstep(0.0, GlowRadius, outside);

    float a = edge * falloff * Intensity * FragColor.a;
    if (a <= 0.001) discard;

    OutColor = vec4(FragColor.rgb, a);
}
