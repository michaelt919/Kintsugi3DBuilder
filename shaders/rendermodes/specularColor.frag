#version 330
#include <colorappearance/material.glsl>
//#include <specularfit/evaluateBRDF.glsl>

//in vec3 specularColor;
layout(location = 0) out vec4 fragColor;

void main() {
    // getMaterial()
    Material m = getMaterial();
    fragColor = vec4(m.specularColor, 1);
}