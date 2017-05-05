#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#include "../reflectance/reflectance.glsl"

#line 7 2004

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D roughnessEstimate;

uniform float fittingGamma;

vec3 getDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
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

vec4 removeDiffuse(vec4 originalColor, vec3 diffuseColor, vec3 light, 
					vec3 attenuatedLightIntensity, vec3 normal)
{
    vec3 diffuseContrib = diffuseColor * max(0, dot(light, normal)) * attenuatedLightIntensity;
    float cap = 1.0 - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
    vec3 remainder = clamp(originalColor.rgb - diffuseContrib, 0, cap);
    return vec4(remainder, 
		originalColor.a * pow(remainder.r * remainder.g * remainder.b, 1.0 / (3 * fittingGamma)));
}

ParameterizedFit fitSpecular()
{
    vec3 normal = normalize(fNormal);
		
	vec3 tangent = normalize(fTangent - dot(normal, fTangent));
	vec3 bitangent = normalize(fBitangent
		- dot(normal, fBitangent) * normal 
		- dot(tangent, fBitangent) * tangent);
		
	mat3 tangentToObject = mat3(tangent, bitangent, normal);
	vec3 shadingNormalTS = getDiffuseNormalVector();
	vec3 shadingNormal = tangentToObject * shadingNormalTS;
		
	vec3 diffuseColor = getDiffuseColor();
		
    float maxLuminance = getMaxLuminance();
	vec2 maxResidual = vec2(0);
	vec3 chromaticitySum = vec3(0);
	
	vec3 roughnessSums = vec3(0);
    
    for (int i = 0; i < viewCount; i++)
    {
        vec3 view = normalize(getViewVector(i));
        
        // Values of 1.0 for this color would correspond to the expected reflectance
        // for an ideal diffuse reflector (diffuse albedo of 1), which is a reflectance of 1 / pi.
        // Hence, this color corresponds to the reflectance times pi.
        // Both the Phong model and the Cook Torrance with a Beckmann distribution also have a 1/pi factor.
		// By adopting the convention that all reflectance values are scaled by pi in this shader,
		// We can avoid division by pi here as well as the 1/pi factors in the parameterized models.
        vec4 color = getLinearColor(i);
        
        if (color.a * dot(view, normal) > 0)
        {
            vec3 lightPreNormalized = getLightVector(i);
            vec3 attenuatedLightIntensity = infiniteLightSources ? 
                getLightIntensity(i) : 
                getLightIntensity(i) / (dot(lightPreNormalized, lightPreNormalized));
            vec3 light = normalize(lightPreNormalized);
			float nDotL = max(0, dot(light, shadingNormal));
			float nDotV = max(0, dot(shadingNormal, view));
            
            vec3 half = normalize(view + light);
            float nDotH = dot(half, shadingNormal);
			float nDotHSquared = nDotH * nDotH;
			
			vec4 colorRemainder = 
				removeDiffuse(color, diffuseColor, light, attenuatedLightIntensity, normal);
            
            if (nDotV > 0 && nDotHSquared > 0.5)
            {
				float hDotV = max(0, dot(half, view));
				
				vec3 colorXYZ = 
					pow(rgbToXYZ(colorRemainder.rgb / attenuatedLightIntensity), vec3(1.0 / fittingGamma));
				
				float luminance = getLuminance(colorRemainder.rgb / attenuatedLightIntensity);
				
				if (colorXYZ.y * nDotH > maxResidual[0] * maxResidual[1])
				{
					maxResidual = vec2(colorXYZ.y, nDotH);
				}
				
				chromaticitySum += vec3(
					colorXYZ.y,
					5 * (colorXYZ.x - colorXYZ.y),
					2 * (colorXYZ.y - colorXYZ.z));
					
				if (nDotV * (1 + nDotHSquared) * (1 + nDotHSquared) > 1.0)
				{
					roughnessSums += pow(luminance * nDotV, 1.0 / fittingGamma) * 
						vec3(sqrt(luminance * nDotV) * vec2(1 - nDotHSquared, nDotHSquared), 1);
				}
            }
        }
    }
	
	vec3 specularPeakLab = maxResidual[0] * chromaticitySum / chromaticitySum[0];
	
	float roughnessSquared = min(1.0, roughnessSums[0] / 
			(sqrt(pow(maxResidual[0], fittingGamma)) * roughnessSums[2] - roughnessSums[1]));
	
	vec3 specularColor = 
		xyzToRGB(pow(clamp(4 * roughnessSquared * vec3(
			specularPeakLab[0] + 0.2 * specularPeakLab[1], 
			specularPeakLab[0],
			specularPeakLab[0] - 0.5 * specularPeakLab[2]
		), 0.0, 1.0), vec3(fittingGamma)));
		
	vec3 adjustedDiffuseColor = diffuseColor - specularColor * roughnessSquared / 2;
	
    // Dividing by the sum of weights to get the weighted average.
    // We'll put a lower cap of 1/m^2 on the alpha we divide by so that noise doesn't get amplified
    // for texels where there isn't enough information at the specular peak.
    return ParameterizedFit(adjustedDiffuseColor, shadingNormalTS, specularColor, sqrt(roughnessSquared));
}

#endif // SPECULARFIT_GLSL
