#version 330
#include "PTMfit.glsl"

layout(location = 0) out vec4 colorInfo;
layout(location = 1) out vec3 lightDir;

void main(){
    //get rgb
    colorInfo=getLinearColor();
    vec3 lightDisplacement = getLightVector();
    //get light uv
    lightDir=normalize(lightDisplacement);
}