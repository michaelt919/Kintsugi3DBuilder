#version 330
#include "PTMfit.glsl"
#include <shaders/colorappearance/colorappearance_multi_as_single.glsl>
#line 5 0
layout(location = 0) out vec4 colorInfo;
layout(location = 1) out vec3 lightDirTS;
void main(){
    vec3 lightDisplacement = getLightVector();
    //get light uv
    vec3 lightDir =normalize(lightDisplacement); // object space

    vec3 incidentRadiance = PI * lightIntensity / dot(lightDisplacement, lightDisplacement);

    vec3 triangleNormal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
    - dot(triangleNormal, fBitangent) * triangleNormal
    - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);

    lightDirTS = transpose(tangentToObject) * lightDir; // tangent space

    //get rgb
    colorInfo = step(0, lightDirTS.z /* n dot l */) * getLinearColor() / vec4(incidentRadiance, 1);
}