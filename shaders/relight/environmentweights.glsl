/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#ifndef ENVIRONMENT_WEIGHTS_GLSL
#define ENVIRONMENT_WEIGHTS_GLSL

#include "../colorappearance/colorappearance.glsl"
#include "../colorappearance/svd_unpack.glsl"
#include "reflectanceequations.glsl"
#include "environment.glsl"

#line 22 3006

#ifndef EIGENTEXTURE_RETURN_COUNT
#define EIGENTEXTURE_RETURN_COUNT 4
#endif

#ifndef SECONDARY_EIGENTEXTURE_COUNT
#define SECONDARY_EIGENTEXTURE_COUNT 2
#endif

#ifndef RAY_POSITION_JITTER
#define RAY_POSITION_JITTER 0.1
#endif

#ifndef SHADOW_JITTER_ENABLED
#define SHADOW_JITTER_ENABLED 1
#endif

struct EnvironmentResult
{
    vec4 baseFresnel;
    vec4 fresnelAdjustment;
};

vec4 getSVDViewWeight(int virtualIndex, int svIndex)
{
    // Light intensities in view set files are assumed to be pre-divided by pi.
    // Or alternatively, the result of getLinearColor gives a result
    // where a diffuse reflectivity of 1 is represented by a value of pi.
    // See diffusefit.glsl
    return computeSVDViewWeights(ivec3(computeBlockStart(fTexCoord, textureSize(eigentextures, 0).xy), virtualIndex), svIndex);
}

EnvironmentResult computeEnvironmentSample(int virtualIndex, vec3 position, vec3 normal, float roughness)
{
    mat4 cameraPose = getCameraPose(virtualIndex);
    vec3 fragmentPos = (cameraPose * vec4(position, 1.0)).xyz;
    vec3 normalDirCameraSpace = mat3(cameraPose) * normal;
    vec3 sampleViewDir = normalize(-fragmentPos);

    // All in camera space
    vec3 sampleLightDirUnnorm = lightPositions[getLightIndex(virtualIndex)].xyz - fragmentPos;
    float lightDistSquared = dot(sampleLightDirUnnorm, sampleLightDirUnnorm);
    vec3 sampleLightDir = sampleLightDirUnnorm * inversesqrt(lightDistSquared);

    vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);

    vec3 virtualViewDir = normalize((cameraPose * vec4(viewPos, 1.0)).xyz - fragmentPos);

    float nDotV_sample = max(0, dot(normalDirCameraSpace, sampleViewDir));
    float nDotL_sample = max(0, dot(normalDirCameraSpace, sampleLightDir));
    float nDotV_virtual = max(0, dot(normalDirCameraSpace, virtualViewDir));

    if (nDotV_sample > 0.0 && nDotL_sample > 0.0 && nDotV_virtual > 0.0)
    {
        vec3 virtualLightDir = -reflect(virtualViewDir, sampleHalfDir);
        float nDotL_virtual = max(0, dot(normalDirCameraSpace, virtualLightDir));

        float hDotV_virtual = max(0, dot(sampleHalfDir, virtualViewDir));

        float oneMinusHDotV = max(0, 1.0 - hDotV_virtual);
        float oneMinusHDotVSq = oneMinusHDotV * oneMinusHDotV;
        float fresnelFactor = oneMinusHDotVSq * oneMinusHDotVSq * oneMinusHDotV;

        float sampleMaskingShadowing;
        float virtualMaskingShadowing;
        float anticipatedFakeMaskingShadowing;

#if PHYSICALLY_BASED_MASKING_SHADOWING
        sampleMaskingShadowing = computeGeometricAttenuationHeightCorrelatedSmith(roughness, nDotV_sample, nDotL_sample);
        virtualMaskingShadowing = computeGeometricAttenuationHeightCorrelatedSmith(roughness, nDotV_virtual, nDotL_virtual);
        anticipatedFakeMaskingShadowing = computeGeometricAttenuationHeightCorrelatedSmith(roughness, nDotV_virtual, 1.0);
#else
        sampleMaskingShadowing = nDotL_sample * nDotV_sample;
        virtualMaskingShadowing = nDotL_virtual * nDotV_virtual;
        anticipatedFakeMaskingShadowing = nDotV_virtual * nDotV_virtual;
#endif

        vec3 sampleBase = vec3(0.25 / PI
            * virtualMaskingShadowing
            * nDotV_sample / sampleMaskingShadowing);

#if SHADOWS_ENABLED && SHADOW_JITTER_ENABLED
        sampleBase *= (getEnvironment(position, transpose(mat3(cameraPose)) * virtualLightDir) +
            getEnvironment(position + transpose(mat3(cameraPose)) * vec3(RAY_POSITION_JITTER,0,0), transpose(mat3(cameraPose)) * virtualLightDir) +
            getEnvironment(position + transpose(mat3(cameraPose)) * vec3(-RAY_POSITION_JITTER,0,0), transpose(mat3(cameraPose)) * virtualLightDir) +
            getEnvironment(position + transpose(mat3(cameraPose)) * vec3(0,RAY_POSITION_JITTER,0), transpose(mat3(cameraPose)) * virtualLightDir) +
            getEnvironment(position + transpose(mat3(cameraPose)) * vec3(0,-RAY_POSITION_JITTER,0), transpose(mat3(cameraPose)) * virtualLightDir)) / 5;
#else
        sampleBase *= getEnvironment(position, transpose(mat3(cameraPose)) * virtualLightDir);
#endif

        float weight = 4 * hDotV_virtual * (getCameraWeight(virtualIndex) * 4 * PI * VIEW_COUNT);
        // dl = 4 * h dot v * dh
        // cameraWeight * VIEW_COUNT -> brings weights back to being on the order of 1
        // This is helpful for consistency with numerical limits (i.e. clamping)
        // Everything gets normalized at the end again anyways.

        return EnvironmentResult(vec4(sampleBase, 1.0 / (2.0 * PI)) * weight, vec4(fresnelFactor * sampleBase, 1.0 / (2.0 * PI)) * weight);
    }
    else
    {
        return EnvironmentResult(vec4(0.0), vec4(0.0));
    }
}

vec4[EIGENTEXTURE_RETURN_COUNT] computeAllSVDWeights(int virtualIndex, int startingSVIndex)
{
    vec4[EIGENTEXTURE_RETURN_COUNT] results;

    results[0] = vec4(0.5, 0.5, 0.5, 1.0);

#if EIGENTEXTURE_RETURN_COUNT > 1

    vec4 svdViewWeight = getSVDViewWeight(virtualIndex, 0);
    results[1] = vec4(svdViewWeight.rgb, sign(svdViewWeight.a));

#if EIGENTEXTURE_RETURN_COUNT > 2
    for (int i = 0; i < SECONDARY_EIGENTEXTURE_COUNT; i++)
    {
        svdViewWeight = getSVDViewWeight(virtualIndex, startingSVIndex + i);
        results[i + 2] = vec4(svdViewWeight.rgb, sign(svdViewWeight.a));
    }
#endif

#endif

    return results;
}

EnvironmentResult[EIGENTEXTURE_RETURN_COUNT] computeSVDEnvironmentShading(int startingSVIndex, vec3 position, vec3 normal, float roughness)
{
    EnvironmentResult[EIGENTEXTURE_RETURN_COUNT] sums;

    for (int j = 0; j < EIGENTEXTURE_RETURN_COUNT; j++)
    {
        sums[j].baseFresnel = vec4(0.0);
        sums[j].fresnelAdjustment = vec4(0.0);
    }

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        vec4[EIGENTEXTURE_RETURN_COUNT] svdWeights = computeAllSVDWeights(i, startingSVIndex);

        EnvironmentResult environmentSample = computeEnvironmentSample(i, position, normal, roughness);

        for (int j = 0; j < EIGENTEXTURE_RETURN_COUNT; j++)
        {
            sums[j].baseFresnel += environmentSample.baseFresnel * svdWeights[j];
            sums[j].fresnelAdjustment += environmentSample.fresnelAdjustment * svdWeights[j];
        }
    }

    EnvironmentResult[EIGENTEXTURE_RETURN_COUNT] results;

    for (int j = 0; j < EIGENTEXTURE_RETURN_COUNT; j++)
    {
        float normalizationFactor =
            VIEW_COUNT;                                 // better spatial consistency, worse directional consistency?
//            clamp(sums[j].baseFresnel.w, 0, 1000000.0); // Better directional consistency, worse spatial consistency?

        results[j].baseFresnel = /*sign(sums[j].baseFresnel.w) * */sums[j].baseFresnel / normalizationFactor;
        results[j].fresnelAdjustment = /*sign(sums[j].baseFresnel.w) * */sums[j].fresnelAdjustment / normalizationFactor;
    }

    return results;
}

#endif