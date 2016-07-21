#ifndef REFLECTANCE_DEBUG_GLSL
#define REFLECTANCE_DEBUG_GLSL

#include "reflectance.glsl"

#line 7 1109

#define DIFFUSE_COLOR vec3(0.5)
#define SPECULAR_COLOR vec3(0.5)
#define ROUGHNESS_SQUARED 0.25

vec4 getColor(int index)
{
    vec3 normal = normalize(fNormal);
    vec3 view = normalize(getViewVector(index));
	float nDotV = max(0, dot(normal, view));
	
	if (nDotV > 0)
	{
		vec3 lightPreNormalized = getLightVector(index);
		vec3 attenuatedLightIntensity = infiniteLightSources ? 
			getLightIntensity(index) : 
			getLightIntensity(index) / (dot(lightPreNormalized, lightPreNormalized));
		vec3 light = normalize(lightPreNormalized);
		float nDotL = max(0, dot(light, normal));
		
		vec3 half = normalize(view + light);
		float nDotH = dot(half, normal);
		
		if (nDotH > 0.0)
		{
			float nDotHSquared = nDotH * nDotH;
			
			// float mfdEval = exp((nDotHSquared - 1.0) / (nDotHSquared * ROUGHNESS_SQUARED)) 
				// / (ROUGHNESS_SQUARED * nDotHSquared * nDotHSquared);
				
			float q = ROUGHNESS_SQUARED + (1 - nDotHSquared) / nDotHSquared;
			float mfdEval =  ROUGHNESS_SQUARED / (nDotHSquared * nDotHSquared * q * q);
			
			float hDotV = max(0, dot(half, view));
				
			return vec4(pow((DIFFUSE_COLOR * nDotL + SPECULAR_COLOR * mfdEval / (4 * nDotV) 
					* min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV)) * attenuatedLightIntensity
					/ getMaxLuminance(), vec3(1.0 / gamma)), 1.0);
		}
		else
		{
			return vec4(0.0);
		}
	}
	else
	{
		return vec4(0.0);
	}
}

#endif // REFLECTANCE_DEBUG_GLSL
