#version 330
#include <colorappearance/material.glsl>
#include <specularfit/evaluateBRDF.glsl>

layout(location = 0) out vec4 fragColor;


void main() {
    vec4 weightmap0 = texture(weightMaps, vec3(fTexCoord, 0)).r * vec4(0,0,1,1);
    vec4 weightmap1 = texture(weightMaps, vec3(fTexCoord, 1)).r * vec4(0,1,0,1);
    vec4 weightmap2 = texture(weightMaps, vec3(fTexCoord, 2)).r * vec4(1,0,0,1);
    vec4 weightmap3 = texture(weightMaps, vec3(fTexCoord, 3)).r * vec4(1,0,1,1);
    vec4 weightmap4 = texture(weightMaps, vec3(fTexCoord, 4)).r * vec4(1,1,0,1);
    vec4 weightmap5 = texture(weightMaps, vec3(fTexCoord, 5)).r * vec4(0,1,1,1);
    vec4 weightmap6 = texture(weightMaps, vec3(fTexCoord, 6)).r * vec4(0.5,0,1,1);
    vec4 weightmap7 = texture(weightMaps, vec3(fTexCoord, 7)).r * vec4(0,0.5,0.5,1);


    fragColor = weightmap0 + weightmap1 + weightmap2 + weightmap3 + weightmap4 + weightmap5 + weightmap6 + weightmap7;
}