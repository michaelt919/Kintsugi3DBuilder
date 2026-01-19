#version 330
#include <colorappearance/material.glsl>
//#include <specularfit/evaluateBRDF.glsl>
#include <colorappearance/linearize.glsl>
#line 6 0

//in vec3 specularColor;
layout(location = 0) out vec4 fragColor;

void main() {
    // getMaterial()
    Material m = getMaterial();
    fragColor = vec4(linearToSRGB(m.diffuseColor), 1);
}