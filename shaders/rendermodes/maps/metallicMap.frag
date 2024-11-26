#version 330
#include <colorappearance/material.glsl>

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 texCoords = getTexCoords();
    vec3 metallic = texture(ormMap.b, texCoords).rgb;
    fragColor = vec4(metallic, 1);
}