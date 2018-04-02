#version 330

#include "../colorappearance/colorappearance.glsl"
#include "../colorappearance/svd_unpack.glsl"
#include "environment.glsl"

vec4[EIGENTEXTURE_COUNT] computeEnvironmentSamples(int virtualIndex, vec3 diffuseColor, vec3 normalDir, vec3 specularColor, vec3 roughness, float maxLuminance)
{
    vec4[EIGENTEXTURE_COUNT] results;

    mat4 cameraPose = getCameraPose(virtualIndex);
    vec3 fragmentPos = (cameraPose * vec4(fPosition, 1.0)).xyz;
    vec3 normalDirCameraSpace = normalize((cameraPose * vec4(normalDir, 0.0)).xyz);
    vec3 sampleViewDir = normalize(-fragmentPos);

    // All in camera space
    vec3 sampleLightDirUnnorm = lightPositions[getLightIndex(virtualIndex)].xyz - fragmentPos;
    float lightDistSquared = dot(sampleLightDirUnnorm, sampleLightDirUnnorm);
    vec3 sampleLightDir = sampleLightDirUnnorm * inversesqrt(lightDistSquared);

    vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);

    float nDotH = max(0, dot(normalDirCameraSpace, sampleHalfDir));

    vec3 virtualViewDir =
        normalize((cameraPose * vec4(viewPos, 1.0)).xyz - fragmentPos);
    vec3 virtualLightDir = -reflect(virtualViewDir, sampleHalfDir);
    float nDotL_virtual = max(0, dot(normalDirCameraSpace, virtualLightDir));
    float hDotV_virtual = max(0, dot(sampleHalfDir, virtualViewDir));

    vec3 microfacetShadowing;
#if PHYSICALLY_BASED_MASKING_SHADOWING
    microfacetShadowing = geomPartial(roughness, nDotL_virtual);
#else
    microfacetShadowing = vec3(nDotL_virtual);
#endif

    vec3 roughnessSq = roughness * roughness;

    for (int i = 0; i < EIGENTEXTURE_COUNT; i++)
    {
        // Light intensities in view set files are assumed to be pre-divided by pi.
        // Or alternatively, the result of getLinearColor gives a result
        // where a diffuse reflectivity of 1 is represented by a value of pi.
        // See diffusefit.glsl
        vec4 mfdTimesRoughnessSq = computeSVDViewWeights(computeBlockSize(fTexCoord, textureSize(eigentextures, 0)), i);

        if (mfdTimesRoughnessSq.w > 0.0)
        {
            vec3 mfdFresnelDividedByPeak;
#if FRESNEL_ENABLED
            mfdFresnelDividedByPeak = fresnel(mfdTimesRoughnessSq.xyz, rgbToXYZ(vec3(mfdTimesRoughnessSq.y)) / rgbToXYZ(specularColor), hDotV_virtual);
#else
            mfdFresnelDividedByPeak = mfdTimesRoughnessSq.xyz;
#endif

            vec4 unweightedSample;
            unweightedSample.rgb = mfdFresnelDividedByPeak * microfacetShadowing
                * rgbToXYZ(getEnvironment(mat3(envMapMatrix) * transpose(mat3(cameraPose)) * virtualLightDir));

            unweightedSample.a = 1.0 / (2.0 * PI);

            results[i] = unweightedSample * 4 * hDotV_virtual * (getCameraWeight(virtualIndex) * 4 * PI * VIEW_COUNT);
            // dl = 4 * h dot v * dh
            // weight * VIEW_COUNT -> brings weights back to being on the order of 1
            // This is helpful for consistency with numerical limits (i.e. clamping)
            // Everything gets normalized at the end again anyways.
        }
        else
        {
            results[i] = vec4(0.0);
        }
    }

    return results;
}

vec3[EIGENTEXTURE_COUNT] getEnvironmentShadingWeights(vec3 diffuseColor, vec3 normalDir, vec3 specularColor, vec3 roughness)
{
    float maxLuminance = getMaxLuminance();

    vec4[EIGENTEXTURE_COUNT] sums;

    for (int j = 0; j < EIGENTEXTURE_COUNT; j++)
    {
        sums[j] = vec4(0.0);
    }

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        vec4[EIGENTEXTURE_COUNT] samples = computeEnvironmentSamples(i, diffuseColor, normalDir, specularColor, roughness, maxLuminance);
        for (int j = 0; j < EIGENTEXTURE_COUNT; j++)
        {
            sums[j] += samples[j];
        }
    }

    vec3[EIGENTEXTURE_COUNT] results;

    for (int j = 0; j < EIGENTEXTURE_COUNT; j++)
    {
        if (sums[j].w > 0.0)
        {
            results[j] = sum[j].rgb
                / VIEW_COUNT;                    // better spatial consistency, worse directional consistency?
            //    / clamp(sum.w, 0, 1000000.0);    // Better directional consistency, worse spatial consistency?
        }
        else
        {
            results[j] = vec3(0.0);
        }
    }

    return results;
}
