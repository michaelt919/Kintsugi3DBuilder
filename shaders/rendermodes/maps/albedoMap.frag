#version 330
#include <colorappearance/material.glsl>

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 texCoords = getTexCoords();
    vec3 albedo = texture(albedoMap, texCoords).rgb;
    fragColor = vec4(albedo, 1);
}