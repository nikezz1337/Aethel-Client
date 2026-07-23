#version 150

in vec2 TexCoord;
in vec4 vertexColor;

uniform sampler2D Sampler0;
uniform float Strength;
uniform float Desat;
uniform float Contrast;
uniform vec3 VignetteColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, TexCoord);

    vec3 result = color.rgb;

    float lum = dot(result, vec3(0.2126, 0.7152, 0.0722));
    result = mix(result, vec3(lum), Desat);

    result = (result - 0.5) * (1.0 + Contrast) + 0.5;
    result = clamp(result, 0.0, 1.0);

    vec2 uv = TexCoord;
    vec2 centered = uv - 0.5;
    float dist = length(centered) * 1.41421356;
    float vig = smoothstep(0.4, 1.2, dist);
    result = mix(result, VignetteColor, vig * Strength);

    fragColor = vec4(result, color.a);
}
