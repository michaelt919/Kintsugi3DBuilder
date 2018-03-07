#version 330
#extension GL_ARB_texture_query_lod : enable

//#define SVD_MODE

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out int fragObjectID;


#include "../colorappearance/colorappearance_subset.glsl"
#define MAX_SORTING_SAMPLE_COUNT 5
#define MAX_SORTING_TOTAL_COUNT MAX_CAMERA_POSE_COUNT

#include "reflectanceequations.glsl"
#include "environment.glsl"
#include "sort.glsl"
#include "tonemap.glsl"

#ifdef SVD_MODE
#include "../colorappearance/svd_unpack_subset.glsl"
#else
#include "../colorappearance/imgspace_subset.glsl"
#endif

#line 32 0

uniform int objectID;

uniform mat4 model_view;
uniform vec3 viewPos;
uniform mat4 envMapMatrix;

#define MAX_VIRTUAL_LIGHT_COUNT 4
uniform vec3 lightIntensityVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform vec3 lightPosVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform mat4 lightMatrixVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform int virtualLightCount;

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

#define interpolateRoughness false
#define brdfMode false

#ifdef SVD_MODE
#define residualImages true
#else
#define residualImages false
#endif

layout(std140) uniform ViewWeights
{
    vec4 viewWeights[MAX_CAMERA_POSE_COUNT_DIV_4];
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

#ifdef SVD_MODE
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
//        vec4 roughnessResid = getColor(virtualIndex);
//        if (roughnessResid.w > 0)
//        {
//            vec3 sqrtAdjustedRoughness = sqrt(roughness) + roughnessResid.xyz - vec3(0.5);
//            vec3 adjustedRoughness = sqrtAdjustedRoughness * sqrtAdjustedRoughness;
//            precomputedSample = vec4(xyzToRGB(dist(nDotH, adjustedRoughness)
//                * rgbToXYZ(specularColor) * adjustedRoughness * adjustedRoughness / (roughness * roughness)), 1.0);
//        }

        vec4 residual = getColor(virtualIndex);
        if (residual.w > 0)
        {
            vec3 roughnessSquared = roughness * roughness;
            precomputedSample = residual.w * vec4(xyzToRGB(

                pow(max(vec3(0),

                    pow(dist(nDotH, roughness) * roughnessSquared, vec3(1.0 / gamma))
//                    pow(rgbToXYZ(pow(texture(diffuseMap, fTexCoord).rgb, vec3(gamma))) * roughnessSquared / rgbToXYZ(specularColor) * 4 * nDotL * nDotV / geom(roughness, nDotH, nDotV, nDotL, hDotV), vec3(1 / gamma))

                    + (residual.xyz - vec3(0.5))

                ), vec3(gamma))

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

struct RoughnessSample
{
    vec3 weightedSqrtRoughness;
    vec3 weight;
};

vec4 computeRoughnessSampleSingle(int virtualIndex, vec3 diffuseColor, vec3 normalDir, vec3 specularColor, vec3 roughness, float maxLuminance)
{
    vec4 sampleColor = getColor(virtualIndex);
    mat4 cameraPose = getCameraPose(virtualIndex);

    if (sampleColor.a > 0.0)
    {
        // All in camera space
        vec3 fragmentPos = (cameraPose * vec4(fPosition, 1.0)).xyz;
        vec3 sampleViewDir = normalize(-fragmentPos);
        vec3 sampleLightDirUnnorm = lightPositions[getLightIndex(virtualIndex)].xyz - fragmentPos;
        float lightDistSquared = dot(sampleLightDirUnnorm, sampleLightDirUnnorm);
        vec3 sampleLightDir = sampleLightDirUnnorm * inversesqrt(lightDistSquared);
        vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
        vec3 normalDirCameraSpace = normalize((cameraPose * vec4(normalDir, 0.0)).xyz);

        float nDotL = max(0, dot(normalDirCameraSpace, sampleLightDir));
        float nDotV = max(0, dot(normalDirCameraSpace, sampleViewDir));
        float nDotH = max(0, dot(normalDirCameraSpace, sampleHalfDir));
        float hDotV = max(0, dot(sampleHalfDir, sampleViewDir));

        vec3 geomAtten = pbrGeometricAttenuationEnabled ? geom(roughness, nDotH, nDotV, nDotL, hDotV) : vec3(nDotL * nDotV);

        if (geomAtten != vec3(0))
        {
            float nDotHSq = nDotH * nDotH;
            vec4 roughnessSample;

            vec3 mfdFresnelEstimate = dist(nDotH, roughness) * rgbToXYZ(fresnel(specularColor, vec3(1), hDotV));

            if (residualImages)
            {
                roughnessSample = vec4(sqrt(roughness) + sampleColor.xyz - vec3(0.5), 1.0);
            }
            else
            {
                vec3 lightIntensity = getLightIntensity(virtualIndex);
                vec3 diffuseContrib = diffuseColor * nDotL * lightIntensity / (infiniteLightSources ? 1.0 : lightDistSquared);

                vec4 mfdFresnelSample;
                vec4 specularResid = removeDiffuse(linearizeColor(sampleColor), diffuseContrib, nDotL, maxLuminance);

                if(pbrGeometricAttenuationEnabled)
                {
                    mfdFresnelSample = vec4(specularResid.rgb
                        * 4 * nDotV / lightIntensity * (infiniteLightSources ? 1.0 : lightDistSquared),
                        sampleColor.a * geomAtten);
                }
                else
                {
                    mfdFresnelSample =
                        vec4(specularResid.rgb * 4 / lightIntensity
                            * (infiniteLightSources ? 1.0 : lightDistSquared),
                            sampleColor.a * nDotL);
                }

                vec3 denominator = max(vec3(0), sqrt(rgbToXYZ(specularColor) * mfdFresnelSample.a) / roughness
                                       - sqrt(rgbToXYZ(mfdFresnelSample.rgb)) * vec3(nDotHSq));

                roughnessSample = vec4(
                    sqrt(sqrt(sqrt(rgbToXYZ(mfdFresnelSample.rgb)) * vec3(1 - nDotHSq) * denominator.y / denominator)),
                    sqrt(sqrt(denominator.y)));
            }

            roughnessSample.xyz = clamp(roughnessSample.xyz, vec3(0), vec3(roughnessSample.w));
            return roughnessSample;
        }
        else
        {
            return vec4(0);
        }
    }
    else
    {
        return vec4(0);
    }
}

RoughnessSample[MAX_VIRTUAL_LIGHT_COUNT] computeRoughnessSample(int virtualIndex, vec3 diffuseColor, vec3 normalDir,
    vec3 specularColor, vec3 roughness, float maxLuminance)
{
    vec4 sampleColor = getColor(virtualIndex);
    mat4 cameraPose = getCameraPose(virtualIndex);

    if (sampleColor.a > 0.0)
    {
        RoughnessSample result[MAX_VIRTUAL_LIGHT_COUNT];

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

        vec3 geomAtten = pbrGeometricAttenuationEnabled ? geom(roughness, nDotH, nDotV, nDotL, hDotV) : vec3(nDotL * nDotV);

        if (geomAtten != vec3(0))
        {
            float nDotHSq = nDotH * nDotH;
            RoughnessSample precomputedSample;

            vec3 mfdFresnelEstimate = dist(nDotH, roughness) * rgbToXYZ(fresnel(specularColor, vec3(1), hDotV));

            if (residualImages)
            {
                vec3 sqrtRoughnessEstimate = sqrt(roughness) + sampleColor.xyz - vec3(0.5);
                vec3 denominator = max(vec3(0), sqrt(rgbToXYZ(specularColor) * geomAtten) / roughness - sqrt(mfdFresnelEstimate) * vec3(nDotHSq));
                precomputedSample = RoughnessSample(denominator * sqrtRoughnessEstimate, denominator);
            }
            else
            {
                vec4 mfdFresnelSample;
                vec4 specularResid = removeDiffuse(linearizeColor(sampleColor), diffuseContrib, nDotL, maxLuminance);

                if(pbrGeometricAttenuationEnabled)
                {
                    mfdFresnelSample = vec4(specularResid.rgb
                        * 4 * nDotV / lightIntensity * (infiniteLightSources ? 1.0 : lightDistSquared),
                        sampleColor.a * geomAtten);
                }
                else
                {
                    mfdFresnelSample =
                        vec4(specularResid.rgb * 4 / lightIntensity
                            * (infiniteLightSources ? 1.0 : lightDistSquared),
                            sampleColor.a * nDotL);
                }

                vec3 denominator = max(vec3(0), sqrt(rgbToXYZ(specularColor) * mfdFresnelSample.a) / roughness
                                       - sqrt(rgbToXYZ(mfdFresnelSample.rgb)) * vec3(nDotHSq));

                vec3 weight = max(vec3(0), sqrt(rgbToXYZ(specularColor) * mfdFresnelSample.a) / roughness
                                    - sqrt(mfdFresnelEstimate) * vec3(nDotHSq));

                precomputedSample = RoughnessSample(
                    sqrt(sqrt(sqrt(rgbToXYZ(mfdFresnelSample.rgb)) * vec3(1 - nDotHSq) / denominator)) * weight,
                    weight);
            }

            precomputedSample.weightedSqrtRoughness = clamp(precomputedSample.weightedSqrtRoughness, vec3(0), precomputedSample.weight);

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
//                if (perPixelWeightsEnabled)
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
                        isotropyFactor * (1.0 - (nDotH - virtualNdotH) * (nDotH - virtualNdotH)) +
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
                    result[lightPass] = RoughnessSample(weight * precomputedSample.weightedSqrtRoughness, weight * precomputedSample.weight);
                }
//                else
//                {
//                    result[lightPass] = getViewWeight(virtualIndex) * precomputedSample;
//                }
            }
        }
        else
        {
            for (int lightPass = 0; lightPass < MAX_VIRTUAL_LIGHT_COUNT; lightPass++)
            {
                result[lightPass] = RoughnessSample(vec3(0), vec3(0));
            }
        }

        return result;
    }
    else
    {
        RoughnessSample result[MAX_VIRTUAL_LIGHT_COUNT];
        for (int lightPass = 0; lightPass < MAX_VIRTUAL_LIGHT_COUNT; lightPass++)
        {
            result[lightPass] = RoughnessSample(vec3(0), vec3(0));
        }
        return result;
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

#define MIN_ROUGHNESS_WEIGHT 0.1

vec4[MAX_VIRTUAL_LIGHT_COUNT] computeWeightedRoughnessAverages(vec3 diffuseColor, vec3 normalDir, vec3 specularColor, vec3 roughness)
{
    float maxLuminance = getMaxLuminance();

    RoughnessSample[MAX_VIRTUAL_LIGHT_COUNT] sums;
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT; i++)
    {
        sums[i] = RoughnessSample(vec3(0), vec3(0));
    }

    for (int i = 0; i < viewCount; i++)
    {
        RoughnessSample[MAX_VIRTUAL_LIGHT_COUNT] microfacetSample =
            computeRoughnessSample(i, diffuseColor, normalDir, specularColor, roughness, maxLuminance);

        for (int j = 0; j < MAX_VIRTUAL_LIGHT_COUNT; j++)
        {
            sums[j].weightedSqrtRoughness += microfacetSample[j].weightedSqrtRoughness;
            sums[j].weight += microfacetSample[j].weight;
        }
    }

    vec3 sqrtRoughness = sqrt(roughness);

    vec4[MAX_VIRTUAL_LIGHT_COUNT] results;
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT; i++)
    {
//        if (!perPixelWeightsEnabled)
//        {
//            results[i] = sums[i];
//        }
//        else
        {

            vec3 sqrtLocalRoughness = (sums[i].weightedSqrtRoughness
                + max(vec3(0), MIN_ROUGHNESS_WEIGHT - sums[i].weight) * sqrtRoughness) / max(vec3(MIN_ROUGHNESS_WEIGHT), sums[i].weight);
            results[i] = vec4(sqrtLocalRoughness * sqrtLocalRoughness, 1);
        }
    }
    return results;
}


vec4 computeBuehler(vec3 targetDirection, vec3 diffuseColor, vec3 normalDir, vec3 specularColor, vec3 roughness)
{
    float maxLuminance = getMaxLuminance();

    float weights[MAX_SORTING_SAMPLE_COUNT];
    int indices[MAX_SORTING_SAMPLE_COUNT];

    int sampleCount = 5; // TODO change to a parameter

    sort(sampleCount, viewCount, targetDirection, weights, indices);

    // Evaluate the light field
    // weights[0] should be the smallest weight
    vec4 sum = vec4(0.0);
    for (int i = 1; i < sampleCount; i++)
    {
        vec4 computedSample = computeSample(indices[i], diffuseColor, normalDir, specularColor, roughness, maxLuminance)[0];
        if (computedSample.a > 0)
        {
            sum += (weights[i] - weights[0]) * computedSample / computedSample.a;
        }
    }

    return sum / sum.a;
}

vec3 computeRoughnessBuehler(vec3 targetDirection, vec3 diffuseColor, vec3 normalDir, vec3 specularColor, vec3 roughness)
{
    float maxLuminance = getMaxLuminance();

    float weights[MAX_SORTING_SAMPLE_COUNT];
    int indices[MAX_SORTING_SAMPLE_COUNT];

    int sampleCount = 5; // TODO change to a parameter

    sortFast(viewCount, targetDirection, weights, indices);

    // Evaluate the light field
    // weights[0] should be the smallest weight
    vec4 sum = vec4(0.0);
    for (int i = 1; i < sampleCount; i++)
    {
        vec4 computedSample = computeRoughnessSampleSingle(indices[i], diffuseColor, normalDir, specularColor, roughness, maxLuminance);
        sum += (weights[i] - weights[0]) * computedSample;
    }

    vec3 avg = sum.rgb / sum.a;
    return avg * avg;
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
    vec3 reflectance = vec3(0.0);

//    vec4[MAX_VIRTUAL_LIGHT_COUNT] weightedAverages;
//
//    if (imageBasedRenderingEnabled)
//    {
//        weightedAverages = interpolateRoughness ?
//            computeWeightedRoughnessAverages(diffuseColor, normalDir, specularColor, roughness) :
//            computeWeightedAverages(diffuseColor, normalDir, specularColor, roughness);
//    }

    if (relightingEnabled && ambientColor != vec3(0))
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
                    if (interpolateRoughness)
                    {
                        // Use Buehler algorithm
                        vec3 weightedAverage = computeRoughnessBuehler(
                            useTSOverrides ? tangentToObject * halfDir : halfDir,
                            diffuseColor, normalDir, specularColor, roughness);
                        // vec3 weightedAverage = weightedAverages[i].xyz;

                        vec3 localRoughnessSq = weightedAverage * weightedAverage;
                        vec3 mfdDenominatorRoot = 1 - nDotHSq * (1 - localRoughnessSq);
                        predictedMFD = vec4(max(vec3(0), xyzToRGB(localRoughnessSq * localRoughnessSq * specularColorXYZ
                            / (roughnessSq * mfdDenominatorRoot * mfdDenominatorRoot))), 25.0);
                    }
                    else
                    {
                        // Use Buehler algorithm
                        vec4 weightedAverage = computeBuehler(
                            useTSOverrides ? tangentToObject * halfDir : halfDir,
                            diffuseColor, normalDir, specularColor, roughness);
                        // vec4 weightedAverage = weightedAverages[i];

                        predictedMFD = weightedAverage
                            ;//+ (residualImages ? vec4(xyzToRGB(specularColorXYZ * dist(nDotH, roughness)), 0) : vec4(0));
                    }
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
                                // fresnel(weightedAverages[i].rgb, vec3(dist(nDotH, roughness)), hDotV));
                        }

//                        // DEBUG code:
//                        vec3 mfdFresnelBaseXYZ = rgbToXYZ(specularColor) * dist(nDotH, roughness);
//                        mfdFresnel = abs(mfdFresnel - fresnel(xyzToRGB(mfdFresnelBaseXYZ), vec3(mfdFresnelBaseXYZ.y), hDotV));

                        //reflectance = abs(pow(weightedAverages[i].xyz - roughness, vec3(2))); break;

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

                reflectance += (
                    nDotL * diffuseColor +
                    mfdFresnel
                     * (pbrGeometricAttenuationEnabled
                        ? geom(roughness, nDotH, nDotV, nDotL, hDotV) / (4 * nDotV) : vec3(nDotL / 4)))
                     * (useTSOverrides ? lightIntensityVirtual[i] :
                            lightIntensityVirtual[i] / dot(lightVectorTransformed, lightVectorTransformed))
                     / (brdfMode ? nDotL : 1.0);
            }
        }
    }

    fragColor = tonemap(reflectance, 1.0);

    fragObjectID = objectID;
}
