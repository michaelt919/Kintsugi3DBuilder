#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec2 errorResultOut;
layout(location = 1) out float mask;

#include "../colorappearance/colorappearance.glsl"
#include "../colorappearance/imgspace.glsl"
#include "errorcalc.glsl"

#line 15 0

void main()
{
	ErrorResult errorResult = calculateError();
	if (errorResult.mask)
	{
		errorResultOut = vec2(errorResult.dampingFactor, errorResult.sumSqError);
		mask = 1;
	}
	else
	{
		errorResultOut = vec2(errorResult.dampingFactor, errorResult.sumSqError);
		mask = 0;
	}
}
