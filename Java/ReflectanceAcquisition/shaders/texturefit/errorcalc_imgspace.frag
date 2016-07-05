#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec4 sumSqError;

#include "../reflectance/reflectance.glsl"
#include "../reflectance/imgspace.glsl"
#include "errorcalc.glsl"

#line 16 0

void main()
{
	ErrorResult errorResult = calcError();
	if (errorResult.terminated)
	{
	}
	else
	{
	}
}
