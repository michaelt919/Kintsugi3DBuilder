#version 330

#include "../colorappearance/colorappearance.glsl"
#include "../colorappearance/svd_unpack.glsl"
#include "environment.glsl"

#define ACTIVE_EIGENTEXTURE_COUNT 4

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec4 baseFresnel0;
layout(location = 1) out vec4 baseFresnel1;
layout(location = 2) out vec4 baseFresnel2;
layout(location = 3) out vec4 baseFresnel3;
layout(location = 4) out vec4 fresnelAdj0;
layout(location = 5) out vec4 fresnelAdj1;
layout(location = 6) out vec4 fresnelAdj2;
layout(location = 7) out vec4 fresnelAdj3;


uniform sampler2D roughnessMap;

struct EnvironmentResult
{
    vec4 baseFresnel;
    vec4 fresnelAdjustment;
};

vec4[ACTIVE_EIGENTEXTURE_COUNT + 1] computeEnvironmentSamples(int virtualIndex, float roughness, float maxLuminance)
{
    EnvironmentResult[ACTIVE_EIGENTEXTURE_COUNT + 1] results;

    mat4 cameraPose = getCameraPose(virtualIndex);
    vec3 fragmentPos = (cameraPose * vec4(fPosition, 1.0)).xyz;
    vec3 normalDirCameraSpace = normalize((cameraPose * vec4(fNormal, 0.0)).xyz);
    vec3 sampleViewDir = normalize(-fragmentPos);

    // All in camera space
    vec3 sampleLightDirUnnorm = lightPositions[getLightIndex(virtualIndex)].xyz - fragmentPos;
    float lightDistSquared = dot(sampleLightDirUnnorm, sampleLightDirUnnorm);
    vec3 sampleLightDir = sampleLightDirUnnorm * inversesqrt(lightDistSquared);

    vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);

    vec3 virtualViewDir =
        normalize((cameraPose * vec4(viewPos, 1.0)).xyz - fragmentPos);
    vec3 virtualLightDir = -reflect(virtualViewDir, sampleHalfDir);
    float nDotL_virtual = max(0, dot(normalDirCameraSpace, virtualLightDir));
    float hDotV_virtual = max(0, dot(sampleHalfDir, virtualViewDir));

    float oneMinusHDotV = max(0, 1.0 - hDotV_virtual);
    float oneMinusHDotVSq = oneMinusHDotV * oneMinusHDotV;
    float fresnelFactor = oneMinusHDotVSq * oneMinusHDotVSq * oneMinusHDotV;

    float microfacetShadowing;
#if PHYSICALLY_BASED_MASKING_SHADOWING
    microfacetShadowing = geomPartial(roughness, nDotL_virtual);
#else
    microfacetShadowing = nDotL_virtual;
#endif

    vec3 sampleBase = microfacetShadowing * rgbToXYZ(getEnvironment(mat3(envMapMatrix) * transpose(mat3(cameraPose)) * virtualLightDir));
    float weight = 4 * hDotV_virtual * (getCameraWeight(virtualIndex) * 4 * PI * VIEW_COUNT);
    // dl = 4 * h dot v * dh
    // cameraWeight * VIEW_COUNT -> brings weights back to being on the order of 1
    // This is helpful for consistency with numerical limits (i.e. clamping)
    // Everything gets normalized at the end again anyways.

    results[0].baseFresnel = vec4(sampleBase, 1.0 / (2.0 * PI)) * weight;
    results[0].fresnelAdjustment = fresnelFactor * vec4(sampleBase, 1.0) * weight;

    for (int i = 0; i < ACTIVE_EIGENTEXTURE_COUNT; i++)
    {
        // Light intensities in view set files are assumed to be pre-divided by pi.
        // Or alternatively, the result of getLinearColor gives a result
        // where a diffuse reflectivity of 1 is represented by a value of pi.
        // See diffusefit.glsl
        vec4 mfdTimesRoughnessSq = computeSVDViewWeights(computeBlockSize(fTexCoord, textureSize(eigentextures, 0)), i);

        if (mfdTimesRoughnessSq.w > 0.0)
        {
            vec3 unweightedSample = mfdTimesRoughnessSq.xyz * sampleBase;
            results[i + 1].baseFresnel = vec4(unweightedSample, 1.0 / (2.0 * PI)) * weight;
            results[i + 1].fresnelAdjustment = fresnelFactor * vec4(unweightedSample, 1.0) * weight;
        }
        else
        {
            results[i + 1].baseFresnel = vec4(0.0);
            results[i + 1].fresnelAdjustment = vec4(0.0);
        }
    }

    return results;
}

void main()
{
    float maxLuminance = getMaxLuminance();
    float roughness = texture(roughnessMap, fTexCoord).y;

    EnvironmentResult[ACTIVE_EIGENTEXTURE_COUNT + 1] sums;

    for (int j = 0; j < ACTIVE_EIGENTEXTURE_COUNT + 1; j++)
    {
        sums[j].baseFresnel = vec4(0.0);
        sums[j].fresnelAdjustment = vec4(0.0);
    }

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        EnvironmentResult[ACTIVE_EIGENTEXTURE_COUNT + 1] samples = computeEnvironmentSamples(i, roughness, maxLuminance);
        for (int j = 0; j < ACTIVE_EIGENTEXTURE_COUNT + 1; j++)
        {
            sums[j].baseFresnel += samples[j].baseFresnel;
            sums[j].fresnelAdjustment += samples[j].fresnelAdjustment;
        }
    }

    EnvironmentResult[ACTIVE_EIGENTEXTURE_COUNT + 1] results;

    for (int j = 0; j < ACTIVE_EIGENTEXTURE_COUNT + 1; j++)
    {
        if (sums[j].baseFresnel.w > 0.0)
        {
            float normalizationFactor =
                VIEW_COUNT;                             // better spatial consistency, worse directional consistency?
//                clamp(sums.weight, 0, 1000000.0);       // Better directional consistency, worse spatial consistency?

            results[j].baseFresnel = sums[j].baseFresnel / normalizationFactor;
            results[j].fresnelAdjustment = sums[j].fresnelAdjustment / vec4(sums[j].fresnelAdjustment.www, normalizationFactor);
        }
        else
        {
            results[j].baseFresnel = vec4(0.0);
            results[j].fresnelAdjustment = vec4(0.0);
        }
    }

    baseFresnel0 = results[0].baseFresnel;
    baseFresnel1 = results[1].baseFresnel;
    baseFresnel2 = results[2].baseFresnel;
    baseFresnel3 = results[3].baseFresnel;

    if (sums[4].baseFresnel.w > 0.0)
    {
        // Pack an extra term into the 4th component
        baseFresnel0.w = results[4].baseFresnel.x;
        baseFresnel1.w = results[4].baseFresnel.y;
        baseFresnel2.w = results[4].baseFresnel.z;
        baseFresnel3.w = sums[4].fresnelAdjustment.y / VIEW_COUNT;
    }

    fresnelAdj0 = results[0].fresnelAdjustment;
    fresnelAdj1 = results[1].fresnelAdjustment;
    fresnelAdj2 = results[2].fresnelAdjustment;
    fresnelAdj3 = results[3].fresnelAdjustment;
}
