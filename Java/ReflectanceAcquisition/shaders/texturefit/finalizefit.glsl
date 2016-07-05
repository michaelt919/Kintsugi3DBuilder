#ifndef ERRORCALC_GLSL
#define ERRORCALC_GLSL

#include "../reflectance/reflectance.glsl"

#line 7 2004

#define SHIFT_FRACTION 0.00390625 // 1/256

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D specularEstimate;
uniform sampler2D roughnessEstimate;
uniform sampler2D sumSqError;

vec3 getDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

vec3 getSpecularColor()
{
    return pow(texture(specularEstimate, fTexCoord).rgb, vec3(gamma));
}

float getRoughness()
{
    return texture(roughnessEstimate, fTexCoord).r;
}

struct FinalizedFit
{
	vec3 diffuseColor;
	vec3 specularColor;
	float roughness;
	bool terminated;
};

FinalizedFit finalizeFit()
{
	float prevSumSqError, terminated = texture(prevSumSqError, fTexCoord).xy;
	
	if (terminated > 0.0)
	{
		return FinalizedFit(vec3(0), vec3(0), 0.0, true);
	}
	else
	{
		return FinalizedFit(diffuseColor, specularColor, roughness, false);
	}
}

#endif // ERRORCALC_GLSL
