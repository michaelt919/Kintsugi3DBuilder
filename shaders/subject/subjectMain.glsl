/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

#ifndef SUBJECT_MAIN_GLSL
#define SUBJECT_MAIN_GLSL

#include "subject.glsl"
#include <colorappearance/material.glsl>
#line 19 3001

in vec3 fNormal;

layout(location = 1) out int fragObjectID;

uniform int objectID;

#ifndef RELIGHTING_ENABLED
#define RELIGHTING_ENABLED 0
#endif

#ifndef ENVIRONMENT_ILLUMINATION_ENABLED
#define ENVIRONMENT_ILLUMINATION_ENABLED 1
#endif

#if VIRTUAL_LIGHT_COUNT > 0

uniform vec3 lightIntensityVirtual[VIRTUAL_LIGHT_COUNT];

#if RELIGHTING_ENABLED
uniform vec3 lightPosVirtual[VIRTUAL_LIGHT_COUNT];
#endif // RELIGHTING_ENABLED

#endif // VIRTUAL_LIGHT_COUNT > 0

#if !RELIGHTING_ENABLED
#include <colorappearance/colorappearance.glsl> // Need if relighting is not enabled to infer light direction
#line 47 3001
#endif

vec3 getLightVectorVirtual(int lightIndex)
{
#if RELIGHTING_ENABLED && VIRTUAL_LIGHT_COUNT > 0
    return lightPosVirtual[lightIndex] - fPosition;
#elif !RELIGHTING_ENABLED
    return transpose(mat3(model_view)) * lightPositions[getLightIndex(0)].xyz + viewPos - fPosition;
#else // VIRTUAL_LIGHT_COUNT == 0
    return vec3(0);
#endif
}

#ifndef SHADOWS_ENABLED
#define SHADOWS_ENABLED 0
#endif

#if SHADOWS_ENABLED
uniform sampler2DArray shadowMaps;
uniform mat4 lightMatrixVirtual[VIRTUAL_LIGHT_COUNT];

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
#endif // SHADOWS_ENABLED

#ifndef SPOTLIGHTS_ENABLED
#define SPOTLIGHTS_ENABLED 0
#endif

#if VIRTUAL_LIGHT_COUNT > 0 && RELIGHTING_ENABLED && SPOTLIGHTS_ENABLED
uniform vec3 lightOrientationVirtual[VIRTUAL_LIGHT_COUNT];
uniform float lightSpotSizeVirtual[VIRTUAL_LIGHT_COUNT];
uniform float lightSpotTaperVirtual[VIRTUAL_LIGHT_COUNT];
#endif // SPOTLIGHTS_ENABLED

vec3 emissive(Material m);
vec3 global(ViewingParameters v, Material m);
vec3 diffuse(LightingParameters l, Material m);

#ifdef SPECULAR_PRECOMPUTATION
SPECULAR_PRECOMPUTATION precomputeSpecular(ViewingParameters v, Material m);
vec3 specular(LightingParameters l, Material m, SPECULAR_PRECOMPUTATION p);
#else
vec3 specular(LightingParameters l, Material m);
#endif

#ifndef MODULATE_ENABLED
#define MODULATE_ENABLED 0
#endif

#if MODULATE_ENABLED
vec4 modulate(Material m);
#endif

void main()
{
    vec3 triangleNormal = normalize(fNormal);
    ViewingParameters v = buildViewingParameters(getRefinedWorldSpaceNormal(triangleNormal), viewPos - fPosition);

    float nDotV_triangle = dot(triangleNormal, v.viewDir);

    // Flip normals if necessary, but don't do anything if nDotV is zero.
    // This is required for the ground plane.
    float flip = (sign(nDotV_triangle) + 1 - abs(sign(nDotV_triangle)));
    triangleNormal *= flip;
    v.normalDir *= flip;
    nDotV_triangle = abs(nDotV_triangle);

    // TODO: is it desirable for the primary object (not the ground plane) to NOT shade backfacing polygons?
    //    if (nDotV_triangle == 0.0)
    //    {
    //        fragColor = vec4(0, 0, 0, 1);
    //        return;
    //    }

    Material m = getMaterial();

    vec3 radiance = emissive(m);

#if RELIGHTING_ENABLED && ENVIRONMENT_ILLUMINATION_ENABLED
    radiance += global(v, m);
#endif // RELIGHTING_ENABLED && ENVIRONMENT_ILLUMINATION_ENABLED

#if VIRTUAL_LIGHT_COUNT > 0

#ifdef SPECULAR_PRECOMPUTATION
    SPECULAR_PRECOMPUTATION precomputation = precomputeSpecular(v, m);
#endif

    for (int i = 0; i < VIRTUAL_LIGHT_COUNT; i++)
    {
        LightingParameters l = buildLightingParameters(i, getLightVectorVirtual(i), v);

        if (l.nDotL > 0.0 && dot(triangleNormal, l.lightDir) > 0.0)
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
#ifdef SPECULAR_PRECOMPUTATION
                vec3 mfdFresnel = specular(l, m, precomputation);
#else
                vec3 mfdFresnel = specular(l, m);
#endif

                vec3 cosineWeightedReflectance = l.nDotL * diffuse(l, m);
#if PHYSICALLY_BASED_MASKING_SHADOWING
                cosineWeightedReflectance += mfdFresnel * geom(m.roughness, l.nDotH, l.nDotV, l.nDotL, l.hDotV) / (4 * l.nDotV);
#else
                cosineWeightedReflectance += mfdFresnel * l.nDotL / 4;
#endif

                vec3 lightVectorTransformed = (model_view * vec4(l.lightDirUnnorm, 0.0)).xyz;
                vec3 pointRadiance;

#if RELIGHTING_ENABLED
                vec3 incidentRadiance = lightIntensityVirtual[i] / dot(lightVectorTransformed, lightVectorTransformed);
#if SPOTLIGHTS_ENABLED
                float lightDirCorrelation = max(0.0, dot(l.lightDir, -lightOrientationVirtual[i]));
                float spotBoundaryDistance = lightSpotSizeVirtual[i] - sqrt(1 - lightDirCorrelation * lightDirCorrelation);
                incidentRadiance *= clamp(
                spotBoundaryDistance / max(0.001, max(lightSpotSizeVirtual[i] * lightSpotTaperVirtual[i], spotBoundaryDistance)),
                0.0, 1.0);
#endif // SPOTLIGHTS_ENABLED

                pointRadiance = cosineWeightedReflectance * incidentRadiance;
#else // !RELIGHTING_ENABLED
                pointRadiance = cosineWeightedReflectance;
#endif // RELIGHTING_ENABLED
                radiance += pointRadiance;
            }
        }
    }

#endif // VIRTUAL_LIGHT_COUNT > 0

    float alpha;
#if MODULATE_ENABLED
    vec4 modulation = modulate(m);
    radiance *= modulation.rgb;
    alpha = modulation.a;
#else
    alpha = 1.0;
#endif

    fragColor = tonemap(radiance, alpha);

    fragObjectID = objectID;
}

#endif // SUBJECT_MAIN_GLSL
