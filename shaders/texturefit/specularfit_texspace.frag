#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 diffuseColor;
layout(location = 1) out vec4 normal;
layout(location = 2) out vec4 specularColor;
layout(location = 3) out vec4 roughness;
layout(location = 4) out vec4 roughnessStdDev;

#include "../colorappearance/colorappearance.glsl"
#include "../colorappearance/texspace.glsl"
#include "specularfit.glsl"

#line 20 0

void main()
{
    ParameterizedFit fit = fitSpecular();
    diffuseColor = vec4(pow(fit.diffuseColor.rgb, vec3(1 / gamma)), fit.diffuseColor.a);
    normal = vec4(fit.normal.xyz * 0.5 + vec3(0.5), fit.normal.w);
    specularColor = vec4(pow(fit.specularColor.rgb, vec3(1 / gamma)), fit.specularColor.a);
    roughness = vec4(sqrt(fit.roughness.xyz), fit.roughness.w);
    roughnessStdDev = fit.roughnessStdDev;
}
