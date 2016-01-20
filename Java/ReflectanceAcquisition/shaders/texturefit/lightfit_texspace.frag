#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec4 lightPosition;
layout(location = 1) out vec4 lightIntensity;

#include "../reflectance/texspace.glsl"
#include "lightfit.glsl"

#line 14 0

void main()
{
    LightFit fit = fitLight();
    lightPosition = vec4(fit.position, fit.quality);
    lightIntensity = vec4(vec3(fit.intensity), fit.quality);
}
