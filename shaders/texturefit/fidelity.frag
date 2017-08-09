#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

#include "../colorappearance/colorappearance.glsl"
#include "../colorappearance/imgspace.glsl"

#line 13 0

// uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D specularEstimate;
uniform sampler2D roughnessEstimate;

uniform mat4 model_view;
uniform int targetViewIndex;
uniform float fittingGamma;
uniform bool evaluateInXYZ;

uniform bool useMaskTexture;
uniform sampler2D maskTexture;

layout(location = 0) out vec2 fidelity;

vec3 getDiffuseColor()
{
	return vec3(0);
    // return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));
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

vec2 computeFidelity()
{
	vec3 normal = normalize(fNormal);
	vec3 tangent = normalize(fTangent - dot(normal, fTangent));
	vec3 bitangent = normalize(fBitangent
		- dot(normal, fBitangent) * normal 
		- dot(tangent, fBitangent) * tangent);
		
	mat3 tangentToObject = mat3(tangent, bitangent, normal);
	vec3 shadingNormal = tangentToObject * getDiffuseNormalVector();
	
	vec3 diffuseColor, specularColor;
	
	if (evaluateInXYZ)
	{
		diffuseColor = rgbToXYZ(getDiffuseColor());
		specularColor = rgbToXYZ(getSpecularColor());
	}
	else
	{
		diffuseColor = getDiffuseColor();
		specularColor = getSpecularColor();
	}
	
	float maxLuminance = getMaxLuminance();
	float roughness = getRoughness();
	float roughnessSquared = roughness * roughness;
	float fittingGammaInv = 1.0 / fittingGamma;
	
	vec3 view = normalize(getViewVector(targetViewIndex));
	float nDotV = max(0, dot(shadingNormal, view));
	vec4 color = getLinearColor(targetViewIndex);
	
	if (color.a > 0 && dot(normal, view) > 0)
	{
		vec3 lightPreNormalized = getLightVector(targetViewIndex);
		vec3 attenuatedLightIntensity = infiniteLightSources ? 
			getLightIntensity(targetViewIndex) : 
			getLightIntensity(targetViewIndex) / (dot(lightPreNormalized, lightPreNormalized));
		vec3 light = normalize(lightPreNormalized);
		float nDotL = max(0, dot(light, shadingNormal));
		
		vec3 half = normalize(view + light);
		float nDotH = dot(half, shadingNormal);
		
		vec3 colorScaled;
		if (evaluateInXYZ)
		{
			colorScaled = pow(rgbToXYZ(color.rgb / attenuatedLightIntensity), vec3(fittingGammaInv));
		}
		else
		{
			colorScaled = pow(color.rgb / attenuatedLightIntensity, vec3(fittingGammaInv));
		}
		
		if (nDotV > 0 && nDotL > 0.0)
		{
			float nDotHSquared = nDotH * nDotH;
			
			float q1 = roughnessSquared * nDotHSquared + (1.0 - nDotHSquared);
			float mfdEval = roughnessSquared / (q1 * q1);
			
			float hDotV = max(0, dot(half, view));
			float geomRatio = min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV) / (4 * nDotV);
			
			vec3 currentFit = diffuseColor * nDotL + specularColor * mfdEval * geomRatio;
			vec3 colorResidual = colorScaled - 
				pow(min(maxLuminance / attenuatedLightIntensity, currentFit), vec3(fittingGammaInv));
			
			float luminanceResidual =  
				pow(getLuminance(color.rgb / attenuatedLightIntensity), fittingGammaInv)
					- pow(min(getLuminance(maxLuminance / attenuatedLightIntensity), 
						getLuminance(currentFit)), fittingGammaInv);
			
			//return nDotV * color.a * vec2(dot(colorResidual, colorResidual), 1);
			return nDotV * color.a * vec2(luminanceResidual * luminanceResidual, 1);
		}
		else
		{
			//return nDotV * color.a * vec2(dot(colorScaled, colorScaled), 1);
			
			float luminance = getLuminance(color.rgb / attenuatedLightIntensity);
			return nDotV * color.a * vec2(luminance * luminance, 1);
		}
	}
	else
	{
		return vec2(0);
	}
}

void main()
{
	if (useMaskTexture && texture(maskTexture, fTexCoord)[0] < 1.0)
	{
		discard;
	}
	else
	{
		fidelity = computeFidelity();
	}
}
