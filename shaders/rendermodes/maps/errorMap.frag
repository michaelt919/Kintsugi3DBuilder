#version 330
#include <colorappearance/material.glsl>

uniform sampler2D metadataMap_error;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 texCoords = getTexCoords();
    vec3 albedo = texture(albedoMap, texCoords).rgb;
    vec3 error = texture(metadataMap_error, texCoords).rgb;
    float sqrtErrorSum = sqrt(error.r + error.g + error.b);
    fragColor = vec4(mix(vec3(0,1,0), mix(vec3(1,1,0), vec3(1,0,0), clamp(sqrtErrorSum * 2 - 1, 0, 1)), clamp(sqrtErrorSum * 2, 0, 1)), 1);
}