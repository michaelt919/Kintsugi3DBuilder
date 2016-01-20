#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec4 specularColor;
layout(location = 1) out vec4 specularRoughness;
layout(location = 2) out vec4 specularNormalMap;
layout(location = 3) out vec4 debug;

#include "../reflectance/reflectance.glsl"
#include "../reflectance/imgspace.glsl"
#include "specularfit.glsl"

#line 17 0

void main()
{
    SpecularFit fit = fitSpecular();
    specularColor = vec4(pow(fit.color, vec3(1 / gamma)), 1.0);
    specularNormalMap = vec4(fit.normal * 0.5 + vec3(0.5), 1.0);
    specularRoughness = vec4(vec3(fit.roughness / specularRoughnessScale), 1.0);
}
