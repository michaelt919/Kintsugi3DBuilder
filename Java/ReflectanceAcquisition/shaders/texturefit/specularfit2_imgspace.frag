#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec4 specularColor;

#include "../reflectance/reflectance.glsl"
#include "../reflectance/imgspace.glsl"
#include "specularfit2.glsl"

#line 14 0

void main()
{
    specularColor = vec4(fitSpecular(), 1.0);
}
