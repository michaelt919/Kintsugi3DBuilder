#version 330
#extension GL_ARB_texture_query_lod : enable

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out int fragObjectID;

#ifndef SVD_MODE
#define SVD_MODE 0
#endif

#ifndef BUEHLER_ALGORITHM
#define BUEHLER_ALGORITHM 1
#endif

#ifndef USE_HEAPSORT
#define USE_HEAPSORT 1
#endif

#ifndef SORTING_SAMPLE_COUNT
#define SORTING_SAMPLE_COUNT 5
#endif

#ifndef MAX_VIRTUAL_LIGHT_COUNT
#define MAX_VIRTUAL_LIGHT_COUNT 4
#endif

#include "../colorappearance/colorappearance.glsl"
#include "reflectanceequations.glsl"
#include "environment.glsl"
#include "tonemap.glsl"

#if SVD_MODE
#include "../colorappearance/svd_unpack.glsl"
#else
#include "../colorappearance/imgspace.glsl"
#endif

#define SORTING_TOTAL_COUNT VIEW_COUNT

#include "sort.glsl"

#line 49 0

uniform int objectID;

uniform mat4 model_view;
uniform vec3 viewPos;
uniform mat4 envMapMatrix;

uniform vec3 lightIntensityVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform vec3 lightPosVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform vec3 lightOrientationVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform float lightSpotSizeVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform float lightSpotTaperVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform mat4 lightMatrixVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform int virtualLightCount;
uniform bool useSpotLights;

uniform float weightExponent;
uniform float isotropyFactor;

uniform sampler2D diffuseMap;
uniform sampler2D normalMap;
uniform sampler2D specularMap;
uniform sampler2D roughnessMap;

uniform bool useDiffuseTexture;
uniform bool useNormalTexture;
uniform bool useSpecularTexture;
uniform bool useRoughnessTexture;

uniform sampler2DArray shadowMaps;
uniform bool shadowsEnabled;

uniform bool useTSOverrides;
uniform vec3 lightDirTSOverride;
uniform vec3 viewDirTSOverride;

uniform bool imageBasedRenderingEnabled;
uniform bool relightingEnabled;
uniform bool pbrGeometricAttenuationEnabled;
uniform bool fresnelEnabled;

uniform bool perPixelWeightsEnabled;
uniform vec3 holeFillColor;

#define brdfMode false

#if SVD_MODE
#define residualImages true
#else
#define residualImages false
#endif

layout(std140) uniform ViewWeights
{
    vec4 viewWeights[VIEW_COUNT_DIV_4];
};

float getViewWeight(int viewIndex)
{
    return extractComponentByIndex(viewWeights[viewIndex/4], viewIndex%4);
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

            vec3 mfdFresnel;
            float mfdMono;

#if SVD_MODE
            vec3 sqrtAdjustedRoughness = sqrt(roughness) + getColor(virtualIndex).xyz - vec3(0.5);
            vec3 adjustedRoughness = sqrtAdjustedRoughness * sqrtAdjustedRoughness;
            vec3 mfd = dist(nDotH, adjustedRoughness) / PI;
            mfdMono = mfd.y;
            mfdFresnel = xyzToRGB(mfd * rgbToXYZ(specularColor) * adjustedRoughness * adjustedRoughness / (roughness * roughness));
#else
            vec4 sampleColor = getLinearColor(virtualIndex);
            if (sampleColor.a == 0.0)
            {
                return vec4(0.0, 0.0, 0.0, 0.0);
            }

            vec4 specularResid = removeDiffuse(sampleColor, diffuseContrib, nDotL_sample, maxLuminance);

            // Light intensities in view set files are assumed to be pre-divided by pi.
            // Or alternatively, the result of getLinearColor gives a result
            // where a diffuse reflectivity of 1 is represented by a value of pi.
            // See diffusefit.glsl
            mfdFresnel = specularResid.rgb / (lightIntensity * PI)
                 * (infiniteLightSources ? 1.0 : lightDistSquared)
                 * (pbrGeometricAttenuationEnabled ?
                    4 * nDotV_sample / geomAttenSample : vec3(4 / nDotL_sample));

            mfdMono = getLuminance(mfdFresnel / specularColor);
#endif

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
            ) * 4 * hDotV_virtual * (weight * 4 * PI * VIEW_COUNT);
            // dl = 4 * h dot v * dh
            // weight * VIEW_COUNT -> brings weights back to being on the order of 1
            // This is helpful for consistency with numerical limits (i.e. clamping)
            // Everything gets normalized at the end again anyways.
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

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        sum += computeEnvironmentSample(i, diffuseColor, normalDir, specularColor, roughness, maxLuminance);
    }

    if (sum.y > 0.0)
    {
        return sum.rgb
            / VIEW_COUNT;                    // better spatial consistency, worse directional consistency?
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
    // All in camera space
    mat4 cameraPose = getCameraPose(virtualIndex);
    vec3 fragmentPos = (cameraPose * vec4(fPosition, 1.0)).xyz;
    vec3 sampleViewDir = normalize(-fragmentPos);
    vec3 sampleLightDirUnnorm = lightPositions[getLightIndex(virtualIndex)].xyz - fragmentPos;
    float lightDistSquared = dot(sampleLightDirUnnorm, sampleLightDirUnnorm);
    vec3 sampleLightDir = sampleLightDirUnnorm * inversesqrt(lightDistSquared);
    vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
    vec3 normalDirCameraSpace = normalize((cameraPose * vec4(normalDir, 0.0)).xyz);
    float nDotH = max(0, dot(normalDirCameraSpace, sampleHalfDir));
    float nDotL = max(0, dot(normalDirCameraSpace, sampleLightDir));
    float nDotV = max(0, dot(normalDirCameraSpace, sampleViewDir));
    float hDotV = max(0, dot(sampleHalfDir, sampleViewDir));

    vec4 precomputedSample = vec4(0);

    if (residualImages)
    {
        vec4 residual = getColor(virtualIndex);
        if (residual.w > 0)
        {
            vec3 roughnessSquared = roughness * roughness;
            precomputedSample = residual.w * vec4(xyzToRGB(
                pow(max(vec3(0), pow(dist(nDotH, roughness) * roughnessSquared, vec3(1.0 / gamma))
                    + (residual.xyz - vec3(0.5))), vec3(gamma))
                * rgbToXYZ(specularColor) / roughnessSquared) , 1.0);
        }
    }
    else
    {
        vec4 sampleColor = getLinearColor(virtualIndex);
        if (sampleColor.a > 0.0)
        {
            vec3 lightIntensity = getLightIntensity(virtualIndex);

            vec3 diffuseContrib = diffuseColor * nDotL * lightIntensity
                / (infiniteLightSources ? 1.0 : lightDistSquared);

            vec3 geomAtten = geom(roughness, nDotH, nDotV, nDotL, hDotV);
            if (geomAtten != vec3(0))
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
        }
    }

    if (precomputedSample.w != 0)
    {
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

        vec4 result[MAX_VIRTUAL_LIGHT_COUNT];

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
                result[lightPass] = weight * precomputedSample;
            }
            else
            {
                result[lightPass] = getViewWeight(virtualIndex) * precomputedSample;
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
    // All in camera space
    mat4 cameraPose = getCameraPose(virtualIndex);
    vec3 fragmentPos = (cameraPose * vec4(fPosition, 1.0)).xyz;
    vec3 sampleViewDir = normalize(-fragmentPos);
    vec3 sampleLightDirUnnorm = lightPositions[getLightIndex(virtualIndex)].xyz - fragmentPos;
    float lightDistSquared = dot(sampleLightDirUnnorm, sampleLightDirUnnorm);
    vec3 sampleLightDir = sampleLightDirUnnorm * inversesqrt(lightDistSquared);
    vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
    vec3 normalDirCameraSpace = normalize((cameraPose * vec4(normalDir, 0.0)).xyz);
    float nDotH = max(0, dot(normalDirCameraSpace, sampleHalfDir));
    float nDotL = max(0, dot(normalDirCameraSpace, sampleLightDir));
    float nDotV = max(0, dot(normalDirCameraSpace, sampleViewDir));
    float hDotV = max(0, dot(sampleHalfDir, sampleViewDir));

    vec4 precomputedSample = vec4(0);

    if (residualImages)
    {
        vec4 residual = getColor(virtualIndex);
        if (residual.w > 0)
        {
            vec3 roughnessSquared = roughness * roughness;
            return residual.w * vec4(xyzToRGB(
                pow(max(vec3(0), pow(dist(nDotH, roughness) * roughnessSquared, vec3(1.0 / gamma))
                    + (residual.xyz - vec3(0.5))), vec3(gamma))
                * rgbToXYZ(specularColor) / roughnessSquared) , 1.0);
        }
    }
    else
    {
        vec4 sampleColor = getLinearColor(virtualIndex);
        if (sampleColor.a > 0.0)
        {
            vec3 lightIntensity = getLightIntensity(virtualIndex);

            vec3 diffuseContrib = diffuseColor * nDotL * lightIntensity
                / (infiniteLightSources ? 1.0 : lightDistSquared);

            vec3 geomAtten = geom(roughness, nDotH, nDotV, nDotL, hDotV);
            if (geomAtten != vec3(0))
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
        }
    }

    return vec4(0.0);
}

vec4[MAX_VIRTUAL_LIGHT_COUNT] computeWeightedAverages(vec3 diffuseColor, vec3 normalDir, vec3 specularColor, vec3 roughness)
{
    float maxLuminance = getMaxLuminance();

    vec4[MAX_VIRTUAL_LIGHT_COUNT] sums;
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT; i++)
    {
        sums[i] = vec4(0.0);
    }

    for (int i = 0; i < VIEW_COUNT; i++)
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

vec4 computeBuehler(vec3 targetDirection, vec3 diffuseColor, vec3 normalDir, vec3 specularColor, vec3 roughness)
{
    float maxLuminance = getMaxLuminance();

    float weights[SORTING_SAMPLE_COUNT];
    int indices[SORTING_SAMPLE_COUNT];

    sort(targetDirection, weights, indices);

    // Evaluate the light field
    // weights[0] should be the smallest weight
    vec4 sum = vec4(0.0);
    for (int i = 1; i < SORTING_SAMPLE_COUNT; i++)
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
        vec3 roughnessLookup = texture(roughnessMap, fTexCoord).rgb;
        vec3 sqrtRoughness = vec3(
            roughnessLookup.y + roughnessLookup.x - 16.0 / 31.0,
            roughnessLookup.y,
            roughnessLookup.y + roughnessLookup.z - 16.0 / 31.0);
        roughness = sqrtRoughness * sqrtRoughness;
    }
    else
    {
        roughness = vec3(0.25); // TODO pass in a default?
    }

    vec3 roughnessSq = roughness * roughness;

    float nDotV = useTSOverrides ? viewDir.z : dot(normalDir, viewDir);
    vec3 radiance = vec3(0.0);

#if !BUEHLER_ALGORITHM
    vec4[MAX_VIRTUAL_LIGHT_COUNT] weightedAverages;

    if (imageBasedRenderingEnabled)
    {
        weightedAverages = computeWeightedAverages(diffuseColor, normalDir, specularColor, roughness);
    }
#endif

    if (relightingEnabled && ambientColor != vec3(0))
    {
        radiance += diffuseColor * getEnvironmentDiffuse((envMapMatrix * vec4(normalDir, 0.0)).xyz);

        if (imageBasedRenderingEnabled)
        {
            // Old fresnel implementation
            // if (fresnelEnabled)
            // {
                // radiance +=
                    // fresnel(getEnvironmentShading(diffuseColor, normalDir, specularColor, roughness),
                        // getEnvironmentFresnel(
                            // (envMapMatrix * vec4(-reflect(viewDir, normalDir), 0.0)).xyz,
                                // pow(1 - nDotV, 5)), nDotV);
            // }
            // else
            {
                radiance += getEnvironmentShading(diffuseColor, normalDir, specularColor, roughness);
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
                radiance += fresnel(ambientColor * reflectivity, ambientColor, nDotV);
            }
            else
            {
                radiance += ambientColor * reflectivity;
            }
        }

        // For debugging environment mapping:
        //radiance = getEnvironment((envMapMatrix * vec4(-reflect(viewDir, normalDir), 0.0)).xyz);
        //radiance = getEnvironmentDiffuse((envMapMatrix * vec4(normalDir, 0.0)).xyz);
    }

    int effectiveLightCount = (relightingEnabled ? virtualLightCount : 1);

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
        else if (relightingEnabled)
        {
            lightDirUnNorm = lightPosVirtual[i] - fPosition;
            lightDir = normalize(lightDirUnNorm);
            nDotL = max(0.0, dot(normalDir, lightDir));
        }
        else
        {
            lightDirUnNorm = viewPos - fPosition;
            lightDir = viewDir;
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
#if BUEHLER_ALGORITHM
                    vec4 weightedAverage = computeBuehler(
                        useTSOverrides ? tangentToObject * halfDir : halfDir,
                        diffuseColor, normalDir, specularColor, roughness);
                    predictedMFD = weightedAverage;
#else
                    predictedMFD = weightedAverages[i];
#endif
                }

                if (predictedMFD.w < 1.0)
                {
                    predictedMFD.rgb += (1 - predictedMFD.w) * holeFillColor;
                }

                vec3 mfdFresnel;

                if (relightingEnabled && fresnelEnabled)
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
                        vec3 mfdFresnelBaseXYZ = specularColorXYZ * dist(nDotH, roughness);
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
                        mfdFresnel = xyzToRGB(specularColorXYZ * dist(nDotH, roughness));
                    }
                }

                vec3 lightVectorTransformed = (model_view * vec4(lightDirUnNorm, 0.0)).xyz;

                vec3 pointRadiance;

                vec3 reflectance = nDotL * diffuseColor;

                if (pbrGeometricAttenuationEnabled)
                {
                    reflectance += mfdFresnel * geom(roughness, nDotH, nDotV, nDotL, hDotV) / (4 * nDotV);
                }
                else
                {
                    reflectance += mfdFresnel * nDotL / 4;
                }

                vec3 irradiance = lightIntensityVirtual[i];

                if (!useTSOverrides)
                {
                    irradiance /= dot(lightVectorTransformed, lightVectorTransformed);

                    if (useSpotLights)
                    {
                        float lightDirCorrelation = max(0.0, dot(lightDir, -lightOrientationVirtual[i]));
                        float spotBoundaryDistance = lightSpotSizeVirtual[i] - sqrt(1 - lightDirCorrelation * lightDirCorrelation);
                        irradiance *= clamp(
                            spotBoundaryDistance / max(0.001, max(lightSpotSizeVirtual[i] * lightSpotTaperVirtual[i], spotBoundaryDistance)),
                            0.0, 1.0);
                    }
                }

                pointRadiance = reflectance * irradiance;

                if (brdfMode)
                {
                    radiance += pointRadiance / nDotL;
                }
                else
                {
                    radiance += pointRadiance;
                }
            }
        }
    }

    fragColor = tonemap(radiance, 1.0);

    fragObjectID = objectID;
}
