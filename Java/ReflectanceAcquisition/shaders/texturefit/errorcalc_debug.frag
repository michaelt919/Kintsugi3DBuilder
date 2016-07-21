#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec2 sumSqError;

#include "../reflectance/reflectance.glsl"
#include "../reflectance/debug.glsl"
#include "errorcalc.glsl"

#line 14 0

void main()
{
	ErrorResult errorResult = calculateError();
	if (errorResult.terminated)
	{
		sumSqError = vec2(0.0, errorResult.sumSqError);
	}
	else
	{
		sumSqError = vec2(1.0, errorResult.sumSqError);
	}
}
