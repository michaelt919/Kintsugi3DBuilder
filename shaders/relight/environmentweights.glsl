#ifndef ENVIRONMENT_WEIGHTS_GLSL
#define ENVIRONMENT_WEIGHTS_GLSL

#include "../colorappearance/colorappearance.glsl"
#include "../colorappearance/svd_unpack.glsl"
#include "reflectanceequations.glsl"
#include "environment.glsl"

#line 10 3006

#ifndef EIGENTEXTURE_RETURN_COUNT
#define EIGENTEXTURE_RETURN_COUNT 4
#endif

#ifndef SECONDARY_EIGENTEXTURE_COUNT
#define SECONDARY_EIGENTEXTURE_COUNT 2
#endif

#ifndef RAY_POSITION_JITTER
#define RAY_POSITION_JITTER 0.1
#endif

struct EnvironmentResult
{
    vec4 baseFresnel;
    vec4 fresnelAdjustment;
};

EnvironmentResult getWeights(vec3 base, float fresnelFactor, float weight, int virtualIndex, int svIndex)
{
    // Light intensities in view set files are assumed to be pre-divided by pi.
    // Or alternatively, the result of getLinearColor gives a result
    // where a diffuse reflectivity of 1 is represented by a value of pi.
    // See diffusefit.glsl
    vec4 weights = computeSVDViewWeights(ivec3(computeBlockStart(fTexCoord, textureSize(eigentextures, 0).xy), virtualIndex), svIndex);

    vec4 mfdTimesRoughnessSq = vec4(weights.rgb, weights.w);

    if (mfdTimesRoughnessSq.w > 0.0)
    {
        vec3 unweightedSample = mfdTimesRoughnessSq.rgb * base;
        return EnvironmentResult(vec4(unweightedSample, 1.0 / (2.0 * PI)) * weight, vec4(fresnelFactor * unweightedSample, 1.0 / (2.0 * PI)) * weight);
    }
    else
    {
        return EnvironmentResult(vec4(0), vec4(0));
    }
}

EnvironmentResult[EIGENTEXTURE_RETURN_COUNT] computeSVDEnvironmentSamples(int virtualIndex, int startingSVIndex, vec3 position, vec3 normal, float roughness)
{
    EnvironmentResult[EIGENTEXTURE_RETURN_COUNT] results;

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

#if SHADOWS_ENABLED
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

        results[0].baseFresnel = vec4(0.5 * sampleBase, 1.0 / (2.0 * PI)) * weight;
        results[0].fresnelAdjustment = vec4(fresnelFactor * 0.5 * sampleBase, 1.0 / (2.0 * PI)) * weight;

#if EIGENTEXTURE_RETURN_COUNT > 1
        results[1] = getWeights(sampleBase, fresnelFactor, weight, virtualIndex, 0);

#if EIGENTEXTURE_RETURN_COUNT > 2
        for (int i = 0; i < SECONDARY_EIGENTEXTURE_COUNT; i++)
        {
            results[i + 2] = getWeights(sampleBase, fresnelFactor, weight, virtualIndex, startingSVIndex + i);
        }
#endif

#endif
    }
    else
    {
        for (int i = 0; i < EIGENTEXTURE_RETURN_COUNT; i++)
        {
            results[i].baseFresnel = vec4(0.0);
            results[i].fresnelAdjustment = vec4(0.0);
        }
    }

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
        EnvironmentResult[EIGENTEXTURE_RETURN_COUNT] samples = computeSVDEnvironmentSamples(i, startingSVIndex, position, normal, roughness);
        for (int j = 0; j < EIGENTEXTURE_RETURN_COUNT; j++)
        {
            sums[j].baseFresnel += samples[j].baseFresnel;
            sums[j].fresnelAdjustment += samples[j].fresnelAdjustment;
        }
    }

    EnvironmentResult[EIGENTEXTURE_RETURN_COUNT] results;

    for (int j = 0; j < EIGENTEXTURE_RETURN_COUNT; j++)
    {
        if (sums[j].baseFresnel.w > 0.0)
        {
            float normalizationFactor =
                VIEW_COUNT;                                 // better spatial consistency, worse directional consistency?
//                clamp(sums[j].baseFresnel.w, 0, 1000000.0); // Better directional consistency, worse spatial consistency?

            results[j].baseFresnel = sums[j].baseFresnel / normalizationFactor;
            results[j].fresnelAdjustment = sums[j].fresnelAdjustment / normalizationFactor;
        }
        else
        {
            results[j].baseFresnel = vec4(0.0);
            results[j].fresnelAdjustment = vec4(0.0);
        }
    }

    return results;
}

#endif