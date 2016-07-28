#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec4 fragColor;

#include "../reflectance/reflectance.glsl"
#include "../reflectance/imgspace.glsl"

#line 13 0

uniform mat4 projection;
uniform mat4 model_view;
in vec3 fViewPos;

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
uniform sampler1D mfdMap;

uniform bool useDiffuseTexture;
uniform bool useNormalTexture;
uniform bool useSpecularTexture;
uniform bool useRoughnessTexture;
uniform bool useMFDTexture;

uniform bool useInverseLuminanceMap;
uniform sampler1D inverseLuminanceMap;

uniform sampler2DArray shadowMaps;

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

float computeGeometricAttenuationSmithBeckmann(
    float roughness, float nDotH, float nDotV, float nDotL, float hDotV, float hDotL)
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

float geom(float roughness, float nDotH, float nDotV, float nDotL, float hDotV, float hDotL)
{
    //return nDotV * nDotL;
    return computeGeometricAttenuationVCavity(roughness, nDotH, nDotV, nDotL, hDotV, hDotL);
    //return computeGeometricAttenuationSmithBeckmann(roughness, nDotH, nDotV, nDotL, hDotV, hDotL);
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
	return min(1000000, 1.0 / (1.0 - pow(max(0.0, dot(targetDir, sampleDir)), weightExponent)) - 1.0);
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
        if (geomAtten > 0.0)
        {
            vec4 specularResid = removeDiffuse(sampleColor, diffuseContrib, nDotL, maxLuminance);
            vec4 precomputedSample = vec4(specularResid.rgb 
                * 4 * nDotV * lightDistSquared / lightIntensity, 
                sampleColor.a * geomAtten);
            
            vec3 virtualViewDir = normalize((cameraPoses[index] * vec4(fViewPos, 1.0)).xyz - fragmentPos);
            
            for (int lightPass = 0; lightPass < MAX_VIRTUAL_LIGHT_COUNT; lightPass++)
            {
                vec3 virtualLightDir = normalize((cameraPoses[index] * 
                    vec4(lightPosVirtual[lightPass], 1.0)).xyz - fragmentPos);
                vec3 virtualHalfDir = normalize(virtualViewDir + virtualLightDir);

                // Compute sample weight
                float weight = computeSampleWeight(virtualHalfDir, sampleHalfDir);
				
				// float virtualNDotH = dot(normalDirCameraSpace, virtualHalfDir);
				// float sampleNDotH = dot(normalDirCameraSpace, sampleHalfDir);
				// float weight = computeSampleWeight(
					// vec3(virtualNDotH, sqrt(1.0 - virtualNDotH * virtualNDotH), 0.0),
					// vec3(sampleNDotH, sqrt(1.0 - sampleNDotH * sampleNDotH), 0.0));
				// float weight = 1.0 / abs(virtualNDotH - sampleNDotH);
                result[lightPass] = weight * precomputedSample ;
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

vec3[MAX_VIRTUAL_LIGHT_COUNT] computeWeightedAverages(
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
        vec4[MAX_VIRTUAL_LIGHT_COUNT] microfacetSample = 
            computeSample(i, diffuseColor, normalDir, specularColor, roughness, maxLuminance);
        
        for (int j = 0; j < MAX_VIRTUAL_LIGHT_COUNT; j++)
        {
            sums[j] += microfacetSample[j];
        }
	}
    
    vec3[MAX_VIRTUAL_LIGHT_COUNT] results;
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT; i++)
    {
        if (sums[i].y > 0.0)
        {
            results[i] = sums[i].rgb / max(0.01, sums[i].a);
        }
        else
        {
            results[i] = vec3(0.0);
        }
    }
	return results;
}

vec4 tonemap(vec3 color, float alpha)
{
    if (useInverseLuminanceMap)
    {
        if (color.r <= 0.0 && color.g <= 0.0 && color.b <= 0.0)
        {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
        else
        {
            // Step 1: convert to CIE luminance
            // Clamp to 1 so that the ratio computed in step 3 is well defined
            // if the luminance value somehow exceeds 1.0
            float luminance = getLuminance(color);
			float maxLuminance = getMaxLuminance();
			// if (luminance >= maxLuminance)
			{
				return vec4(pow(color / maxLuminance, vec3(1.0 / gamma)), alpha);
			}
			// else
			// {
				// float scaledLuminance = min(1.0, luminance / maxLuminance);
				
				// // Step 2: determine the ratio between the tonemapped and linear luminance
				// // Remove implicit gamma correction from the lookup table
				// float scale = pow(texture(inverseLuminanceMap, scaledLuminance).r, gamma) / luminance;
					
				// // Step 3: return the color, scaled to have the correct luminance,
				// // but the original saturation and hue.
				// // Step 4: apply gamma correction
				// return vec4(pow(color * scale, vec3(1.0 / gamma)), alpha);
			// }
        }
    }
    else
    {
        return vec4(pow(color, vec3(1.0 / gamma)), alpha);
    }
}

void main()
{
    vec3 viewDir = normalize(fViewPos - fPosition);
    
    vec3 normalDir;
    if (useNormalTexture)
    {
        normalDir = normalize(texture(normalMap, fTexCoord).xyz * 2 - vec3(1.0));
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
		specularColor = vec3(0.5);
        //specularColor = vec3(0.040504154647770796); // TODO pass in a default?
    }
    
    // if (dot(specularColor, vec3(1)) < 0.01)
    // {
        // specularColor = vec3(0.5);
    // }
    
    float roughness;
    if (useRoughnessTexture)
    {
        roughness = texture(roughnessMap, fTexCoord).r;
    }
    else
    {
        roughness = 0.1; // TODO pass in a default?
    }
    
    vec3[] weightedAverages = computeWeightedAverages(diffuseColor, normalDir, specularColor, roughness);
	
	vec3 ambient = vec3(0.0); // TODO make this an input variable
    float nDotV = dot(normalDir, viewDir);
    vec3 reflectance = fresnel(ambient * diffuseColor, ambient, nDotV);
    
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT && i < virtualLightCount; i++)
    {
        vec3 lightDirUnNorm = lightPosVirtual[i] - fPosition;
        vec3 lightDir = normalize(lightDirUnNorm);
        float nDotL = max(0.0, dot(normalDir, lightDir));
        
        if (nDotL > 0.0)
        {
            vec4 projTexCoord = lightMatrixVirtual[i] * vec4(fPosition, 1.0);
            projTexCoord /= projTexCoord.w;
            projTexCoord = (projTexCoord + vec4(1)) / 2;
            
            if (projTexCoord.x >= 0 && projTexCoord.x <= 1 && projTexCoord.y >= 0 && projTexCoord.y <= 1 
                && projTexCoord.z >= 0 && projTexCoord.z <= 1
                && texture(shadowMaps, vec3(projTexCoord.xy, i)).r - projTexCoord.z >= -0.01)
            {
                vec3 halfDir = normalize(viewDir + lightDir);
                float hDotV = dot(halfDir, viewDir);
                float hDotL = dot(halfDir, lightDir);
                float nDotH = dot(normalDir, halfDir);
                
                vec3 mfdFresnel;
                float grazingIntensity = getLuminance(weightedAverages[i] / specularColor);
                if (grazingIntensity <= 0.0)
                {
                    mfdFresnel = vec3(0,0,0);
                }
                else
                {
                    //mfdFresnel = max(vec3(0.0), fresnel(weightedAverages[i], vec3(grazingIntensity), hDotV));
					//mfdFresnel = max(vec3(0.0), fresnel(weightedAverages[i], vec3(dist(nDotH, roughness)), hDotV));
                    mfdFresnel = fresnel(specularColor, vec3(1.0), hDotV) * vec3(dist(nDotH, roughness));
					
					// mfdFresnel = -max(vec3(0.0), fresnel(weightedAverages[i], vec3(dist(nDotH, roughness)), hDotV))
									// + fresnel(specularColor, vec3(1.0), hDotV) * vec3(dist(nDotH, roughness))
									// + vec3(0.5);
					
					// mfdFresnel = vec3(getLuminance(max(vec3(0.0), fresnel(weightedAverages[i], vec3(dist(nDotH, roughness)), hDotV))),
						// getLuminance(fresnel(specularColor, vec3(1.0), hDotV) * vec3(dist(nDotH, roughness))),
						// getLuminance(fresnel(specularColor, vec3(1.0), hDotV) * vec3(dist(nDotH, roughness))));
						
						
					// vec3 reference = max(vec3(0.0), fresnel(weightedAverages[i], vec3(dist(nDotH, roughness)), hDotV));
					// vec3 fitted = fresnel(specularColor, vec3(1.0), hDotV) * vec3(dist(nDotH, roughness));
					
					// mfdFresnel = reference;
					
					// vec3 referenceXYZ = rgbToXYZ(reference);
					// vec3 fittedXYZ = rgbToXYZ(fitted);
				
					// // Pseudo-LAB color space
					// vec3 referenceLAB = vec3(referenceXYZ.y, 5 * (referenceXYZ.x - referenceXYZ.y), 2 * (referenceXYZ.y - referenceXYZ.z));
					// vec3 fittedLAB = vec3(fittedXYZ.y, 5 * (fittedXYZ.x - fittedXYZ.y), 2 * (fittedXYZ.y - fittedXYZ.z));
					
					// mfdFresnel = reference;
						// //vec3(0.5 - referenceLAB.y / sqrt(referenceLAB.x), 0.5, 0.5 - referenceLAB.z / sqrt(referenceLAB.x));
						// //vec3(referenceLAB.y * 0.5 + 0.5, fittedLAB.y * 0.5 + 0.5, fittedLAB.y * 0.5 + 0.5);
					
					// mfdFresnel = nDotH < sqrt(0.5) ? vec3(0.0) : fresnel(specularColor, vec3(1.0), hDotV)
						// * vec3(texture(mfdMap,(nDotH-sqrt(0.5))/(1.0-sqrt(0.5))).r) 
						// * 1 / 0.12459144371535306;
					
					// float error = getLuminance(nDotH < sqrt(0.5) ? vec3(0.0) : fresnel(specularColor, vec3(1.0), hDotV) * 
							// vec3(texture(mfdMap,(nDotH-sqrt(0.5))/(1.0-sqrt(0.5))).r) 
							// * 1 / 0.12459144371535306)
						// - getLuminance(max(vec3(0.0), fresnel(weightedAverages[i], vec3(dist(nDotH, roughness)), hDotV)));
					
					// mfdFresnel = //nDotH < sqrt(0.5) ? vec3(1,0,0) :
					// //	getLuminance(fresnel(specularColor, vec3(1.0), hDotV) * vec3(dist(nDotH, roughness)))
					// //		* vec3(0,1,0) + 
						// max(0,error)
							// * vec3(1,0,1) +
						// max(0, -error)
							// * vec3(0,1,0);
                }
				
				vec3 lightVectorTransformed = (model_view * vec4(lightDirUnNorm, 0.0)).xyz;
            
                reflectance += (fresnel(nDotL * diffuseColor, vec3(0.0), nDotL) + 
                    mfdFresnel * geom(roughness, nDotH, nDotV, nDotL, hDotV, hDotL) / (4 * nDotV))
                    * lightIntensityVirtual[i] / dot(lightVectorTransformed, lightVectorTransformed);
            }
        }
    }
    
    fragColor = tonemap(reflectance, 1.0);
}
