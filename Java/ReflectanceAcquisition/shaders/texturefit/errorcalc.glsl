#ifndef ERRORCALC_GLSL
#define ERRORCALC_GLSL

#include "../reflectance/reflectance.glsl"

#line 7 2006

#define MAX_ERROR 3.402822E38 // Max 32-bit floating-point is 3.4028235E38
#define MIN_SHIFT_FRACTION 0.00390625 // 1/256
//#define MIN_SHIFT_FRACTION 0.01171875 // 3/256

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D specularEstimate;
uniform sampler2D roughnessEstimate;
uniform sampler2D errorTexture;

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

struct ErrorResult
{
	bool mask;
	float dampingFactor;
	float sumSqError;
};

ErrorResult calculateError()
{
	vec4 prevErrorResult = texture(errorTexture, fTexCoord);
	
	if (prevErrorResult.x <= MIN_SHIFT_FRACTION)
	{
		return ErrorResult(false, 0.0, prevErrorResult.y);
	}
	else
	{
		vec3 geometricNormal = normalize(fNormal);
		vec3 diffuseNormal = getDiffuseNormalVector();
		vec3 diffuseColor = getDiffuseColor();
		vec3 specularColor = getSpecularColor();
		float roughness = getRoughness();
		float roughnessSquared = roughness * roughness;
		float maxLuminance = getMaxLuminance();
		float gammaInv = 1.0 / gamma;
		
		float sumSqError = 0.0;
		
		for (int i = 0; i < viewCount; i++)
		{
			vec3 view = normalize(getViewVector(i));
			float nDotV = max(0, dot(diffuseNormal, view));
			vec4 color = getLinearColor(i);
			
			if (color.a > 0 && nDotV > 0 && dot(geometricNormal, view) > 0)
			{
				vec3 lightPreNormalized = getLightVector(i);
				vec3 attenuatedLightIntensity = infiniteLightSources ? 
					getLightIntensity(i) : 
					getLightIntensity(i) / (dot(lightPreNormalized, lightPreNormalized));
				vec3 light = normalize(lightPreNormalized);
				float nDotL = max(0, dot(light, diffuseNormal));
				
				vec3 half = normalize(view + light);
				float nDotH = dot(half, diffuseNormal);
				
				if (nDotL > 0.0 && nDotH > 0.0)
				{
					float nDotHSquared = nDotH * nDotH;
						
					float q1 = roughnessSquared + (1.0 - nDotHSquared) / nDotHSquared;
					float mfdEval = roughnessSquared / (nDotHSquared * nDotHSquared * q1 * q1);
					
					float hDotV = max(0, dot(half, view));
					float geomRatio = min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV) / (4 * nDotV);
					
					vec3 colorScaled = pow(rgbToXYZ(color.rgb / attenuatedLightIntensity), vec3(gammaInv));
					vec3 currentFit = diffuseColor * nDotL + specularColor * mfdEval * geomRatio;
					vec3 colorResidual = colorScaled - pow(currentFit, vec3(gammaInv));
					
					sumSqError += dot(colorResidual, colorResidual);
				}
			}
		}
		
		sumSqError = min(sumSqError, MAX_ERROR);
		
		if (sumSqError >= prevErrorResult.y)
		{
			//return ErrorResult(false, prevErrorResult.x / 2, prevErrorResult.y);
			return ErrorResult(false, prevErrorResult.x * 2, prevErrorResult.y);
		}
		else
		{
			//return ErrorResult(true, prevErrorResult.x, sumSqError);
			return ErrorResult(true, prevErrorResult.x / 2, sumSqError);
		}
	}
}

#endif // ERRORCALC_GLSL
