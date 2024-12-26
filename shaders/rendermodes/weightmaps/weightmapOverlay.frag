#version 330
#include <colorappearance/material.glsl>
#include <specularfit/evaluateBRDF.glsl>

#ifndef WEIGHTMAP_INDEX
#define WEIGHTMAP_INDEX 0
#endif

//in vec3 specularColor;
layout(location = 0) out vec4 fragColor;

void main() {
    vec4 weightmapTex = texture(weightMaps, vec3(fTexCoord, WEIGHTMAP_INDEX));

    Material m = getMaterial();
    fragColor = vec4(vec4(m.specularColor, 1) + (weightmapTex.r * vec4(0.9,0,1,1)));

}