#version 330
#include "PTMfit.glsl"
#include <shaders/colorappearance/colorappearance_multi_as_single.glsl>
#line 5 0
layout(location = 0) out vec4 colorInfo;
layout(location = 1) out vec3 lightDir;
void main(){
    vec3 lightDisplacement = getLightVector();
    //get light uv
    lightDir=normalize(lightDisplacement);

    vec3 incidentRadiance = PI * lightIntensity / dot(lightDisplacement, lightDisplacement);

    //get rgb
    colorInfo = step(0, dot(lightDir, fNormal)) * getLinearColor() / vec4(incidentRadiance, 1);
}