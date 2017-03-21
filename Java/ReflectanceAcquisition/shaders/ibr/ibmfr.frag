#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;

#include "../reflectance/reflectance.glsl"
#include "../reflectance/imgspace.glsl"

#line 15 0

uniform mat4 model_view;
uniform mat4 envMapMatrix;
in vec3 fViewPos;

uniform bool skipViewEnabled;
uniform int skipView;

#define MAX_VIRTUAL_LIGHT_COUNT 4
uniform vec3 lightIntensityVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform vec3 lightPosVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform mat4 lightMatrixVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform int virtualLightCount;
uniform vec3 ambientColor;

uniform float weightExponent;

uniform sampler2D diffuseMap;
uniform sampler2D normalMap;
uniform sampler2D specularMap;
uniform sampler2D roughnessMap;
uniform sampler2D environmentMap;
uniform int environmentMipMapLevel;
uniform int diffuseEnvironmentMipMapLevel;
uniform float environmentMapGamma;
uniform sampler1D mfdMap;

uniform bool useDiffuseTexture;
uniform bool useNormalTexture;
uniform bool useSpecularTexture;
uniform bool useRoughnessTexture;
uniform bool useEnvironmentTexture;
uniform bool useMFDTexture;

uniform bool useInverseLuminanceMap;
uniform sampler1D inverseLuminanceMap;

uniform sampler2DArray shadowMaps;

uniform bool useTSOverrides;
uniform vec3 lightDirTSOverride;
uniform vec3 viewDirTSOverride;

uniform bool imageBasedRenderingEnabled;
uniform bool relightingEnabled;
uniform bool pbrGeometricAttenuationEnabled;
uniform bool fresnelEnabled;

vec3 getEnvironmentFresnel(vec3 lightDirection, float fresnelFactor)
{
	vec2 texCoords = vec2(atan(lightDirection.x, -lightDirection.z) / 2, asin(lightDirection.y))
						/ PI + vec2(0.5);
	
	// // To prevent seams when the texture wraps around
	// vec4 color1 = texture(environmentMap, texCoords);
	// vec4 color2 = texture(environmentMap, 
		// mod(texCoords + vec2(0.5, 0.0), 1.0) - vec2(0.5, 0.0));
	// return pow(mix(color1, color2, 2.0 * abs(texCoords.x - 0.5)).rgb, vec3(environmentMapGamma));
	
	return pow(textureLod(environmentMap, texCoords, mix(environmentMipMapLevel, 0, fresnelFactor)).rgb, 
		vec3(environmentMapGamma));
}

vec3 getEnvironment(vec3 lightDirection)
{
	vec2 texCoords = vec2(atan(lightDirection.x, -lightDirection.z) / 2, asin(lightDirection.y))
						/ PI + vec2(0.5);
	
	return pow(textureLod(environmentMap, texCoords, environmentMipMapLevel).rgb, 
		vec3(environmentMapGamma));
}

vec3 getEnvironmentDiffuse(vec3 normalDirection)
{
	vec2 texCoords = vec2(atan(normalDirection.x, -normalDirection.z) / 2, asin(normalDirection.y))
						/ PI + vec2(0.5);
	return pow(textureLod(environmentMap, texCoords, diffuseEnvironmentMipMapLevel).rgb, 
		vec3(environmentMapGamma));
}

vec3 computeFresnelReflectivityActual(vec3 specularColor, vec3 grazingColor, float hDotV)
{
    float maxLuminance = dot(grazingColor, vec3(0.2126, 0.7152, 0.0722));
    float f0 = clamp(dot(specularColor, vec3(0.2126, 0.7152, 0.0722)) / maxLuminance, 0.001, 0.999);
    float sqrtF0 = sqrt(f0);
    float ior = (1 + sqrtF0) / (1 - sqrtF0);
    float g = sqrt(ior*ior + hDotV * hDotV - 1);
    float fresnel = 0.5 * pow(g - hDotV, 2) / pow(g + hDotV, 2)
        * (1 + pow(hDotV * (g + hDotV) - 1, 2) / pow(hDotV * (g - hDotV) + 1, 2));
        
    return specularColor + (grazingColor - specularColor) * max(0, fresnel - f0) / (1.0 - f0);
}

vec3 computeFresnelReflectivitySchlick(vec3 specularColor, vec3 grazingColor, float hDotV)
{
    return specularColor + (grazingColor - specularColor) * pow(max(0.0, 1.0 - hDotV), 5.0);
}

vec3 fresnel(vec3 specularColor, vec3 grazingColor, float hDotV)
{
	//return specularColor;
    //return computeFresnelReflectivityActual(specularColor, grazingColor, hDotV);
    return computeFresnelReflectivitySchlick(specularColor, grazingColor, hDotV);
}

float computeGeometricAttenuationVCavity(
    float roughness, float nDotH, float nDotV, float nDotL, float hDotV, float hDotL)
{
    return min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV);
}

float computeGeometricAttenuationSmithBeckmann(float roughness, float nDotV, float nDotL)
{
    float aV = 1.0 / (roughness * sqrt(1.0 - nDotV * nDotV) / nDotV);
    float aVSq = aV * aV;
    float aL = 1.0 / (roughness * sqrt(1.0 - nDotL * nDotL) / nDotL);
    float aLSq = aL * aL;
        
    return (aV < 1.6 ? (3.535 * aV + 2.181 * aVSq) / (1 + 2.276 * aV + 2.577 * aVSq) : 1.0)
            * (aL < 1.6 ? (3.535 * aL + 2.181 * aLSq) / (1 + 2.276 * aL + 2.577 * aLSq) : 1.0);
        // ^ See Walter et al. "Microfacet Models for Refraction through Rough Surfaces"
        // for this formula
}

float computeGeometricAttenuationSmithGGX(float roughness, float nDotV, float nDotL)
{
	float roughnessSq = roughness * roughness;
	return 4 / (1 + sqrt(1 + roughnessSq * (1 / (nDotV * nDotV) - 1.0)))
			 / (1 + sqrt(1 + roughnessSq * (1 / (nDotL * nDotL) - 1.0)));
}

float geom(float roughness, float nDotH, float nDotV, float nDotL, float hDotV, float hDotL)
{
    //return nDotV * nDotL;
    return computeGeometricAttenuationVCavity(roughness, nDotH, nDotV, nDotL, hDotV, hDotL);
    //return computeGeometricAttenuationSmithBeckmann(roughness, nDotV, nDotL);
	//return computeGeometricAttenuationSmithGGX(roughness, nDotV, nDotL);
}

float computeMicrofacetDistributionGGX(float nDotH, float roughness)
{
	float roughnessSquared = roughness * roughness;
	float nDotHSquared = nDotH * nDotH;
	float q = roughnessSquared + (1 - nDotHSquared) / nDotHSquared;

	// Assume scaling by pi
	return roughnessSquared / (nDotHSquared * nDotHSquared * q * q);
}

float computeMicrofacetDistributionBeckmann(float nDotH, float roughness)
{
    float nDotHSquared = nDotH * nDotH;
    float roughnessSquared = roughness * roughness;
    
	// Assume scaling by pi
    return exp((nDotHSquared - 1.0) / (nDotHSquared * roughnessSquared)) 
            / (nDotHSquared * nDotHSquared * roughnessSquared);
}

float computeMicrofacetDistributionPhong(float nDotH, float roughness)
{
    float nDotHSquared = nDotH * nDotH;
    float roughnessSquared = roughness * roughness;
    
	// Assume scaling by pi
    return max(0.0, pow(nDotH, 2 / 2/roughnessSquared - 2) / (roughnessSquared));
}

float dist(float nDotH, float roughness)
{
	//return texture(mfdMap, nDotH).r / 0.1822322092566941;
	return computeMicrofacetDistributionGGX(nDotH, roughness);
    //return computeMicrofacetDistributionBeckmann(nDotH, roughness);
    //return computeMicrofacetDistributionPhong(nDotH, roughness);
}

float computeSampleWeight(vec3 targetDir, vec3 sampleDir)
{
	return 1.0 / max(0.000001, 1.0 - pow(max(0.0, dot(targetDir, sampleDir)), weightExponent)) - 1.0;
}

vec4 removeDiffuse(vec4 originalColor, vec3 diffuseContrib, float nDotL, float maxLuminance)
{
    if (nDotL == 0.0)
    {
        return vec4(0);
    }
    else
    {
        float cap = maxLuminance - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
        vec3 remainder = clamp(originalColor.rgb - diffuseContrib, 0, cap);
        return vec4(remainder, originalColor.a);
    }
}

vec4 computeEnvironmentSample(int index, vec3 diffuseColor, vec3 normalDir, 
    vec3 specularColor, float roughness, float maxLuminance)
{
	vec3 fragmentPos = (cameraPoses[index] * vec4(fPosition, 1.0)).xyz;
	vec3 normalDirCameraSpace = normalize((cameraPoses[index] * vec4(normalDir, 0.0)).xyz);
	vec3 sampleViewDir = normalize(-fragmentPos);
	float nDotV_sample = max(0, dot(normalDirCameraSpace, sampleViewDir));
	
	if (nDotV_sample <= 0.0)
	{
		return vec4(0.0, 0.0, 0.0, 0.0);
	}
	else
	{
		vec4 sampleColor = getLinearColor(index);
		if (sampleColor.a > 0.0)
		{
			// All in camera space
			vec3 sampleLightDirUnnorm = lightPositions[getLightIndex(index)].xyz - fragmentPos;
			float lightDistSquared = dot(sampleLightDirUnnorm, sampleLightDirUnnorm);
			vec3 sampleLightDir = sampleLightDirUnnorm * inversesqrt(lightDistSquared);
			vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
			vec3 lightIntensity = getLightIntensity(index);
			
			float nDotL_sample = max(0, dot(normalDirCameraSpace, sampleLightDir));
			float nDotH = max(0, dot(normalDirCameraSpace, sampleHalfDir));
			float hDotV_sample = max(0, dot(sampleHalfDir, sampleViewDir));
			float hDotL_sample = max(0, dot(sampleHalfDir, sampleLightDir));
		
			vec3 diffuseContrib = diffuseColor * nDotL_sample * lightIntensity / lightDistSquared;
			
			float geomAttenSample = geom(roughness, nDotH, nDotV_sample, nDotL_sample, 
				hDotV_sample, hDotL_sample);
			
			if (nDotV_sample > 0.0 && geomAttenSample > 0.0)
			{
				vec3 virtualViewDir = 
					normalize((cameraPoses[index] * vec4(fViewPos, 1.0)).xyz - fragmentPos);
				vec3 virtualLightDir = -reflect(virtualViewDir, sampleHalfDir);
				float nDotL_virtual = max(0, dot(normalDirCameraSpace, virtualLightDir));
				float nDotV_virtual = max(0, dot(normalDirCameraSpace, virtualViewDir));
				float hDotV_virtual = max(0, dot(sampleHalfDir, virtualViewDir));
				float hDotL_virtual = max(0, dot(sampleHalfDir, virtualLightDir));
			
				float geomAttenVirtual = geom(roughness, nDotH, nDotV_virtual, nDotL_virtual, 
					hDotV_virtual, hDotL_virtual);
			
				vec4 specularResid = removeDiffuse(sampleColor, diffuseContrib, nDotL_sample, maxLuminance);
				
				vec3 mfd = specularResid.rgb * 4 * nDotV_sample * lightDistSquared / lightIntensity;
				// vec3 mfd = specularResid.rgb * lightDistSquared / lightIntensity;
				
				
				float mfd_mono = getLuminance(mfd / specularColor);
				
				return vec4(
					mfd
					//fresnel(mfd, vec3(mfd_mono), hDotV_virtual)
				/*	* geomAttenVirtual */
					* getEnvironment(mat3(envMapMatrix) * transpose(mat3(cameraPoses[index]))
										* virtualLightDir),
					mfd_mono /* * geomAttenSample */);
			}
			else
			{
				return vec4(0.0, 0.0, 0.0, 0.0);
			}
		}
		else
		{
		   return vec4(0.0, 0.0, 0.0, 0.0);
		}
	}
}

vec3 getEnvironmentShading(vec3 diffuseColor, vec3 normalDir, vec3 specularColor, float roughness)
{
	float maxLuminance = getMaxLuminance();

	vec4 sum = vec4(0.0);
    
	for (int i = 0; i < viewCount; i++)
	{
        sum += computeEnvironmentSample(i, diffuseColor, normalDir, specularColor, roughness, maxLuminance);
	}
    
    if (sum.y > 0.0)
	{
		return sum.rgb / clamp(sum.a, 1.0, 1000000.0);
	}
	else
	{
		return vec3(0.0);
	}
}

vec4[MAX_VIRTUAL_LIGHT_COUNT] computeSample(int index, vec3 diffuseColor, vec3 normalDir, 
    vec3 specularColor, float roughness, float maxLuminance)
{
    vec4 sampleColor = getLinearColor(index);
    if (sampleColor.a > 0.0)
    {
        vec4 result[MAX_VIRTUAL_LIGHT_COUNT];
        
        // All in camera space
        vec3 fragmentPos = (cameraPoses[index] * vec4(fPosition, 1.0)).xyz;
        vec3 sampleViewDir = normalize(-fragmentPos);
        vec3 sampleLightDirUnnorm = lightPositions[getLightIndex(index)].xyz - fragmentPos;
        float lightDistSquared = dot(sampleLightDirUnnorm, sampleLightDirUnnorm);
        vec3 sampleLightDir = sampleLightDirUnnorm * inversesqrt(lightDistSquared);
        vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
        vec3 normalDirCameraSpace = normalize((cameraPoses[index] * vec4(normalDir, 0.0)).xyz);
        vec3 lightIntensity = getLightIntensity(index);
        
        float nDotL = max(0, dot(normalDirCameraSpace, sampleLightDir));
        float nDotV = max(0, dot(normalDirCameraSpace, sampleViewDir));
        float nDotH = max(0, dot(normalDirCameraSpace, sampleHalfDir));
        float hDotV = max(0, dot(sampleHalfDir, sampleViewDir));
        float hDotL = max(0, dot(sampleHalfDir, sampleLightDir));
    
        vec3 diffuseContrib = diffuseColor * nDotL * lightIntensity / lightDistSquared;
        
        float geomAtten = geom(roughness, nDotH, nDotV, nDotL, hDotV, hDotL);
        if (!relightingEnabled || geomAtten > 0.0)
        {
            vec4 specularResid = removeDiffuse(sampleColor, diffuseContrib, nDotL, maxLuminance);
            vec4 precomputedSample;

			if (relightingEnabled)
			{
				if(pbrGeometricAttenuationEnabled)
				{
					precomputedSample = vec4(specularResid.rgb 
						* 4 * nDotV * lightDistSquared / lightIntensity, 
						sampleColor.a * geomAtten);
				}
				else
				{
					precomputedSample = 
						vec4(specularResid.rgb * lightDistSquared / lightIntensity, sampleColor.a * nDotL);
				}
			}
			else
			{
				precomputedSample = 
					vec4(specularResid.rgb, sampleColor.a);
			}
            
			mat3 tangentToObject = mat3(0.0);
			
			vec3 virtualViewDir;
			if (useTSOverrides)
			{
				vec3 gNormal = normalize(fNormal);
				vec3 tangent = normalize(fTangent - dot(gNormal, fTangent));
				vec3 bitangent = normalize(fBitangent
					- dot(gNormal, fBitangent) * gNormal 
					- dot(tangent, fBitangent) * tangent);
				tangentToObject = mat3(tangent, bitangent, gNormal);
				
				virtualViewDir = normalize(mat3(cameraPoses[index]) * tangentToObject * viewDirTSOverride);
			}
			else
			{
				virtualViewDir = normalize((cameraPoses[index] * vec4(fViewPos, 1.0)).xyz - fragmentPos);
			}
            
            for (int lightPass = 0; lightPass < MAX_VIRTUAL_LIGHT_COUNT; lightPass++)
            {
                vec3 virtualLightDir;
				if (useTSOverrides)
				{
					virtualLightDir = 
						normalize(mat3(cameraPoses[index]) * tangentToObject * lightDirTSOverride);
				}
				else if (relightingEnabled)
				{
					virtualLightDir = normalize((cameraPoses[index] * 
						vec4(lightPosVirtual[lightPass], 1.0)).xyz - fragmentPos);
				}
				else
				{
					virtualLightDir = virtualViewDir;
				}
				
                vec3 virtualHalfDir = normalize(virtualViewDir + virtualLightDir);

                // Compute sample weight
                float weight = computeSampleWeight(virtualHalfDir, sampleHalfDir);
				
				
                // float weight = computeSampleWeight(
					// normalize(vec3(1,1,10) * (transpose(tangentToObject) * virtualHalfDir)), 
					// normalize(vec3(1,1,10) * (transpose(tangentToObject) * sampleHalfDir)));
				
				// float virtualNDotH = dot(normalDirCameraSpace, virtualHalfDir);
				// float sampleNDotH = dot(normalDirCameraSpace, sampleHalfDir);
				// float weight = computeSampleWeight(
					// vec3(virtualNDotH, sqrt(1.0 - virtualNDotH * virtualNDotH), 0.0),
					// vec3(sampleNDotH, sqrt(1.0 - sampleNDotH * sampleNDotH), 0.0));
				// float weight = 1.0 / abs(virtualNDotH - sampleNDotH);
                result[lightPass] = weight * precomputedSample;
            }
        }
        
        return result;
    }
    else
    {
        vec4 result[MAX_VIRTUAL_LIGHT_COUNT];
        for (int lightPass = 0; lightPass < MAX_VIRTUAL_LIGHT_COUNT; lightPass++)
        {
            result[lightPass] = vec4(0.0);
        }
        return result;
    }
}

vec4[MAX_VIRTUAL_LIGHT_COUNT] computeWeightedAverages(
    vec3 diffuseColor, vec3 normalDir, vec3 specularColor, float roughness)
{
    float maxLuminance = getMaxLuminance();

	vec4[MAX_VIRTUAL_LIGHT_COUNT] sums;
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT; i++)
    {
        sums[i] = vec4(0.0);
    }
    
	for (int i = 0; i < viewCount; i++)
	{
		if (!skipViewEnabled || i != skipView)
		{
			vec4[MAX_VIRTUAL_LIGHT_COUNT] microfacetSample = 
				computeSample(i, diffuseColor, normalDir, specularColor, roughness, maxLuminance);
			
			for (int j = 0; j < MAX_VIRTUAL_LIGHT_COUNT; j++)
			{
				sums[j] += microfacetSample[j];
			}
		}
	}
    
    vec4[MAX_VIRTUAL_LIGHT_COUNT] results;
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT; i++)
    {
        if (sums[i].y > 0.0)
        {
            results[i] = vec4(sums[i].rgb / max(0.01, sums[i].a), sums[i].a);
        }
        else
        {
            results[i] = vec4(0.0);
        }
    }
	return results;
}

vec3 linearToSRGB(vec3 color)
{
	//return pow(color, vec3(1.0/2.2));

	vec3 sRGBColor;

	if(color.r <= 0.0031308)
	{
		sRGBColor.r = 12.92 * color.r;
	}
	else
	{
		sRGBColor.r = (1.055) * pow(color.r, 1.0/2.4) - 0.055;
	}

	if(color.g <= 0.0031308)
	{
		sRGBColor.g = 12.92 * color.g;
	}
	else
	{
		sRGBColor.g = (1.055) * pow(color.g, 1.0/2.4) - 0.055;
	}

	if(color.b <= 0.0031308)
	{
		sRGBColor.b = 12.92 * color.b;
	}
	else
	{
		sRGBColor.b = (1.055) * pow(color.b, 1.0/2.4) - 0.055;
	}
	
	return sRGBColor;
}

vec3 sRGBToLinear(vec3 sRGBColor)
{
	//return pow(sRGBColor, vec3(2.2));

	vec3 linearColor;
	
	if(sRGBColor.r <= 0.04045)
	{
		linearColor.r = sRGBColor.r / 12.92;
	}
	else
	{
		linearColor.r = pow((sRGBColor.r + 0.055) / 1.055, 2.4);
	}
	
	if(sRGBColor.g <= 0.04045)
	{
		linearColor.g = sRGBColor.g / 12.92;
	}
	else
	{
		linearColor.g = pow((sRGBColor.g + 0.055) / 1.055, 2.4);
	}
	
	if(sRGBColor.b <= 0.04045)
	{
		linearColor.b = sRGBColor.b / 12.92;
	}
	else
	{
		linearColor.b = pow((sRGBColor.b + 0.055) / 1.055, 2.4);
	}
	
	return linearColor;
}

vec4 tonemap(vec3 color, float alpha)
{
    // if (useInverseLuminanceMap)
    // {
        // if (color.r <= 0.000001 && color.g <= 0.000001 && color.b <= 0.000001)
        // {
            // return vec4(0.0, 0.0, 0.0, 1.0);
        // }
        // else
        // {
            // // Step 1: convert to CIE luminance
            // // Clamp to 1 so that the ratio computed in step 3 is well defined
            // // if the luminance value somehow exceeds 1.0
            // float luminance = getLuminance(color);
			// float maxLuminance = getMaxLuminance();
			// if (luminance >= maxLuminance)
			// {
				// return vec4(linearToSRGB(color / maxLuminance), alpha);
			// }
			// else
			// {
				// float scaledLuminance = min(1.0, luminance / maxLuminance);
				
				// // Step 2: determine the ratio between the tonemapped and linear luminance
				// // Remove implicit gamma correction from the lookup table
				// float tonemappedGammaCorrected = texture(inverseLuminanceMap, scaledLuminance).r;
				// float tonemappedNoGamma = sRGBToLinear(vec3(tonemappedGammaCorrected))[0];
				// float scale = tonemappedNoGamma / luminance;
					
				// // Step 3: return the color, scaled to have the correct luminance,
				// // but the original saturation and hue.
				// // Step 4: apply gamma correction
				// vec3 colorScaled = color * scale;
				// return vec4(linearToSRGB(colorScaled), alpha);
			// }
        // }
    // }
    // else
    {
        return vec4(linearToSRGB(color), alpha);
    }
}

void main()
{
	vec3 viewDir;
	if (useTSOverrides)
	{
		viewDir = viewDirTSOverride;
	}
	else
	{
		viewDir = normalize(fViewPos - fPosition);
	}
    
    vec3 normalDir;
    if (useNormalTexture)
    {
        vec3 normalDirTS = normalize(texture(normalMap, fTexCoord).xyz * 2 - vec3(1.0));
		
		vec3 gNormal = normalize(fNormal);
        vec3 tangent = normalize(fTangent - dot(gNormal, fTangent));
        vec3 bitangent = normalize(fBitangent
            - dot(gNormal, fBitangent) * gNormal 
            - dot(tangent, fBitangent) * tangent);
        mat3 tangentToObject = mat3(tangent, bitangent, gNormal);
		
		normalDir = tangentToObject * normalDirTS;
		//normalDir = gNormal;
		//normalDir = normalDirTS;
    }
    else
    {
        normalDir = normalize(fNormal);
    }
    
    vec3 diffuseColor;
    if (useDiffuseTexture)
    {
        diffuseColor = pow(texture(diffuseMap, fTexCoord).rgb, vec3(gamma));
    }
    else
    {
        diffuseColor = vec3(0.0);
    }
    
    vec3 specularColor;
    if (useSpecularTexture)
    {
        specularColor = pow(texture(specularMap, fTexCoord).rgb, vec3(gamma));
    }
    else
    {
		specularColor = vec3(0.5); // TODO pass in a default?
    }
    
    float roughness;
    if (useRoughnessTexture)
    {
        roughness = texture(roughnessMap, fTexCoord).r;
    }
    else
    {
        roughness = 0.25; // TODO pass in a default?
    }
    
    float nDotV = useTSOverrides ? viewDir.z : dot(normalDir, viewDir);
    vec3 reflectance;

	vec4[MAX_VIRTUAL_LIGHT_COUNT] weightedAverages;

	if (imageBasedRenderingEnabled)
	{
		weightedAverages = computeWeightedAverages(diffuseColor, normalDir, specularColor, roughness);
	}
	
	if (relightingEnabled)
	{
		if (useEnvironmentTexture)
		{
			reflectance = diffuseColor * getEnvironmentDiffuse((envMapMatrix * vec4(normalDir, 0.0)).xyz);
			
			if (imageBasedRenderingEnabled)
			{
				if (fresnelEnabled)
				{
					reflectance += ambientColor * 
						fresnel(getEnvironmentShading(diffuseColor, normalDir, specularColor, roughness),
							getEnvironmentFresnel(
								(envMapMatrix * vec4(-reflect(viewDir, normalDir), 0.0)).xyz, 
									pow(1 - nDotV, 5)), nDotV);
				}
				else
				{
					reflectance += ambientColor * 
						getEnvironmentShading(diffuseColor, normalDir, specularColor, roughness);
				}
			}
		
			// For debugging environment mapping:
			//reflectance = getEnvironment((envMapMatrix * vec4(-reflect(viewDir, normalDir), 0.0)).xyz);
			//reflectance = getEnvironmentDiffuse((envMapMatrix * vec4(normalDir, 0.0)).xyz);
		}
		else
		{
			if (fresnelEnabled)
			{
				reflectance = fresnel(ambientColor * (diffuseColor + specularColor), ambientColor, nDotV);
			}
			else
			{
				reflectance = ambientColor;
			}
		}
	}
    
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT && i < (relightingEnabled ? virtualLightCount : 1); i++)
    {
        vec3 lightDirUnNorm;
        vec3 lightDir;
		float nDotL;
		if (useTSOverrides)
		{
			lightDirUnNorm = lightDir = lightDirTSOverride;
			nDotL = max(0.0, lightDir.z);
		}
		else if (relightingEnabled)
		{
			lightDirUnNorm = lightPosVirtual[i] - fPosition;
			lightDir = normalize(lightDirUnNorm);
			nDotL = max(0.0, dot(normalDir, lightDir));
		}
		else
		{
			lightDirUnNorm = lightDir = viewDir;
			nDotL = max(0.0, dot(normalDir, viewDir));
		}
        
        if (nDotL > 0.0)
        {
			bool shadow = false;
			if (!useTSOverrides && relightingEnabled)
			{
				vec4 projTexCoord = lightMatrixVirtual[i] * vec4(fPosition, 1.0);
				projTexCoord /= projTexCoord.w;
				projTexCoord = (projTexCoord + vec4(1)) / 2;
				shadow = !(projTexCoord.x >= 0 && projTexCoord.x <= 1 
					&& projTexCoord.y >= 0 && projTexCoord.y <= 1 
					&& projTexCoord.z >= 0 && projTexCoord.z <= 1
					&& texture(shadowMaps, vec3(projTexCoord.xy, i)).r - projTexCoord.z >= -0.01);
			}
            
            if (!shadow)
            {
                vec3 halfDir = normalize(viewDir + lightDir);
                float hDotV = dot(halfDir, viewDir);
                float hDotL = dot(halfDir, lightDir);
                float nDotH = useTSOverrides ? halfDir.z : dot(normalDir, halfDir);
                
                vec3 mfdFresnel;
				
				if (relightingEnabled && fresnelEnabled)
				{
					if (imageBasedRenderingEnabled)
					{
						float grazingIntensity = getLuminance(weightedAverages[i].rgb 
							/ max(vec3(1 / weightedAverages[i].a), specularColor));
						
						if (grazingIntensity <= 0.0)
						{
							mfdFresnel = vec3(0,0,0);
						}
						else
						{
							mfdFresnel = max(vec3(0.0), 
								fresnel(weightedAverages[i].rgb, vec3(grazingIntensity), hDotV));
								// fresnel(weightedAverages[i].rgb, vec3(dist(nDotH, roughness)), hDotV));
						}
						
						// The following debug code is for visualizing differences between reference 
						// and fitted in perceptually linear color space
						
						// vec3 reference = max(vec3(0.0), 
							// fresnel(weightedAverages[i].rgb, vec3(dist(nDotH, roughness)), hDotV));
						// vec3 fitted = fresnel(specularColor, vec3(1.0), hDotV) 
							// * vec3(dist(nDotH, roughness));
						
						// vec3 referenceXYZ = rgbToXYZ(reference);
						// vec3 fittedXYZ = rgbToXYZ(fitted);
					
						// // Pseudo-LAB color space
						// vec3 referenceLAB = vec3(referenceXYZ.y, 
							// 5 * (referenceXYZ.x - referenceXYZ.y), 
							// 2 * (referenceXYZ.y - referenceXYZ.z));
						// vec3 fittedLAB = vec3(fittedXYZ.y, 
							// 5 * (fittedXYZ.x - fittedXYZ.y), 2 * (fittedXYZ.y - fittedXYZ.z));
						
						// vec3 resultLAB = ???
						// vec3 resultXYZ = vec3(resultLAB.x + 0.2 * resultLAB.y, 
							/// resultLAB.x, resultLAB.x - 0.5 * resultLAB.z);
						// vec3 resultRGB = xyzToRGB(resultXYZ);
					}
					else
					{
						mfdFresnel = 
							fresnel(specularColor, vec3(1.0), hDotV) * vec3(dist(nDotH, roughness));
					}
				}
				else
				{
					if (imageBasedRenderingEnabled)
					{
						mfdFresnel = max(vec3(0.0), weightedAverages[i].rgb);
					}
					else
					{
						mfdFresnel = specularColor * vec3(dist(nDotH, roughness));
					}
				}
				
				vec3 lightVectorTransformed = (model_view * vec4(lightDirUnNorm, 0.0)).xyz;
            
                reflectance += (nDotL * diffuseColor + //fresnel(nDotL * diffuseColor, vec3(0.0), nDotL) + 
                    mfdFresnel 
					* (relightingEnabled && pbrGeometricAttenuationEnabled ? 
						geom(roughness, nDotH, nDotV, nDotL, hDotV, hDotL) / (4 * nDotV) : 
							(relightingEnabled ? nDotL : 1.0)))
                    * (relightingEnabled ? (useTSOverrides ? lightIntensityVirtual[i] : 
							lightIntensityVirtual[i] / dot(lightVectorTransformed, lightVectorTransformed))
						: vec3(1.0));
            }
        }
    }
		
	fragColor = tonemap(vec3(reflectance), 1.0);
}
