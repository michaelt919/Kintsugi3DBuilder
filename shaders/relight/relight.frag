#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out int fragObjectID;

#include "../colorappearance/colorappearance_subset.glsl"
#include "../colorappearance/imgspace_subset.glsl"

#line 16 0

uniform int objectID;

uniform mat4 model_view;
uniform vec3 viewPos;
uniform mat4 envMapMatrix;

#define MAX_VIRTUAL_LIGHT_COUNT 4
uniform vec3 lightIntensityVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform vec3 lightPosVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform mat4 lightMatrixVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform int virtualLightCount;
uniform vec3 ambientColor;

uniform float renderGamma;
uniform float weightExponent;
uniform float isotropyFactor;

uniform sampler2D diffuseMap;
uniform sampler2D normalMap;
uniform sampler2D specularMap;
uniform sampler2D roughnessMap;
uniform samplerCube environmentMap;
uniform int environmentMipMapLevel;
uniform int diffuseEnvironmentMipMapLevel;

uniform bool useDiffuseTexture;
uniform bool useNormalTexture;
uniform bool useSpecularTexture;
uniform bool useRoughnessTexture;
uniform bool useEnvironmentMap;

uniform bool useInverseLuminanceMap;
uniform sampler1D inverseLuminanceMap;

uniform sampler2DArray shadowMaps;
uniform bool shadowsEnabled;

uniform bool useTSOverrides;
uniform vec3 lightDirTSOverride;
uniform vec3 viewDirTSOverride;

uniform bool imageBasedRenderingEnabled;
uniform bool relightingEnabled;
uniform bool pbrGeometricAttenuationEnabled;
uniform bool fresnelEnabled;

uniform int buehlerViewCount;
uniform bool perPixelWeightsEnabled;
uniform vec3 holeFillColor;

#define brdfMode false

#define BUEHLER_ALGORITHM true
#define BUEHLER_VIEW_COUNT 5

layout(std140) uniform ViewWeights
{
    vec4 viewWeights[MAX_CAMERA_POSE_COUNT_DIV_4];
};

float getViewWeight(int viewIndex)
{
    return extractComponentByIndex(viewWeights[viewIndex/4], viewIndex%4);
}

vec3 getEnvironmentFresnel(vec3 lightDirection, float fresnelFactor)
{
    if (useEnvironmentMap)
    {
        return ambientColor * textureLod(environmentMap, lightDirection,
            mix(environmentMipMapLevel, 0, fresnelFactor)).rgb;
    }
    else
    {
        return ambientColor;
    }
}

vec3 getEnvironment(vec3 lightDirection)
{
    if (useEnvironmentMap)
    {
        return ambientColor * textureLod(environmentMap, lightDirection, environmentMipMapLevel).rgb;
    }
    else
    {
        return ambientColor;
    }
}

vec3 getEnvironmentDiffuse(vec3 normalDirection)
{
    if (useEnvironmentMap)
    {
        return ambientColor * textureLod(environmentMap, normalDirection, diffuseEnvironmentMipMapLevel).rgb / 2;
    }
    else
    {
        return ambientColor;
    }
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
    return max(specularColor,
        specularColor + (grazingColor - specularColor) * pow(max(0.0, 1.0 - hDotV), 5.0));
}

vec3 fresnel(vec3 specularColor, vec3 grazingColor, float hDotV)
{
    //return specularColor;
    //return computeFresnelReflectivityActual(specularColor, grazingColor, hDotV);
    return computeFresnelReflectivitySchlick(specularColor, grazingColor, hDotV);
}

float computeGeometricAttenuationVCavity(float nDotH, float nDotV, float nDotL, float hDotV)
{
    return min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV);
}

//vec3 computeGeometricAttenuationSmithBeckmann(vec3 roughness, float nDotV, float nDotL)
//{
//    vec3 aV = 1.0 / (roughness * sqrt(1.0 - nDotV * nDotV) / nDotV);
//    vec3 aVSq = aV * aV;
//    vec3 aL = 1.0 / (roughness * sqrt(1.0 - nDotL * nDotL) / nDotL);
//    vec3 aLSq = aL * aL;
//
//    return (aV < 1.6 ? (3.535 * aV + 2.181 * aVSq) / (1 + 2.276 * aV + 2.577 * aVSq) : 1.0)
//            * (aL < 1.6 ? (3.535 * aL + 2.181 * aLSq) / (1 + 2.276 * aL + 2.577 * aLSq) : 1.0);
//        // ^ See Walter et al. "Microfacet Models for Refraction through Rough Surfaces"
//        // for this formula
//}

vec3 computeGeometricAttenuationSmithGGX(vec3 roughness, float nDotV, float nDotL)
{
    vec3 roughnessSq = roughness * roughness;
    return 4 / (1 + sqrt(1 + roughnessSq * (1 / (nDotV * nDotV) - 1.0)))
             / (1 + sqrt(1 + roughnessSq * (1 / (nDotL * nDotL) - 1.0)));
}

vec3 geom(vec3 roughness, float nDotH, float nDotV, float nDotL, float hDotV)
{
    //return nDotV * nDotL;
    return vec3(computeGeometricAttenuationVCavity(nDotH, nDotV, nDotL, hDotV));
    //return computeGeometricAttenuationSmithBeckmann(roughness, nDotV, nDotL);
    //return computeGeometricAttenuationSmithGGX(roughness, nDotV, nDotL);
}

vec3 computeMicrofacetDistributionGGX(float nDotH, vec3 roughness)
{
    vec3 roughnessSquared = roughness * roughness;
    float nDotHSquared = nDotH * nDotH;
    vec3 q = roughnessSquared + (1 - nDotHSquared) / nDotHSquared;

    // Assume scaling by pi
    return roughnessSquared / (nDotHSquared * nDotHSquared * q * q);
}

vec3 computeMicrofacetDistributionBeckmann(float nDotH, vec3 roughness)
{
    float nDotHSquared = nDotH * nDotH;
    vec3 roughnessSquared = roughness * roughness;

    // Assume scaling by pi
    return exp((nDotHSquared - 1.0) / (nDotHSquared * roughnessSquared))
            / (nDotHSquared * nDotHSquared * roughnessSquared);
}

vec3 computeMicrofacetDistributionPhong(float nDotH, vec3 roughness)
{
    float nDotHSquared = nDotH * nDotH;
    vec3 roughnessSquared = roughness * roughness;

    // Assume scaling by pi
    return max(vec3(0.0), pow(vec3(nDotH), 2 / roughnessSquared - 2) / (roughnessSquared));
}

vec3 dist(float nDotH, vec3 roughness)
{
    return computeMicrofacetDistributionGGX(nDotH, roughness);
    //return computeMicrofacetDistributionBeckmann(nDotH, roughness);
    //return computeMicrofacetDistributionPhong(nDotH, roughness);
}

float computeSampleWeight(float correlation)
{
    return 1.0 / max(0.000001, 1.0 - pow(max(0.0, correlation), weightExponent)) - 1.0;
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

vec4 computeEnvironmentSample(int virtualIndex, vec3 diffuseColor, vec3 normalDir,
    vec3 specularColor, vec3 roughness, float maxLuminance)
{
    mat4 cameraPose = getCameraPose(virtualIndex);
    vec3 fragmentPos = (cameraPose * vec4(fPosition, 1.0)).xyz;
    vec3 normalDirCameraSpace = normalize((cameraPose * vec4(normalDir, 0.0)).xyz);
    vec3 sampleViewDir = normalize(-fragmentPos);
    float nDotV_sample = max(0, dot(normalDirCameraSpace, sampleViewDir));

    if (nDotV_sample <= 0.0)
    {
        return vec4(0.0, 0.0, 0.0, 0.0);
    }
    else
    {
        vec4 sampleColor = getLinearColor(virtualIndex);
        if (sampleColor.a > 0.0)
        {
            // All in camera space
            vec3 sampleLightDirUnnorm = lightPositions[getLightIndex(virtualIndex)].xyz - fragmentPos;
            float lightDistSquared = dot(sampleLightDirUnnorm, sampleLightDirUnnorm);
            vec3 sampleLightDir = sampleLightDirUnnorm * inversesqrt(lightDistSquared);
            vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
            vec3 lightIntensity = getLightIntensity(virtualIndex);

            float nDotL_sample = max(0, dot(normalDirCameraSpace, sampleLightDir));
            float nDotH = max(0, dot(normalDirCameraSpace, sampleHalfDir));
            float hDotV_sample = max(0, dot(sampleHalfDir, sampleViewDir));

            vec3 diffuseContrib = diffuseColor * nDotL_sample * lightIntensity
                / (infiniteLightSources ? 1.0 : lightDistSquared);

            vec3 geomAttenSample = geom(roughness, nDotH, nDotV_sample, nDotL_sample, hDotV_sample);

            if (nDotV_sample > 0.0 && geomAttenSample != vec3(0))
            {
                vec3 virtualViewDir =
                    normalize((cameraPose * vec4(viewPos, 1.0)).xyz - fragmentPos);
                vec3 virtualLightDir = -reflect(virtualViewDir, sampleHalfDir);
                float nDotL_virtual = max(0, dot(normalDirCameraSpace, virtualLightDir));
                float nDotV_virtual = max(0, dot(normalDirCameraSpace, virtualViewDir));
                float hDotV_virtual = max(0, dot(sampleHalfDir, virtualViewDir));

                vec3 geomAttenVirtual =
                    (pbrGeometricAttenuationEnabled ?
                        geom(roughness, nDotH, nDotV_virtual, nDotL_virtual, hDotV_virtual) :
                            vec3(nDotL_virtual * nDotV_virtual));

                vec4 specularResid = removeDiffuse(sampleColor, diffuseContrib, nDotL_sample, maxLuminance);

                // Light intensities in view set files are assumed to be pre-divided by pi.
                // Or alternatively, the result of getLinearColor gives a result
                // where a diffuse reflectivity of 1 is represented by a value of pi.
                // See diffusefit.glsl
                vec3 mfdFresnel = specularResid.rgb / (lightIntensity * PI)
                     * (infiniteLightSources ? 1.0 : lightDistSquared)
                     * (pbrGeometricAttenuationEnabled ?
                        4 * nDotV_sample / geomAttenSample : vec3(4 / nDotL_sample));

                float mfdMono = getLuminance(mfdFresnel / specularColor);

                float weight = getCameraWeight(virtualIndex);

                return vec4(
                    (fresnelEnabled ? fresnel(mfdFresnel, vec3(mfdMono), hDotV_virtual) : mfdFresnel)
                        * geomAttenVirtual / (4 * nDotV_virtual)
                        * getEnvironment(mat3(envMapMatrix) * transpose(mat3(cameraPose))
                                            * virtualLightDir),
                    // // Disabled code: normalizes with respect to specular texture when available
                    // // as described in our Archiving 2017 paper.
                    // (useSpecularTexture ?
                            // mfdMono * geomAttenVirtual / (4 * nDotV_virtual) : 1.0 / (2.0 * PI))
                    1.0 / (2.0 * PI)
                ) * 4 * hDotV_virtual * (weight * 4 * PI * viewCount);
                // dl = 4 * h dot v * dh
                // weight * viewCount -> brings weights back to being on the order of 1
                // This is helpful for consistency with numerical limits (i.e. clamping)
                // Everything gets normalized at the end again anyways.
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

vec3 getEnvironmentShading(vec3 diffuseColor, vec3 normalDir, vec3 specularColor, vec3 roughness)
{
    float maxLuminance = getMaxLuminance();

    vec4 sum = vec4(0.0);

    for (int i = 0; i < viewCount; i++)
    {
        sum += computeEnvironmentSample(i, diffuseColor, normalDir, specularColor, roughness, maxLuminance);
    }

    if (sum.y > 0.0)
    {
        return sum.rgb
            / viewCount;                    // better spatial consistency, worse directional consistency?
        //    / clamp(sum.a, 0, 1000000.0);    // Better directional consistency, worse spatial consistency?
    }
    else
    {
        return vec3(0.0);
    }
}

vec4[MAX_VIRTUAL_LIGHT_COUNT] computeSample(int virtualIndex, vec3 diffuseColor, vec3 normalDir,
    vec3 specularColor, vec3 roughness, float maxLuminance)
{
    vec4 sampleColor = getLinearColor(virtualIndex);
    mat4 cameraPose = getCameraPose(virtualIndex);
    if (sampleColor.a > 0.0)
    {
        vec4 result[MAX_VIRTUAL_LIGHT_COUNT];

        // All in camera space
        vec3 fragmentPos = (cameraPose * vec4(fPosition, 1.0)).xyz;
        vec3 sampleViewDir = normalize(-fragmentPos);
        vec3 sampleLightDirUnnorm = lightPositions[getLightIndex(virtualIndex)].xyz - fragmentPos;
        float lightDistSquared = dot(sampleLightDirUnnorm, sampleLightDirUnnorm);
        vec3 sampleLightDir = sampleLightDirUnnorm * inversesqrt(lightDistSquared);
        vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
        vec3 normalDirCameraSpace = normalize((cameraPose * vec4(normalDir, 0.0)).xyz);
        vec3 lightIntensity = getLightIntensity(virtualIndex);

        float nDotL = max(0, dot(normalDirCameraSpace, sampleLightDir));
        float nDotV = max(0, dot(normalDirCameraSpace, sampleViewDir));
        float nDotH = max(0, dot(normalDirCameraSpace, sampleHalfDir));
        float hDotV = max(0, dot(sampleHalfDir, sampleViewDir));

        vec3 diffuseContrib = diffuseColor * nDotL * lightIntensity
            / (infiniteLightSources ? 1.0 : lightDistSquared);

        vec3 geomAtten = geom(roughness, nDotH, nDotV, nDotL, hDotV);
        if (!relightingEnabled || geomAtten != vec3(0))
        {
            vec4 precomputedSample;

            if (relightingEnabled)
            {
                vec4 specularResid = removeDiffuse(sampleColor, diffuseContrib, nDotL, maxLuminance);

                if(pbrGeometricAttenuationEnabled)
                {
                    precomputedSample = sampleColor.a
                        * vec4(specularResid.rgb * 4 * nDotV / lightIntensity * (infiniteLightSources ? 1.0 : lightDistSquared), geomAtten);
                }
                else
                {
                    precomputedSample = sampleColor.a
                        * vec4(specularResid.rgb * 4 / lightIntensity * (infiniteLightSources ? 1.0 : lightDistSquared), nDotL);
                }
            }
            else
            {
                precomputedSample = sampleColor.a * vec4(sampleColor.rgb, 1.0);
            }

            mat3 tangentToObject = mat3(1.0);

            vec3 virtualViewDir;
            if (useTSOverrides)
            {
                vec3 gNormal = normalize(fNormal);
                vec3 tangent = normalize(fTangent - dot(gNormal, fTangent));
                vec3 bitangent = normalize(fBitangent
                    - dot(gNormal, fBitangent) * gNormal
                    - dot(tangent, fBitangent) * tangent);
                tangentToObject = mat3(tangent, bitangent, gNormal);

                virtualViewDir = normalize(mat3(cameraPose) * tangentToObject * viewDirTSOverride);
            }
            else
            {
                virtualViewDir = normalize((cameraPose * vec4(viewPos, 1.0)).xyz - fragmentPos);
            }

            for (int lightPass = 0; lightPass < MAX_VIRTUAL_LIGHT_COUNT; lightPass++)
            {
                if (perPixelWeightsEnabled)
                {
                    vec3 virtualLightDir;
                    if (useTSOverrides)
                    {
                        virtualLightDir =
                            normalize(mat3(cameraPose) * tangentToObject * lightDirTSOverride);
                    }
                    else if (relightingEnabled)
                    {
                        virtualLightDir = normalize((cameraPose *
                            vec4(lightPosVirtual[lightPass], 1.0)).xyz - fragmentPos);
                    }
                    else
                    {
                        virtualLightDir = virtualViewDir;
                    }

                    // Compute sample weight
                    vec3 virtualHalfDir = normalize(virtualViewDir + virtualLightDir);
                    float virtualNdotH = max(0, dot(normalDirCameraSpace, virtualHalfDir));
                    float weight = computeSampleWeight(
                        isotropyFactor * (nDotH * virtualNdotH + sqrt(1 - nDotH*nDotH) * sqrt(1 - virtualNdotH*virtualNdotH)) +
                        (1 - isotropyFactor) * dot(virtualHalfDir, sampleHalfDir));

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
                else
                {
                    result[lightPass] = getViewWeight(virtualIndex) * precomputedSample;
                }
            }
        }
        else
        {
            for (int lightPass = 0; lightPass < MAX_VIRTUAL_LIGHT_COUNT; lightPass++)
            {
                result[lightPass] = vec4(0.0);
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

vec4 computeSampleSingle(int virtualIndex, vec3 diffuseColor, vec3 normalDir,  vec3 specularColor, vec3 roughness, float maxLuminance)
{
    vec4 sampleColor = getLinearColor(virtualIndex);
    mat4 cameraPose = getCameraPose(virtualIndex);
    if (sampleColor.a > 0.0)
    {
        vec4 result[MAX_VIRTUAL_LIGHT_COUNT];

        // All in camera space
        vec3 fragmentPos = (cameraPose * vec4(fPosition, 1.0)).xyz;
        vec3 sampleViewDir = normalize(-fragmentPos);
        vec3 sampleLightDirUnnorm = lightPositions[getLightIndex(virtualIndex)].xyz - fragmentPos;
        float lightDistSquared = dot(sampleLightDirUnnorm, sampleLightDirUnnorm);
        vec3 sampleLightDir = sampleLightDirUnnorm * inversesqrt(lightDistSquared);
        vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
        vec3 normalDirCameraSpace = normalize((cameraPose * vec4(normalDir, 0.0)).xyz);
        vec3 lightIntensity = getLightIntensity(virtualIndex);

        float nDotL = max(0, dot(normalDirCameraSpace, sampleLightDir));
        float nDotV = max(0, dot(normalDirCameraSpace, sampleViewDir));
        float nDotH = max(0, dot(normalDirCameraSpace, sampleHalfDir));
        float hDotV = max(0, dot(sampleHalfDir, sampleViewDir));

        vec3 diffuseContrib = diffuseColor * nDotL * lightIntensity
            / (infiniteLightSources ? 1.0 : lightDistSquared);

        vec3 geomAtten = geom(roughness, nDotH, nDotV, nDotL, hDotV);
        if (!relightingEnabled || geomAtten != vec3(0))
        {
            vec4 precomputedSample;

            if (relightingEnabled)
            {
                vec4 specularResid = removeDiffuse(sampleColor, diffuseContrib, nDotL, maxLuminance);

                if(pbrGeometricAttenuationEnabled)
                {
                    return sampleColor.a
                        * vec4(specularResid.rgb * 4 * nDotV / lightIntensity * (infiniteLightSources ? 1.0 : lightDistSquared), geomAtten);
                }
                else
                {
                    return sampleColor.a
                        * vec4(specularResid.rgb * 4 / lightIntensity * (infiniteLightSources ? 1.0 : lightDistSquared), nDotL);
                }
            }
            else
            {
                return sampleColor.a * vec4(sampleColor.rgb, 1.0);
            }
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

vec4[MAX_VIRTUAL_LIGHT_COUNT] computeWeightedAverages(vec3 diffuseColor, vec3 normalDir, vec3 specularColor, vec3 roughness)
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

    vec4[MAX_VIRTUAL_LIGHT_COUNT] results;
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT; i++)
    {
        if (!perPixelWeightsEnabled)
        {
            results[i] = sums[i];
        }
        else if (sums[i].y > 0.0)
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

float computeBuehlerWeight(vec3 targetDirection, vec3 sampleDirection)
{
    return 1.0 / (1.0 - clamp(dot(sampleDirection, targetDirection), 0.0, 0.99999));
}

float getBuehlerWeight(int virtualIndex, vec3 targetDirection)
{
    mat4 cameraPose = getCameraPose(virtualIndex);
    return computeBuehlerWeight(mat3(cameraPose) * targetDirection, -normalize((cameraPose * vec4(fPosition, 1)).xyz));
}

vec4 computeBuehler(vec3 targetDirection, vec3 diffuseColor, vec3 normalDir, vec3 specularColor, vec3 roughness)
{
    float maxLuminance = getMaxLuminance();

    float weights[BUEHLER_VIEW_COUNT];
    int indices[BUEHLER_VIEW_COUNT];

    // Initialization
    for (int i = 0; i < BUEHLER_VIEW_COUNT && i < viewCount; i++)
    {
        weights[i] = -(1.0 / 0.0); // Parentheses needed for AMD cards.
        indices[i] = -1;
    }

    for (int i = viewCount; i < BUEHLER_VIEW_COUNT; i++)
    {
        weights[i] = 0.0; // If there are less samples available than requested, fill in with weights of 0.0.
        indices[i] = 0;
    }

    // Partial heapsort
    for (int i = 0; i < MAX_CAMERA_POSE_COUNT && i < viewCount; i++)
    {
        float weight = getBuehlerWeight(i, targetDirection);
        if (weight >= weights[0]) // Decide if the new view goes in the heap
        {
            // Replace the min node in the heap with the new one
            weights[0] = weight;
            indices[0] = i;

            int currentIndex = 0;
            int minIndex = -1;

            while (currentIndex != -1)
            {
                // The two "children" in the heap
                int leftIndex = 2*currentIndex+1;
                int rightIndex = 2*currentIndex+2;

                // Find the smallest of the current node, and its left and right children
                if ( leftIndex < BUEHLER_VIEW_COUNT && weights[leftIndex] < weights[currentIndex])
                {
                    minIndex = leftIndex;
                }
                else
                {
                    minIndex = currentIndex;
                }

                if (rightIndex < BUEHLER_VIEW_COUNT && weights[rightIndex] < weights[minIndex])
                {
                    minIndex = rightIndex;
                }

                // If a child is smaller than the current node, then swap
                if (minIndex != currentIndex)
                {
                    float weightTmp = weights[currentIndex];
                    int indexTmp = indices[currentIndex];
                    weights[currentIndex] = weights[minIndex];
                    indices[currentIndex] = indices[minIndex];
                    weights[minIndex] = weightTmp;
                    indices[minIndex] = indexTmp;

                    currentIndex = minIndex;
                }
                else
                {
                    currentIndex = -1; // Signal to quit
                }
            }
        }
    }

    // Evaluate the light field
    // Because of the min-heap property, weights[0] should be the smallest weight
    vec4 sum = vec4(0.0);
    for (int i = 1; i < BUEHLER_VIEW_COUNT; i++)
    {
        vec4 computedSample = computeSampleSingle(indices[i], diffuseColor, normalDir, specularColor, roughness, maxLuminance);
        if (computedSample.a > 0)
        {
            sum += (weights[i] - weights[0]) * computedSample;
        }
    }

    if (sum.a == 0.0)
    {
        return vec4(holeFillColor, 1.0);
    }
    else
    {
        return sum / sum.a;
    }
}

vec4 tonemap(vec3 color, float alpha)
{
    return vec4(pow(color, vec3(1.0 / renderGamma)), alpha);
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
        viewDir = normalize(viewPos - fPosition);
    }

    vec2 normalDirXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));

    vec3 gNormal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(gNormal, fTangent) * gNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(gNormal, fBitangent) * gNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, gNormal);

    vec3 normalDir;
    if (useNormalTexture)
    {
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
    else if (!imageBasedRenderingEnabled && !useSpecularTexture)
    {
        diffuseColor = vec3(0.125);
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
    else if (!imageBasedRenderingEnabled && useDiffuseTexture)
    {
        specularColor = vec3(0.0);
    }
    else
    {
        specularColor = vec3(0.03125); // TODO pass in a default?
    }

    vec3 specularColorXYZ = rgbToXYZ(specularColor);

    vec3 roughness;
    if (useRoughnessTexture)
    {
        vec3 sqrtRoughness = texture(roughnessMap, fTexCoord).rgb;
        roughness = sqrtRoughness * sqrtRoughness;
    }
    else
    {
        roughness = vec3(0.25); // TODO pass in a default?
    }

    vec3 roughnessSq = roughness * roughness;

    float nDotV = useTSOverrides ? viewDir.z : dot(normalDir, viewDir);
    vec3 reflectance = vec3(0.0);

    vec4[MAX_VIRTUAL_LIGHT_COUNT] weightedAverages;

    if (!BUEHLER_ALGORITHM && imageBasedRenderingEnabled)
    {
        weightedAverages = computeWeightedAverages(diffuseColor, normalDir, specularColor, roughness);
    }

    if ((relightingEnabled || !imageBasedRenderingEnabled) && ambientColor != vec3(0))
    {
        reflectance += diffuseColor * getEnvironmentDiffuse((envMapMatrix * vec4(normalDir, 0.0)).xyz);

        if (imageBasedRenderingEnabled)
        {
            // Old fresnel implementation
            // if (fresnelEnabled)
            // {
                // reflectance +=
                    // fresnel(getEnvironmentShading(diffuseColor, normalDir, specularColor, roughness),
                        // getEnvironmentFresnel(
                            // (envMapMatrix * vec4(-reflect(viewDir, normalDir), 0.0)).xyz,
                                // pow(1 - nDotV, 5)), nDotV);
            // }
            // else
            {
                reflectance += getEnvironmentShading(diffuseColor, normalDir, specularColor, roughness);
            }
        }
        else
        {
            vec3 reflectivity;
            if (useSpecularTexture)
            {
                reflectivity = min(vec3(1.0), diffuseColor + specularColor);
            }
            else
            {
                reflectivity = diffuseColor;
            }

            if (fresnelEnabled)
            {
                reflectance += fresnel(ambientColor * reflectivity, ambientColor, nDotV);
            }
            else
            {
                reflectance += ambientColor * reflectivity;
            }
        }

        // For debugging environment mapping:
        //reflectance = getEnvironment((envMapMatrix * vec4(-reflect(viewDir, normalDir), 0.0)).xyz);
        //reflectance = getEnvironmentDiffuse((envMapMatrix * vec4(normalDir, 0.0)).xyz);
    }

    int effectiveLightCount = (relightingEnabled || !imageBasedRenderingEnabled ? virtualLightCount : 1);

    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT && i < effectiveLightCount; i++)
    {
        vec3 lightDirUnNorm;
        vec3 lightDir;
        float nDotL;
        if (useTSOverrides)
        {
            lightDirUnNorm = lightDir = lightDirTSOverride;
            nDotL = max(0.0, lightDir.z);
        }
        else if (!imageBasedRenderingEnabled || relightingEnabled)
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
            if (!useTSOverrides && relightingEnabled && shadowsEnabled)
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
                float nDotH = useTSOverrides ? halfDir.z : dot(normalDir, halfDir);

                float nDotHSq = max(0, nDotH) * max(0, nDotH);

                vec4 predictedMFD;
                if (imageBasedRenderingEnabled)
                {
                    if (BUEHLER_ALGORITHM)
                    {
                        vec4 weightedAverage = computeBuehler(
                            useTSOverrides ? tangentToObject * halfDir : halfDir,
                            diffuseColor, normalDir, specularColor, roughness);
                        predictedMFD = weightedAverage;
                    }
                    else
                    {
                        predictedMFD = weightedAverages[i];
                    }
                }

                if (predictedMFD.w < 1.0)
                {
                    predictedMFD.rgb += (1 - predictedMFD.w) * holeFillColor;
                }

                vec3 mfdFresnel;

                if ((!imageBasedRenderingEnabled || relightingEnabled) && fresnelEnabled)
                {
                    if (imageBasedRenderingEnabled)
                    {
                        float grazingIntensity = getLuminance(predictedMFD.rgb
                            / max(vec3(1 / predictedMFD.a), specularColor));

                        if (grazingIntensity <= 0.0)
                        {
                            mfdFresnel = vec3(0,0,0);
                        }
                        else
                        {
                            mfdFresnel = max(vec3(0.0),
                                fresnel(predictedMFD.rgb, vec3(grazingIntensity), hDotV));
                                // fresnel(predictedMFD.rgb, vec3(dist(nDotH, roughness)), hDotV));
                        }
                    }
                    else
                    {
                        vec3 mfdFresnelBaseXYZ = rgbToXYZ(specularColor) * dist(nDotH, roughness);
                        mfdFresnel = fresnel(xyzToRGB(mfdFresnelBaseXYZ), vec3(mfdFresnelBaseXYZ.y), hDotV);
                    }
                }
                else
                {
                    if (imageBasedRenderingEnabled)
                    {
                        mfdFresnel = max(vec3(0.0), predictedMFD.rgb);
                    }
                    else
                    {
                        mfdFresnel = xyzToRGB(rgbToXYZ(specularColor) * dist(nDotH, roughness));
                    }
                }

                vec3 lightVectorTransformed = (model_view * vec4(lightDirUnNorm, 0.0)).xyz;

                reflectance += (
                    (relightingEnabled || !imageBasedRenderingEnabled ? nDotL * diffuseColor : vec3(0.0)) +
                    mfdFresnel
                     * ((!imageBasedRenderingEnabled || relightingEnabled) && pbrGeometricAttenuationEnabled
                        ? geom(roughness, nDotH, nDotV, nDotL, hDotV) / (4 * nDotV) :
                            vec3(!imageBasedRenderingEnabled || relightingEnabled ? nDotL / 4 : 1.0)))
                     * ((relightingEnabled || !imageBasedRenderingEnabled) ?
                        (useTSOverrides ? lightIntensityVirtual[i] :
                            lightIntensityVirtual[i] / dot(lightVectorTransformed, lightVectorTransformed))
                        : vec3(1.0))
                     / (brdfMode ? nDotL : 1.0);
            }
        }
    }

    fragColor = tonemap(reflectance, 1.0);

    fragObjectID = objectID;
}
