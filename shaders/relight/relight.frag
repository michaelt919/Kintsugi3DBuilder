#version 330
#extension GL_ARB_texture_query_lod : enable

/*
 *  Copyright (c) Michael Tetzlaff 2022
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out int fragObjectID;

uniform mat4 model_view;
uniform mat4 fullProjection;

#ifndef MATERIAL_EXPLORATION_MODE
#define MATERIAL_EXPLORATION_MODE 0
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

#ifndef RELIGHTING_ENABLED
#define RELIGHTING_ENABLED 1
#endif

#ifndef SPOTLIGHTS_ENABLED
#define SPOTLIGHTS_ENABLED 0
#endif

#ifndef SHADOWS_ENABLED
#define SHADOWS_ENABLED 0
#endif

#ifndef IMAGE_BASED_RENDERING_ENABLED
#define IMAGE_BASED_RENDERING_ENABLED 0
#endif

#ifndef FRESNEL_EFFECT_ENABLED
#define FRESNEL_EFFECT_ENABLED 0
#endif

#ifndef PHYSICALLY_BASED_MASKING_SHADOWING
#define PHYSICALLY_BASED_MASKING_SHADOWING 0
#endif

#ifndef ENVIRONMENT_ILLUMINATION_ENABLED
#define ENVIRONMENT_ILLUMINATION_ENABLED 1
#endif

#ifndef PRECOMPUTED_VIEW_WEIGHTS_ENABLED
#define PRECOMPUTED_VIEW_WEIGHTS_ENABLED 0
#endif

#ifndef VIRTUAL_LIGHT_COUNT
#if RELIGHTING_ENABLED
#define VIRTUAL_LIGHT_COUNT 4
#else
#define VIRTUAL_LIGHT_COUNT 1
#endif
#endif

#ifndef ARCHIVING_2017_ENVIRONMENT_NORMALIZATION
#define ARCHIVING_2017_ENVIRONMENT_NORMALIZATION 0
#endif

#if MATERIAL_EXPLORATION_MODE

#include "../colorappearance/analytic.glsl"

#undef SMITH_MASKING_SHADOWING
#define SMITH_MASKING_SHADOWING 1

#undef DEFAULT_DIFFUSE_COLOR
#undef DEFAULT_SPECULAR_COLOR
#undef DEFAULT_SPECULAR_ROUGHNESS
#undef DIFFUSE_TEXTURE_ENABLED
#undef SPECULAR_TEXTURE_ENABLED
#undef ROUGHNESS_TEXTURE_ENABLED
#undef NORMAL_TEXTURE_ENABLED

#define DEFAULT_DIFFUSE_COLOR ANALYTIC_DIFFUSE_COLOR
#define DEFAULT_SPECULAR_COLOR ANALYTIC_SPECULAR_COLOR
#define DEFAULT_SPECULAR_ROUGHNESS vec3(ANALYTIC_ROUGHNESS)
#define DIFFUSE_TEXTURE_ENABLED 0
#define SPECULAR_TEXTURE_ENABLED 0
#define ROUGHNESS_TEXTURE_ENABLED 0
#define NORMAL_TEXTURE_ENABLED 1

#endif

#include "../colorappearance/textures.glsl"

#if !MATERIAL_EXPLORATION_MODE

uniform vec3 defaultDiffuseColor;

#ifndef DEFAULT_DIFFUSE_COLOR
#if !SPECULAR_TEXTURE_ENABLED && !IMAGE_BASED_RENDERING_ENABLED
#define DEFAULT_DIFFUSE_COLOR (defaultDiffuseColor)
#else
#define DEFAULT_DIFFUSE_COLOR (vec3(0.0))
#endif // !SPECULAR_TEXTURE_ENABLED && !IMAGE_BASED_RENDERING_ENABLED
#endif // DEFAULT_DIFFUSE_COLOR

#ifndef DEFAULT_SPECULAR_COLOR
#if DIFFUSE_TEXTURE_ENABLED && !IMAGE_BASED_RENDERING_ENABLED
#define DEFAULT_SPECULAR_COLOR (vec3(0.0))
#elif DIFFUSE_TEXTURE_ENABLED
#define DEFAULT_SPECULAR_COLOR (vec3(0.04))
#else
#define DEFAULT_SPECULAR_COLOR (vec3(0.04))
#endif // DIFFUSE_TEXTURE_ENABLED && !IMAGE_BASED_RENDERING_ENABLED
#endif // DEFAULT_SPECULAR_COLOR

#ifndef DEFAULT_SPECULAR_ROUGHNESS
#define DEFAULT_SPECULAR_ROUGHNESS (vec3(0.1)); // TODO pass in a default?
#endif

#endif // !MATERIAL_EXPLORATION_MODE

#ifndef MIPMAPS_ENABLED
#define MIPMAPS_ENABLED !BUEHLER_ALGORITHM
#endif

#ifndef DISCRETE_DIFFUSE_ENVIRONMENT
#define DISCRETE_DIFFUSE_ENVIRONMENT 1
#endif

#ifndef SMITH_MASKING_SHADOWING
#if ROUGHNESS_TEXTURE_ENABLED
#define SMITH_MASKING_SHADOWING 1
#endif
#endif

#include "reflectanceequations.glsl"
#include "tonemap.glsl"

#if RELIGHTING_ENABLED && ENVIRONMENT_ILLUMINATION_ENABLED
#include "environment.glsl"
#endif

uniform vec3 viewPos;

#include "../colorappearance/colorappearance.glsl"

#if IMAGE_BASED_RENDERING_ENABLED

#if !MATERIAL_EXPLORATION_MODE
#include "../colorappearance/imgspace.glsl"
#endif

#if BUEHLER_ALGORITHM
#define SORTING_TOTAL_COUNT VIEW_COUNT
#include "sort.glsl"
#endif

#endif

#line 182 0

uniform int objectID;
uniform vec3 holeFillColor;

#if VIRTUAL_LIGHT_COUNT > 0

uniform vec3 lightIntensityVirtual[VIRTUAL_LIGHT_COUNT];

#if RELIGHTING_ENABLED
uniform vec3 lightPosVirtual[VIRTUAL_LIGHT_COUNT];
uniform vec3 lightOrientationVirtual[VIRTUAL_LIGHT_COUNT];

#if SPOTLIGHTS_ENABLED
uniform float lightSpotSizeVirtual[VIRTUAL_LIGHT_COUNT];
uniform float lightSpotTaperVirtual[VIRTUAL_LIGHT_COUNT];
#endif // SPOTLIGHTS_ENABLED

#if SHADOWS_ENABLED
uniform sampler2DArray shadowMaps;
uniform mat4 lightMatrixVirtual[VIRTUAL_LIGHT_COUNT];
#endif // SHADOWS_ENABLED

#endif // RELIGHTING_ENABLED

#endif // VIRTUAL_LIGHT_COUNT > 0

#if !BUEHLER_ALGORITHM
uniform float weightExponent;
uniform float isotropyFactor;
#endif

#if PRECOMPUTED_VIEW_WEIGHTS_ENABLED
layout(std140) uniform ViewWeights
{
    vec4 viewWeights[VIEW_COUNT_DIV_4];
};

float getViewWeight(int viewIndex)
{
    return extractComponentByIndex(viewWeights[viewIndex/4], viewIndex%4);
}
#endif

struct Material
{
    vec3 diffuseColor;
    vec3 specularColor;
    vec3 roughnessRGB;
    vec3 roughnessRGBSq;
    float roughness;
    float roughnessSq;
};

#if IMAGE_BASED_RENDERING_ENABLED

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

#if RELIGHTING_ENABLED && ENVIRONMENT_ILLUMINATION_ENABLED

struct EnvironmentSample
{
    vec4 sampleResult;
    float sampleBRDF;
    float sampleWeight;
};

EnvironmentSample computeEnvironmentSample(int virtualIndex, vec3 normalDir, Material m, float maxLuminance)
{
    mat4 cameraPose = getCameraPose(virtualIndex);
    vec3 fragmentPos = (cameraPose * vec4(fPosition, 1.0)).xyz;
    vec3 normalDirCameraSpace = normalize((cameraPose * vec4(normalDir, 0.0)).xyz);
    vec3 sampleViewDir = normalize(-fragmentPos);
    float nDotV_sample = max(0, dot(normalDirCameraSpace, sampleViewDir));

    // All in camera space
    vec3 sampleLightDirUnnorm = lightPositions[getLightIndex(virtualIndex)].xyz - fragmentPos;
    float lightDistSquared = dot(sampleLightDirUnnorm, sampleLightDirUnnorm);
    vec3 sampleLightDir = sampleLightDirUnnorm * inversesqrt(lightDistSquared);
    vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
    vec3 lightIntensity = getLightIntensity(virtualIndex);

#if !INFINITE_LIGHT_SOURCES
    lightIntensity /= lightDistSquared;
#endif

    float nDotL_sample = max(0, dot(normalDirCameraSpace, sampleLightDir));
    float nDotH = max(0, dot(normalDirCameraSpace, sampleHalfDir));
    float hDotV_sample = max(0, dot(sampleHalfDir, sampleViewDir));

    vec3 diffuseContrib = m.diffuseColor * nDotL_sample * lightIntensity;

    float geomAttenSample = geom(m.roughness, nDotH, nDotV_sample, nDotL_sample, hDotV_sample);

    vec3 virtualViewDir =
        normalize((cameraPose * vec4(viewPos, 1.0)).xyz - fragmentPos);
    vec3 virtualLightDir = -reflect(virtualViewDir, sampleHalfDir);
    float nDotL_virtual = max(0, dot(normalDirCameraSpace, virtualLightDir));
    float nDotV_virtual = max(0.125, dot(normalDirCameraSpace, virtualViewDir));
    float hDotV_virtual = max(0, dot(sampleHalfDir, virtualViewDir));
    float geomAttenVirtual = geom(m.roughness, nDotH, nDotV_virtual, nDotL_virtual, hDotV_virtual);

    vec3 mfdFresnel;
    float mfdMono;
    vec3 mfdNewFresnel;
    float validSpecular = 1.0;

    if (nDotV_sample <= 0.0 || nDotL_sample <= 0.0 || geomAttenSample <= 0.0)
    {
        mfdFresnel = vec3(0.0);
        mfdMono = 0.0;
        mfdNewFresnel = vec3(0.0);
        validSpecular = 0.0;
    }
    else
    {
        vec4 sampleColor = getLinearColor(virtualIndex);

        if (sampleColor.a == 0.0)
        {
            //mfdFresnel = distTimesPi(nDotH, m.roughnessRGB) * m.specularColor / PI;
            mfdFresnel = vec3(0);
        }
        else
        {
            vec4 specularResid = removeDiffuse(sampleColor, diffuseContrib, nDotL_sample, maxLuminance);

            // Light intensities in view set files are assumed to be pre-divided by pi.
            // Or alternatively, the result of getLinearColor gives a result
            // where a diffuse reflectivity of 1 is represented by a value of pi.
            // See diffusefit.glsl
            vec3 mfdFresnelGeom = specularResid.rgb / (lightIntensity * PI);

#if PHYSICALLY_BASED_MASKING_SHADOWING
            mfdFresnel = mfdFresnelGeom * 4 * nDotV_sample / geomAttenSample;
#else
            mfdFresnel = mfdFresnelGeom * 4 / nDotL_sample;
#endif
        }

        mfdMono = getLuminance(mfdFresnel / m.specularColor);

#if FRESNEL_EFFECT_ENABLED
        mfdNewFresnel = fresnel(mfdFresnel, vec3(mfdMono), hDotV_virtual);
#else
        mfdNewFresnel = mfdFresnel;
#endif
    }

    vec3 cosineWeightedBRDF = mfdNewFresnel * geomAttenVirtual / (4 * nDotV_virtual);

#if DISCRETE_DIFFUSE_ENVIRONMENT
    cosineWeightedBRDF += m.diffuseColor * nDotL_virtual / PI;
#endif

    float weight = 4 * hDotV_virtual * (getCameraWeight(virtualIndex) * 4 * PI * VIEW_COUNT);

    vec4 unweightedSample;
    unweightedSample.rgb = cosineWeightedBRDF
//        * getEnvironment(fPosition, transpose(mat3(cameraPose)) * virtualLightDir,
//            4 * hDotV_virtual * getCameraWeight(virtualIndex));
        * getEnvironment(fPosition, transpose(mat3(cameraPose)) * virtualLightDir);

#if SPECULAR_TEXTURE_ENABLED && ARCHIVING_2017_ENVIRONMENT_NORMALIZATION
    // Normalizes with respect to specular texture when available as described in our Archiving 2017 paper.
    unweightedSample.a = mfdMono * geomAttenVirtual / (4 * nDotV_virtual);
#else
    unweightedSample.a = sign(validSpecular) * nDotL_virtual / PI;
#endif

    return EnvironmentSample(unweightedSample * weight, mfdMono, weight);
    // dl = 4 * h dot v * dh
    // weight * VIEW_COUNT -> brings weights back to being on the order of 1
    // This is helpful for consistency with numerical limits (i.e. clamping)
    // Everything gets normalized at the end again anyways.
}

vec3 getEnvironmentShading(vec3 normalDir, Material m)
{
#if MATERIAL_EXPLORATION_MODE
    float maxLuminance = max(ANALYTIC_SPECULAR_COLOR.r, max(ANALYTIC_SPECULAR_COLOR.g, ANALYTIC_SPECULAR_COLOR.b))
            / (4 * ANALYTIC_ROUGHNESS * ANALYTIC_ROUGHNESS)
        + max(ANALYTIC_DIFFUSE_COLOR.r, max(ANALYTIC_DIFFUSE_COLOR.g, ANALYTIC_DIFFUSE_COLOR.b));
#else
    float maxLuminance = getMaxLuminance();
#endif

    vec4 sum = vec4(0.0);

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        EnvironmentSample envSample = computeEnvironmentSample(i, normalDir, m, maxLuminance);
        sum += envSample.sampleResult;
    }

    if (sum.a > 0.0)
    {
        return sum.rgb
        //    / VIEW_COUNT;
            / sum.a;
    }
    else
    {
        return vec3(0.0);
    }
}

#endif // RELIGHTING_ENABLED && ENVIRONMENT_ILLUMINATION_ENABLED

#if BUEHLER_ALGORITHM

vec4 computeSampleSingle(int virtualIndex, vec3 normalDir, Material m, float maxLuminance)
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

    if (nDotL > 0 && nDotV > 0)
    {
        float geomAtten = geom(m.roughness, nDotH, nDotV, nDotL, hDotV);

        vec4 sampleColor = getLinearColor(virtualIndex);
        if (sampleColor.a > 0.0)
        {
            vec3 lightIntensity = getLightIntensity(virtualIndex);

#if !INFINITE_LIGHT_SOURCES
            lightIntensity /= lightDistSquared;
#endif

            vec3 diffuseContrib = m.diffuseColor * nDotL * lightIntensity;

#if PHYSICALLY_BASED_MASKING_SHADOWING
            vec4 specularResid = removeDiffuse(sampleColor, diffuseContrib, nDotL, maxLuminance);
            return sampleColor.a * vec4(specularResid.rgb * 4 * nDotV / lightIntensity, geomAtten);
#else
            vec4 specularResid = removeDiffuse(sampleColor, diffuseContrib, nDotL, maxLuminance);
            return sampleColor.a * vec4(specularResid.rgb * 4 / lightIntensity, nDotL);
#endif
        }
    }

    return vec4(0.0);
}

vec4 computeBuehler(vec3 targetDirection, vec3 normalDir, Material m)
{
#if MATERIAL_EXPLORATION_MODE
    float maxLuminance = max(ANALYTIC_SPECULAR_COLOR.r, max(ANALYTIC_SPECULAR_COLOR.g, ANALYTIC_SPECULAR_COLOR.b))
            / (4 * ANALYTIC_ROUGHNESS * ANALYTIC_ROUGHNESS)
        + max(ANALYTIC_DIFFUSE_COLOR.r, max(ANALYTIC_DIFFUSE_COLOR.g, ANALYTIC_DIFFUSE_COLOR.b));
#else
    float maxLuminance = getMaxLuminance();
#endif

    float weights[SORTING_SAMPLE_COUNT];
    int indices[SORTING_SAMPLE_COUNT];

    sort(targetDirection, weights, indices);

    float analyticWeight = 1.0 / (1.0 - clamp(dot(targetDirection, normalDir), 0.0, 0.999));

    float maxSampleLuminance = 0.0;

    vec4 samples[SORTING_SAMPLE_COUNT - 1];
    for (int i = 1; i < SORTING_SAMPLE_COUNT; i++)
    {
        samples[i - 1] = computeSampleSingle(indices[i], normalDir, m, maxLuminance);
        maxSampleLuminance = max(maxSampleLuminance, getLuminance(samples[i - 1].rgb));
    }

    // Evaluate the light field
    // weights[0] should be the smallest weight
    vec4 sum = vec4(0.0);
    float maxWeight = 0;
    for (int i = 0; i < SORTING_SAMPLE_COUNT - 1; i++)
    {
        if (samples[i].a > 0)
        {
            sum += (weights[i + 1] - weights[0]) * samples[i];

            maxWeight = max(maxWeight, weights[i + 1]);
        }
    }

    if (sum.a == 0.0)
    {
        return vec4(holeFillColor, 1.0);
    }
    else
    {
        return vec4(sum.rgb / sum.a, clamp(1.0 / max(1.0, 1.0 + analyticWeight - weights[0]) /*(maxWeight - analyticWeight) / (maxWeight - weights[0])*/, 0, 1));
    }
}

#elif VIRTUAL_LIGHT_COUNT > 0

vec4[VIRTUAL_LIGHT_COUNT] computeSample(int virtualIndex, vec3 normalDir, Material m, float maxLuminance)
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

    float geomAtten = geom(m.roughness, nDotH, nDotV, nDotL, hDotV);

    vec4 sampleColor = getLinearColor(virtualIndex);
    if (sampleColor.a > 0.0)
    {
        vec3 lightIntensity = getLightIntensity(virtualIndex);

#if !INFINITE_LIGHT_SOURCES
        lightIntensity /= lightDistSquared;
#endif

        vec3 diffuseContrib = m.diffuseColor * nDotL * lightIntensity;

#if PHYSICALLY_BASED_MASKING_SHADOWING
        if (geomAtten > 0.0)
        {
            vec4 specularResid = removeDiffuse(sampleColor, diffuseContrib, nDotL, maxLuminance);
            precomputedSample = sampleColor.a
                * vec4(specularResid.rgb * 4 * nDotV / lightIntensity, geomAtten);

        }
#else
        if (nDotL != 0.0)
        {
            vec4 specularResid = removeDiffuse(sampleColor, diffuseContrib, nDotL, maxLuminance);
            precomputedSample = sampleColor.a
                * vec4(specularResid.rgb * 4 / lightIntensity, nDotL);
        }
#endif
    }

    if (precomputedSample.w != 0)
    {
        mat3 tangentToObject = mat3(1.0);

        vec3 virtualViewDir = normalize((cameraPose * vec4(viewPos, 1.0)).xyz - fragmentPos);

        vec4 result[VIRTUAL_LIGHT_COUNT];

        for (int lightPass = 0; lightPass < VIRTUAL_LIGHT_COUNT; lightPass++)
        {
#if PRECOMPUTED_VIEW_WEIGHTS_ENABLED
            result[lightPass] = getViewWeight(virtualIndex) * precomputedSample;
#else

            vec3 virtualLightDir;
#if RELIGHTING_ENABLED
            virtualLightDir = normalize((cameraPose * vec4(lightPosVirtual[lightPass], 1.0)).xyz - fragmentPos);
#else
            virtualLightDir = virtualViewDir + lightPositions[getLightIndex(virtualIndex)].xyz;
#endif

            // Compute sample weight
            vec3 virtualHalfDir = normalize(virtualViewDir + virtualLightDir);
            float virtualNdotH = max(0, dot(normalDirCameraSpace, virtualHalfDir));
            float correlation = isotropyFactor * (nDotH * virtualNdotH + sqrt(1 - nDotH*nDotH) * sqrt(1 - virtualNdotH*virtualNdotH))
                + (1 - isotropyFactor) * dot(virtualHalfDir, sampleHalfDir);
            float weight = 1.0 / max(0.000001, 1.0 - pow(max(0.0, correlation), weightExponent)) - 1.0;
            result[lightPass] = weight * precomputedSample;

#endif // PRECOMPUTED_VIEW_WEIGHTS_ENABLED
        }

        return result;
    }
    else
    {
        vec4 result[VIRTUAL_LIGHT_COUNT];
        for (int lightPass = 0; lightPass < VIRTUAL_LIGHT_COUNT; lightPass++)
        {
            result[lightPass] = vec4(0.0);
        }
        return result;
    }
}

vec4[VIRTUAL_LIGHT_COUNT] computeWeightedAverages(vec3 normalDir, Material m)
{
#if MATERIAL_EXPLORATION_MODE
    float maxLuminance = max(ANALYTIC_SPECULAR_COLOR.r, max(ANALYTIC_SPECULAR_COLOR.g, ANALYTIC_SPECULAR_COLOR.b))
            / (4 * ANALYTIC_ROUGHNESS * ANALYTIC_ROUGHNESS)
        + max(ANALYTIC_DIFFUSE_COLOR.r, max(ANALYTIC_DIFFUSE_COLOR.g, ANALYTIC_DIFFUSE_COLOR.b));
#else
    float maxLuminance = getMaxLuminance();
#endif

    vec4[VIRTUAL_LIGHT_COUNT] sums;
    for (int i = 0; i < VIRTUAL_LIGHT_COUNT; i++)
    {
        sums[i] = vec4(0.0);
    }

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        vec4[VIRTUAL_LIGHT_COUNT] microfacetSample =
            computeSample(i, normalDir, m, maxLuminance);

        for (int j = 0; j < VIRTUAL_LIGHT_COUNT; j++)
        {
            sums[j] += microfacetSample[j];
        }
    }

    vec4[VIRTUAL_LIGHT_COUNT] results;
    for (int i = 0; i < VIRTUAL_LIGHT_COUNT; i++)
    {
#if PRECOMPUTED_VIEW_WEIGHTS_ENABLED
        results[i] = sums[i];
#else
        if (sums[i].a > 0.0)
        {
            results[i] = vec4(sums[i].rgb / max(0.01, sums[i].a), sums[i].a);
        }
        else
        {
            results[i] = vec4(0.0);
        }
#endif
    }
    return results;
}

#endif // BUEHLER_ALGORITHM

#endif // IMAGE_BASED_RENDERING_ENABLED

Material getMaterial()
{
    Material m;

#if DIFFUSE_TEXTURE_ENABLED
    m.diffuseColor = pow(texture(diffuseMap, fTexCoord).rgb, vec3(gamma));
#else
    m.diffuseColor = DEFAULT_DIFFUSE_COLOR;
#endif

#if SPECULAR_TEXTURE_ENABLED
    m.specularColor = max(vec3(0.04), pow(texture(specularMap, fTexCoord).rgb, vec3(gamma)));
#else
    m.specularColor = DEFAULT_SPECULAR_COLOR;
#endif

#if ROUGHNESS_TEXTURE_ENABLED
    vec3 roughnessLookup = texture(roughnessMap, fTexCoord).rgb;
    vec3 sqrtRoughness = vec3(
        roughnessLookup.g + roughnessLookup.r - 16.0 / 31.0,
        roughnessLookup.g,
        roughnessLookup.g + roughnessLookup.b - 16.0 / 31.0);
    m.roughnessRGB = sqrtRoughness * sqrtRoughness;
#else
    m.roughnessRGB = DEFAULT_SPECULAR_ROUGHNESS;
#endif

    m.roughnessRGBSq = m.roughnessRGB * m.roughnessRGB;
    m.roughnessSq = getLuminance(m.specularColor) / (getLuminance(m.specularColor / m.roughnessRGBSq));
    m.roughness = sqrt(m.roughnessSq);

    return m;
}

vec3 getRefinedNormalDir(vec3 triangleNormal)
{
#if TANGENT_SPACE_NORMAL_MAP && NORMAL_TEXTURE_ENABLED
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(triangleNormal, fBitangent) * triangleNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);
#endif

    vec3 normalDir;
#if NORMAL_TEXTURE_ENABLED
#if MATERIAL_EXPLORATION_MODE
    vec2 scaledTexCoord = ANALYTIC_UV_SCALE * fTexCoord;
    vec3 normalDirTS = normalize(getNormal(scaledTexCoord - floor(scaledTexCoord)) * vec3(ANALYTIC_BUMP_HEIGHT, ANALYTIC_BUMP_HEIGHT, 1.0));
    normalDir = tangentToObject * normalDirTS;
#elif TANGENT_SPACE_NORMAL_MAP
    vec2 normalDirXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    normalDir = tangentToObject * normalDirTS;
#else
    normalDir = texture(normalMap, fTexCoord).xyz * 2 - vec3(1.0);
#endif // TANGENT_SPACE_NORMAL_MAP
#else
    normalDir = triangleNormal;
#endif // NORMAL_TEXTURE_ENABLED

    return normalDir;
}

#if SHADOW_MAPS
bool shadowTest(int lightIndex)
{
    vec4 projTexCoord = lightMatrixVirtual[lightIndex] * vec4(fPosition, 1.0);
    projTexCoord /= projTexCoord.w;
    projTexCoord = (projTexCoord + vec4(1)) / 2;

    return (projTexCoord.x >= 0 && projTexCoord.x <= 1
        && projTexCoord.y >= 0 && projTexCoord.y <= 1
        && projTexCoord.z >= 0 && projTexCoord.z <= 1
        && texture(shadowMaps, vec3(projTexCoord.xy, lightIndex)).r - projTexCoord.z >= -0.01);
}
#endif

void main()
{
    vec3 triangleNormal = normalize(fNormal);
    vec3 normalDir = getRefinedNormalDir(triangleNormal);

    vec3 viewDir = normalize(viewPos - fPosition);
    float nDotV_triangle = dot(triangleNormal, viewDir);

    // Flip normals if necessary, but don't do anything if nDotV is zero.
    // This is required for the ground plane.
    float flip = (sign(nDotV_triangle) + 1 - abs(sign(nDotV_triangle)));
    triangleNormal *= flip;
        normalDir *= flip;
    nDotV_triangle = abs(nDotV_triangle);

// TODO: is it desirable for the primary object (not the groudn plane) to NOT shade backfacing polygons?
//    if (nDotV_triangle == 0.0)
//    {
//        fragColor = vec4(0, 0, 0, 1);
//        return;
//    }

    float nDotV = max(0.0, dot(normalDir, viewDir));

    Material m = getMaterial();

    vec3 radiance = vec3(0.0);

#if RELIGHTING_ENABLED && ENVIRONMENT_ILLUMINATION_ENABLED

#if IMAGE_BASED_RENDERING_ENABLED
#if !DISCRETE_DIFFUSE_ENVIRONMENT
    radiance += m.diffuseColor * getEnvironmentDiffuse(normalDir);
#endif
    radiance += getEnvironmentShading(normalDir, m);
#else
    radiance += getEnvironmentDiffuse(normalDir) * min(vec3(1.0), m.diffuseColor + m.specularColor);
#endif // IMAGE_BASED_RENDERING_ENABLED

#endif // RELIGHTING_ENABLED && ENVIRONMENT_ILLUMINATION_ENABLED

#if VIRTUAL_LIGHT_COUNT > 0

#if IMAGE_BASED_RENDERING_ENABLED && !BUEHLER_ALGORITHM
    vec4[VIRTUAL_LIGHT_COUNT] weightedAverages = computeWeightedAverages(normalDir, m);
#endif

    for (int i = 0; i < VIRTUAL_LIGHT_COUNT; i++)
    {
        vec3 lightDirUnNorm;
        vec3 lightDir;
        float nDotL;
#if RELIGHTING_ENABLED
        lightDirUnNorm = lightPosVirtual[i] - fPosition;
        lightDir = normalize(lightDirUnNorm);
        nDotL = max(0.0, dot(normalDir, lightDir));
#elif IMAGE_BASED_RENDERING_ENABLED
        lightDirUnNorm = transpose(mat3(model_view)) * lightPositions[getLightIndex(0)].xyz + viewPos - fPosition;
        lightDir = normalize(lightDirUnNorm);
        nDotL = max(0.0, dot(normalDir, lightDir));
#else
        lightDirUnNorm = transpose(mat3(model_view)) * lightPositions[getLightIndex(0)].xyz + viewPos - fPosition;
        lightDir = normalize(lightDirUnNorm);
        nDotL = max(0.0, dot(normalDir, viewDir));
#endif

        if (nDotL > 0.0 && dot(triangleNormal, lightDir) > 0.0)
        {
#if RELIGHTING_ENABLED && SHADOWS_ENABLED
            vec4 projTexCoord = lightMatrixVirtual[i] * vec4(fPosition, 1.0);
            projTexCoord /= projTexCoord.w;
            projTexCoord = (projTexCoord + vec4(1)) / 2;

            float depth = clamp(projTexCoord.z, 0, 1);
            float shadowMapDepth = texture(shadowMaps, vec3(projTexCoord.xy, i)).r;

            if (projTexCoord.x >= 0 && projTexCoord.x <= 1
                    && projTexCoord.y >= 0 && projTexCoord.y <= 1
                    && shadowMapDepth - depth >= -0.001)
#endif
            {
                vec3 halfDir = normalize(viewDir + lightDir);
                float hDotV = dot(halfDir, viewDir);
                float nDotH = dot(normalDir, halfDir);
                float nDotHSq = max(0, nDotH) * max(0, nDotH);

                vec4 predictedMFD;

#if IMAGE_BASED_RENDERING_ENABLED

#if BUEHLER_ALGORITHM
                predictedMFD = computeBuehler(halfDir, normalDir, m);
#else
                predictedMFD = weightedAverages[i];
#endif // BUEHLER_ALGORITHM

#endif // IMAGE_BASED_RENDERING_ENABLED

                vec3 mfdFresnel;

#if RELIGHTING_ENABLED && FRESNEL_EFFECT_ENABLED
                vec3 mfdFresnelBase = m.specularColor * distTimesPi(nDotH, m.roughnessRGB);
                vec3 mfdFresnelAnalytic = fresnel(mfdFresnelBase, vec3(getLuminance(mfdFresnelBase) / getLuminance(m.specularColor)), hDotV);

#if IMAGE_BASED_RENDERING_ENABLED
                float grazingIntensity = getLuminance(max(vec3(0.0), predictedMFD.rgb) / m.specularColor);

                mfdFresnel = max(vec3(0.0), fresnel(predictedMFD.rgb, vec3(grazingIntensity), hDotV));

#else
                mfdFresnel = mfdFresnelAnalytic;

#endif // IMAGE_BASED_RENDERING_ENABLED

#else
                vec3 mfdFresnelAnalytic = m.specularColor * distTimesPi(nDotH, m.roughnessRGB);

#if IMAGE_BASED_RENDERING_ENABLED
                mfdFresnel = max(vec3(0.0), predictedMFD.rgb);
#else
                mfdFresnel = mfdFresnelAnalytic;
#endif // IMAGE_BASED_RENDERING_ENABLED

#endif // RELIGHTING_ENABLED && FRESNEL_EFFECT_ENABLED

                vec3 lightVectorTransformed = (model_view * vec4(lightDirUnNorm, 0.0)).xyz;

                vec3 pointRadiance;

                vec3 reflectance = nDotL * m.diffuseColor;

#if PHYSICALLY_BASED_MASKING_SHADOWING
                reflectance += mfdFresnel * geom(m.roughness, nDotH, nDotV, nDotL, hDotV) / (4 * nDotV);
#else
                reflectance += mfdFresnel * nDotL / 4;
#endif

#if RELIGHTING_ENABLED
                vec3 irradiance = lightIntensityVirtual[i] / dot(lightVectorTransformed, lightVectorTransformed);

#if SPOTLIGHTS_ENABLED
                float lightDirCorrelation = max(0.0, dot(lightDir, -lightOrientationVirtual[i]));
                float spotBoundaryDistance = lightSpotSizeVirtual[i] - sqrt(1 - lightDirCorrelation * lightDirCorrelation);
                irradiance *= clamp(
                    spotBoundaryDistance / max(0.001, max(lightSpotSizeVirtual[i] * lightSpotTaperVirtual[i], spotBoundaryDistance)),
                    0.0, 1.0);
#endif // RELIGHTING_ENABLED && SPOTLIGHTS_ENABLED

                pointRadiance = reflectance * irradiance;
#else
                pointRadiance = reflectance;
#endif
                radiance += pointRadiance;
            }
        }
    }

#endif // VIRTUAL_LIGHT_COUNT > 0

    fragColor = tonemap(radiance, 1.0);

    fragObjectID = objectID;
}
