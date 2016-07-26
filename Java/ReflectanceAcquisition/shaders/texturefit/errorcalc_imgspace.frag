#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec2 errorResult;
layout(location = 1) out float mask;

#include "../reflectance/reflectance.glsl"
#include "../reflectance/imgspace.glsl"
#include "errorcalc.glsl"

#line 15 0

void main()
{
	ErrorResult errorResult = calculateError();
	if (errorResult.mask)
	{
		errorResult = vec2(errorResult.dampingFactor, errorResult.sumSqError);
		mask = 1;
	}
	else
	{
		errorResult = vec2(errorResult.dampingFactor, errorResult.sumSqError);
		mask = 0;
	}
}
