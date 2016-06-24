#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec4 specularColor;
layout(location = 1) out vec4 diffuseColor;

#include "../reflectance/reflectance.glsl"
#include "../reflectance/texspace.glsl"
#include "specularfit2.glsl"

#line 15 0

void main()
{
	TwoColorFit fit = fitSpecular();
    specularColor = vec4(pow(fit.specularColor, vec3(1 / gamma)), 1.0);
    diffuseColor = vec4(pow(fit.diffuseColor, vec3(1 / gamma)), 1.0);
}
