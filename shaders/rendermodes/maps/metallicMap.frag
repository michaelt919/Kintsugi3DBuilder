#version 330
#include <colorappearance/material.glsl>

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 texCoords = getTexCoords();
    float metallic = texture(ormMap, texCoords).b;
    fragColor = vec4(vec3(metallic), 1);
}