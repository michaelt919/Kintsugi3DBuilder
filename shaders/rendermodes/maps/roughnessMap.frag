#version 330
#include <colorappearance/material.glsl>
//#include <specularfit/evaluateBRDF.glsl>

//in vec3 specularColor;
layout(location = 0) out vec4 fragColor;

void main() {
    // getMaterial()
    vec2 texCoords = getTexCoords();
    vec3 roughness = texture(roughnessMap, texCoords).rgb;
    fragColor = vec4(roughness, 1);
}