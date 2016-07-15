#ifndef ADJUSTMENT_GLSL
#define ADJUSTMENT_GLSL

#include "../reflectance/reflectance.glsl"

#line 7 2005

//#define SHIFT_FRACTION 0.01171875 // 3/256
#define SHIFT_FRACTION 0.125

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D specularEstimate;
uniform sampler2D roughnessEstimate;
uniform sampler2D prevSumSqError;

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

struct ParameterizedFit
{
	vec3 diffuseColor;
	vec3 normal;
	vec3 specularColor;
	float roughness;
};

ParameterizedFit adjustFit()
{
	if (texture(prevSumSqError, fTexCoord).x < 1.0)
	{
		discard;
	}
	else
	{
		vec3 geometricNormal = normalize(fNormal);
		vec3 diffuseNormal = geometricNormal;//getDiffuseNormalVector();
		vec3 prevDiffuseColor = vec3(0.5);//max(vec3(pow(SHIFT_FRACTION, gamma)), getDiffuseColor());
		vec3 prevSpecularColor = vec3(0.5);//max(vec3(pow(SHIFT_FRACTION, gamma)), getSpecularColor());
		float prevRoughness = 0.5;//max(SHIFT_FRACTION, getRoughness());
		float roughnessSquared = prevRoughness * prevRoughness;
		float gammaInv = 1.0 / gamma;
		
		// Partitioned matrix:  [ A B ]
		//						[ C D ]
		mat3   mA = mat3(0);
		mat4x3 mB = mat4x3(0);
		mat3x4 mC = mat3x4(0);
		mat4   mD = mat4(0);
		
		vec3 v1 = vec3(0);
		vec4 v2 = vec4(0);
		
		for (int i = 0; i < viewCount; i++)
		{
			vec3 view = normalize(getViewVector(i));
			float nDotV = max(0, dot(diffuseNormal, view));
			
			// Values of 1.0 for this color would correspond to the expected reflectance
			// for an ideal diffuse reflector (diffuse albedo of 1), which is a reflectance of 1 / pi.
			// Hence, this color corresponds to the reflectance times pi.
			// Both the Phong model and the Cook Torrance with a Beckmann distribution also have a 1/pi factor.
			// By adopting the convention that all reflectance values are scaled by pi in this shader,
			// We can avoid division by pi here as well as avoiding the 1/pi factors in the parameterized models.
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
					
					float q2 = 1.0 + (roughnessSquared - 1.0) * nDotHSquared;
					float mfdDeriv = 1.0 - (roughnessSquared + 1.0) * nDotHSquared / (q2 * q2 * q2);
					
					float hDotV = max(0, dot(half, view));
					float geomRatio = min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV) / (4 * nDotV);
					
					vec3 colorScaled = pow(rgbToXYZ(color.rgb / attenuatedLightIntensity), vec3(gammaInv));
					vec3 currentFit = prevDiffuseColor * nDotL + prevSpecularColor * mfdEval * geomRatio;
					vec3 colorResidual = /*colorScaled*/ - pow(currentFit, vec3(gammaInv));
					
					vec3 innerDeriv = gammaInv * pow(currentFit, vec3(gammaInv - 1));
					mat3 innerDerivMatrix = 
						mat3(vec3(innerDeriv.r, 0, 0),
							vec3(0, innerDeriv.g, 0),
							vec3(0, 0, innerDeriv.b));
					mat3 diffuseDerivs = nDotL * innerDerivMatrix;
					mat3 specularReflectivityDerivs = mfdEval * geomRatio * innerDerivMatrix;
					mat4x3 specularDerivs = mat4x3(
						specularReflectivityDerivs[0],
						specularReflectivityDerivs[1],
						specularReflectivityDerivs[2],
						geomRatio * mfdDeriv * prevSpecularColor * innerDeriv);
						
					mat3 diffuseDerivs2 = nDotL * innerDerivMatrix;
					mat3 diffuseDerivsTranspose = transpose(diffuseDerivs2);
						
					mA += diffuseDerivsTranspose * diffuseDerivs;
					mB += transpose(diffuseDerivs) * specularDerivs;
					mC += transpose(specularDerivs) * diffuseDerivs;
					mD += transpose(specularDerivs) * specularDerivs;
					
					v1 += transpose(diffuseDerivs) * colorResidual;
					v2 += transpose(specularDerivs) * colorResidual;
				}
			}
		}
		
		mat3 mAInverse = inverse(mA);
		mat4 schurInverse = inverse(mD - mC * mAInverse * mB);
		
		vec3 diffuseAdj = (mAInverse + mAInverse * mB * schurInverse * mC * mAInverse) * v1 
			- mAInverse * mB * schurInverse * v2;
		
		vec4 specularAdj = -schurInverse * mC * mAInverse * v1 + schurInverse * v2;
		
		vec3 diffuseAdjLinearized = 
			diffuseAdj * pow(prevDiffuseColor, vec3(gammaInv - 1.0)) * gammaInv;
		vec4 specularAdjLinearized = 
			specularAdj * vec4(pow(prevSpecularColor, vec3(gammaInv - 1.0)) * gammaInv, 1.0);
			
		float scale = SHIFT_FRACTION / 
			sqrt((dot(diffuseAdjLinearized, diffuseAdjLinearized)
				+ dot(specularAdjLinearized, specularAdjLinearized)));
		
		return ParameterizedFit(
			0.5 * normalize(mA[0]) + vec3(0.5),//prevDiffuseColor + scale * diffuseAdj, 
			diffuseNormal,
			prevSpecularColor + scale * specularAdj.rgb,
			prevRoughness + scale * specularAdj.a);
	}
}

#endif // ADJUSTMENT_GLSL
