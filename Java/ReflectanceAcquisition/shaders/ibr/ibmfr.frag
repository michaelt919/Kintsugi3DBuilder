#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec4 fragColor;

#include "../reflectance/reflectance.glsl"
#include "../reflectance/imgspace.glsl"

#line 13 0

uniform mat4 model_view;
in vec3 fViewPos;

#define MAX_VIRTUAL_LIGHT_COUNT 4
uniform vec3 lightIntensity[MAX_VIRTUAL_LIGHT_COUNT];
uniform vec3 lightPos[MAX_VIRTUAL_LIGHT_COUNT];
uniform int virtualLightCount;

uniform float weightExponent;

uniform sampler2D diffuseMap;
uniform sampler2D normalMap;
uniform sampler2D specularMap;

uniform bool useDiffuseTexture;
uniform bool useNormalTexture;
uniform bool useSpecularTexture;

vec3 computeFresnelReflectivity(vec3 specularColor, float hDotV)
{
    float f0 = dot(specularColor, vec3(0.2126, 0.7152, 0.0722));
    float sqrtF0 = sqrt(f0);
    float ior = (1 + sqrtF0) / (1 - sqrtF0);
    float g = sqrt(ior*ior + hDotV * hDotV - 1);
    float fresnel = 0.5 * pow(g - hDotV, 2) / pow(g + hDotV, 2)
        * (1 + pow(hDotV * (g + hDotV) - 1, 2) / pow(hDotV * (g - hDotV) + 1, 2));
        
    return specularColor.rgb + (vec3(1.0) - specularColor.rgb) * max(0, fresnel - f0) / (1.0 - f0);
}

float computeSampleWeight(vec3 targetDir, vec3 sampleDir)
{
	return 1.0 / (1.0 - pow(max(0.0, dot(targetDir, sampleDir)), weightExponent)) - 1.0;
}

float computeGeometricAttenuation(float nDotH, float nDotV, float nDotL, float hDotV, float hDotL)
{
    return min(1.0, 2.0 * min(nDotV / hDotV, nDotL / hDotL));
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

vec2[MAX_VIRTUAL_LIGHT_COUNT] computeSample(int index, vec3 diffuseColor, vec3 normalDir, 
    vec3 specularColor, float maxLuminance)
{
    vec4 sampleColor = getColor(index);
    if (sampleColor.a > 0.0)
    {
        vec2 result[MAX_VIRTUAL_LIGHT_COUNT];
        
        // All in camera space
        vec3 fragmentPos = (cameraPoses[index] * vec4(fPosition, 1.0)).xyz;
        vec3 sampleViewDir = normalize(-fragmentPos);
        vec3 sampleLightDirUnnorm = lightPositions[lightIndices[index]].xyz - fragmentPos;
        vec3 sampleLightDir = normalize(sampleLightDirUnnorm);
        vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
        vec3 normalDirCameraSpace = (cameraPoses[index] * vec4(normalDir, 0.0)).xyz;
        
        float nDotL = max(0, dot(normalDirCameraSpace, sampleLightDir));
        float nDotV = max(0, dot(normalDirCameraSpace, sampleViewDir));
        float nDotH = max(0, dot(normalDirCameraSpace, sampleHalfDir));
        float hDotV = max(0, dot(sampleHalfDir, sampleViewDir));
        float hDotL = max(0, dot(sampleHalfDir, sampleLightDir));
    
        vec3 diffuseContrib = diffuseColor * nDotL ;// * attenuatedLightIntensity;
        
        float geomAtten = computeGeometricAttenuation(nDotH, nDotV, nDotL, hDotV, hDotL);
        if (geomAtten > 0.0)
        {
            vec4 specularResid = removeDiffuse(sampleColor, diffuseContrib, nDotL, maxLuminance);
            float precomputedSample = getLuminance(specularResid.rgb / specularColor)
                * nDotV / computeGeometricAttenuation(nDotH, nDotV, nDotL, hDotV, hDotL);
            
            vec3 virtualViewDir = normalize((cameraPoses[index] * vec4(fViewPos, 1.0)).xyz - fragmentPos);
            
            for (int lightPass = 0; lightPass < MAX_VIRTUAL_LIGHT_COUNT; lightPass++)
            {
                vec3 virtualLightDir = normalize((cameraPoses[index] * 
                    vec4(lightPos[lightPass], 1.0)).xyz - fragmentPos);
                vec3 virtualHalfDir = normalize(virtualViewDir + virtualLightDir);

                // Compute sample weight
                float weight = computeSampleWeight(virtualHalfDir, sampleHalfDir);
                result[lightPass] = weight * vec2(precomputedSample, sampleColor.a);
            }
        }
        
        return result;
    }
    else
    {
        vec2 result[MAX_VIRTUAL_LIGHT_COUNT];
        for (int lightPass = 0; lightPass < MAX_VIRTUAL_LIGHT_COUNT; lightPass++)
        {
            result[lightPass] = vec2(0.0);
        }
        return result;
    }
}

float[MAX_VIRTUAL_LIGHT_COUNT] computeMicrofacetDistributions(
    vec3 diffuseColor, vec3 normalDir, vec3 specularColor)
{
    float maxLuminance = getMaxLuminance();

	vec2[MAX_VIRTUAL_LIGHT_COUNT] sums;
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT; i++)
    {
        sums[i] = vec2(0.0);
    }
    
	for (int i = 0; i < viewCount; i++)
	{
        vec2[MAX_VIRTUAL_LIGHT_COUNT] microfacetSample = 
            computeSample(i, diffuseColor, normalDir, specularColor, maxLuminance);
        
        for (int j = 0; j < MAX_VIRTUAL_LIGHT_COUNT; j++)
        {
            sums[j] += microfacetSample[j];
        }
	}
    
    float[MAX_VIRTUAL_LIGHT_COUNT] results;
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT; i++)
    {
        if (sums[i].y > 0.0)
        {
            results[i] = sums[i].x / sums[i].y;
        }
        else
        {
            results[i] = 0.0;
        }
    }
	return results;
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
        specularColor = pow(texture(diffuseMap, fTexCoord).rgb, vec3(gamma));
    }
    else
    {
        specularColor = vec3(0.5);
    }
    
    float[] microfacetDistributions = 
        computeMicrofacetDistributions(diffuseColor, normalDir, specularColor);
    vec3 reflectance = vec3(0.0);
    
    float nDotV = dot(normalDir, viewDir);
    
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT && i < virtualLightCount; i++)
    {
        vec3 lightDirUnNorm = lightPos[i] - fPosition;
        vec3 lightDir = normalize(lightDirUnNorm);
        float nDotL = max(0.0, dot(normalDir, lightDir));
        
        if (nDotL > 0.0)
        {
            vec3 halfDir = normalize(viewDir + lightDir);
            float hDotV = dot(halfDir, viewDir);
            float hDotL = dot(halfDir, lightDir);
            float nDotH = dot(normalDir, halfDir);
        
            reflectance += nDotL * (diffuseColor + 
                microfacetDistributions[i]
                    * computeGeometricAttenuation(nDotH, nDotV, nDotL, hDotV, hDotL)
                    * computeFresnelReflectivity(specularColor, hDotV))
                * lightIntensity[i]
                * (infiniteLightSources ? 1.0 : 1.0 / 
                    dot(lightDirUnNorm, lightDirUnNorm));
        }
    }
    
    fragColor = vec4(pow(reflectance, vec3(1 / gamma)), 1.0);
}