#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec4 diffuseColor;
layout(location = 1) out vec4 normal;
layout(location = 2) out vec4 specularColor;
layout(location = 3) out vec4 roughness;

#include "../reflectance/reflectance.glsl"
#include "../reflectance/texspace.glsl"
#include "adjustfit.glsl"

#line 17 0

void main()
{
	ParameterizedFit fit = adjustFit();
    diffuseColor = vec4(pow(fit.diffuseColor, vec3(1 / gamma)), 1.0);
	normal = vec4(fit.normal * 0.5 + vec3(0.5), 1.0);
    specularColor = vec4(pow(fit.specularColor, vec3(1 / gamma)), 1.0);
	roughness = vec4(vec3(fit.roughness), 1.0);
}
