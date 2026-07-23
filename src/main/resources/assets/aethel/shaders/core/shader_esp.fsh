#version 150

in vec4 vertexColor;

uniform sampler2D Sampler0;
uniform vec2 Direction;
uniform float Radius;

out vec4 fragColor;

void main() {
    vec2 texelSize = 1.0 / textureSize(Sampler0, 0);
    vec2 uv = gl_FragCoord.xy * texelSize;
    float centerAlpha = texture(Sampler0, uv).a;
    float result = centerAlpha * 0.227027;
    float totalWeight = 0.227027;

    for (float i = 1.0; i <= 10.0; i += 1.0) {
        if (i > Radius) break;
        float weight = 0.1945946 * (1.0 - i / 11.0);
        vec2 offset = Direction * texelSize * i;
        result += texture(Sampler0, uv + offset).a * weight;
        result += texture(Sampler0, uv - offset).a * weight;
        totalWeight += weight * 2.0;
    }

    float blurredAlpha = clamp(result / totalWeight, 0.0, 1.0);
    fragColor = vec4(vertexColor.rgb * blurredAlpha, blurredAlpha * vertexColor.a);
}
