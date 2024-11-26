#version 330
#include <colorappearance/material.glsl>
#include <specularfit/evaluateBRDF.glsl>

layout(location = 0) out vec4 fragColor;

void main() {
    //vec4 red = vec4(1, 0, 0, 1);
    fragColor = texture(weightMaps, vec3(fTexCoord, 3));
}