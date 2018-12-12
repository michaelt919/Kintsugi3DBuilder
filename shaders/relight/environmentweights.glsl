#ifndef ENVIRONMENT_WEIGHTS_GLSL
#define ENVIRONMENT_WEIGHTS_GLSL

#include "../colorappearance/colorappearance.glsl"
#include "../colorappearance/svd_unpack.glsl"
#include "reflectanceequations.glsl"
#include "environment.glsl"

#line 10 3006

#ifndef ACTIVE_EIGENTEXTURE_COUNT
#define ACTIVE_EIGENTEXTURE_COUNT 4
#endif

struct EnvironmentResult
{
    vec4 baseFresnel;
    vec4 fresnelAdjustment;
};

EnvironmentResult[ACTIVE_EIGENTEXTURE_COUNT] computeSVDEnvironmentSamples(int virtualIndex, int startingSVIndex, vec3 position, vec3 normal, float roughness)
{
    EnvironmentResult[ACTIVE_EIGENTEXTURE_COUNT] results;

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

        vec3 sampleBase = 0.25 / PI
            * virtualMaskingShadowing
            * nDotV_sample / sampleMaskingShadowing
            * getEnvironment(position, transpose(mat3(cameraPose)) * virtualLightDir);

        float weight = 4 * hDotV_virtual * (getCameraWeight(virtualIndex) * 4 * PI * VIEW_COUNT);
        // dl = 4 * h dot v * dh
        // cameraWeight * VIEW_COUNT -> brings weights back to being on the order of 1
        // This is helpful for consistency with numerical limits (i.e. clamping)
        // Everything gets normalized at the end again anyways.

        for (int i = 0; i < ACTIVE_EIGENTEXTURE_COUNT; i++)
        {
            int svIndex = startingSVIndex + i;

            if (svIndex == -1)
            {
                results[i].baseFresnel = vec4(0.5 * sampleBase, 1.0 / (2.0 * PI)) * weight;
                results[i].fresnelAdjustment = vec4(fresnelFactor * 0.5 * sampleBase, 1.0 / (2.0 * PI)) * weight;
            }
            else
            {
                // Light intensities in view set files are assumed to be pre-divided by pi.
                // Or alternatively, the result of getLinearColor gives a result
                // where a diffuse reflectivity of 1 is represented by a value of pi.
                // See diffusefit.glsl
                vec4 weights = computeSVDViewWeights(ivec3(computeBlockStart(fTexCoord, textureSize(eigentextures, 0).xy), virtualIndex), svIndex);

                vec4 mfdTimesRoughnessSq =
                    //vec4(yuvToRGB(vec3(0.5, 0.436, 0.615) * weights.rgb), weights.w);
                    vec4(weights.rgb, weights.w);

                if (mfdTimesRoughnessSq.w > 0.0)
                {
                    vec3 unweightedSample = mfdTimesRoughnessSq.rgb * sampleBase;
                    results[i].baseFresnel = vec4(unweightedSample, 1.0 / (2.0 * PI)) * weight;
                    results[i].fresnelAdjustment = vec4(fresnelFactor * unweightedSample, 1.0 / (2.0 * PI)) * weight;
                }
                else
                {
                    results[i].baseFresnel = vec4(0.0);
                    results[i].fresnelAdjustment = vec4(0.0);
                }
            }
        }
    }
    else
    {
        for (int i = 0; i < ACTIVE_EIGENTEXTURE_COUNT; i++)
        {
            results[i].baseFresnel = vec4(0.0);
            results[i].fresnelAdjustment = vec4(0.0);
        }
    }

    return results;
}

EnvironmentResult[ACTIVE_EIGENTEXTURE_COUNT] computeSVDEnvironmentShading(int startingSVIndex, vec3 position, vec3 normal, float roughness)
{
    EnvironmentResult[ACTIVE_EIGENTEXTURE_COUNT] sums;

    for (int j = 0; j < ACTIVE_EIGENTEXTURE_COUNT; j++)
    {
        sums[j].baseFresnel = vec4(0.0);
        sums[j].fresnelAdjustment = vec4(0.0);
    }

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        EnvironmentResult[ACTIVE_EIGENTEXTURE_COUNT] samples = computeSVDEnvironmentSamples(i, startingSVIndex, position, normal, roughness);
        for (int j = 0; j < ACTIVE_EIGENTEXTURE_COUNT; j++)
        {
            sums[j].baseFresnel += samples[j].baseFresnel;
            sums[j].fresnelAdjustment += samples[j].fresnelAdjustment;
        }
    }

    EnvironmentResult[ACTIVE_EIGENTEXTURE_COUNT] results;

    for (int j = 0; j < ACTIVE_EIGENTEXTURE_COUNT; j++)
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