#version 330

/*
 *  Copyright (c) Michael Tetzlaff 2020
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#include "nam2018.glsl"
#line 17 0

layout(location = 0) out vec4 normalTS;

uniform sampler2DArray weightMaps;
uniform sampler1DArray basisFunctions;

layout(std140) uniform DiffuseColors
{
    vec4 diffuseColors[BASIS_COUNT];
};

#ifndef BASIS_COUNT
#define BASIS_COUNT 8
#endif

#ifndef MICROFACET_DISTRIBUTION_RESOLUTION
#define MICROFACET_DISTRIBUTION_RESOLUTION 90
#endif

#define COSINE_CUTOFF 0.0

vec3 getBRDFEstimate(float nDotH, float geomFactor)
{
    vec3 estimate = vec3(0);
    float w = sqrt(max(0.0, acos(nDotH) * 3.0 / PI));

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        estimate += texture(weightMaps, vec3(fTexCoord, b))[0] * (diffuseColors[b].rgb / PI + texture(basisFunctions, vec2(w, b)).rgb * geomFactor);
    }

    return estimate;
}

void main()
{
    vec3 triangleNormal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(triangleNormal, fBitangent) * triangleNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);

    vec2 normalDirXY = texture(normalEstimate, fTexCoord).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    vec3 prevNormal = tangentToObject * normalDirTS;

    mat3 mATA = mat3(0);
    vec3 vATb = vec3(0);

    float estimatedPeak = getLuminance(getBRDFEstimate(1.0, 0.25));
//    float actualPeak = 0.0;
//    vec3 actualPeakHalfway = vec3(0.0);

    for (int k = 0; k < CAMERA_POSE_COUNT; k++)
    {
        vec4 imgColor = getLinearColor(k);
        vec3 lightDisplacement = getLightVector(k);
        vec3 light = normalize(lightDisplacement);
        vec3 view = normalize(getViewVector(k));
        vec3 halfway = normalize(light + view);
        float nDotH = max(0.0, dot(prevNormal, halfway));
        float nDotL = max(0.0, dot(prevNormal, light));
        float nDotV = max(0.0, dot(prevNormal, view));
        float triangleNDotV = max(0.0, dot(triangleNormal, view));

        if (nDotH > COSINE_CUTOFF && nDotL > COSINE_CUTOFF && nDotV > COSINE_CUTOFF && triangleNDotV > COSINE_CUTOFF)
        {
            float hDotV = max(0.0, dot(halfway, view));

            // "Light intensity" is defined in such a way that we need to multiply by pi to be properly normalized.
            vec3 incidentRadiance = PI * getLightIntensity(k) / dot(lightDisplacement, lightDisplacement);

            float roughness = texture(roughnessEstimate, fTexCoord)[0];
            float maskingShadowing = geom(roughness, nDotH, nDotV, nDotL, hDotV);
            vec3 reflectanceEstimate = getBRDFEstimate(nDotH, maskingShadowing / (4 * nDotL * nDotV));

            float weight = nDotL * sqrt(max(0, 1 - nDotH * nDotH));

            vec3 actualReflectanceTimesNDotL = imgColor.rgb / incidentRadiance;
            mATA += weight * weight * dot(reflectanceEstimate, reflectanceEstimate) * outerProduct(light, light);
            vATb += weight * weight * dot(reflectanceEstimate, actualReflectanceTimesNDotL) * light;
//
//            float grayscaleReflectanceTimesNDotL = getLuminance(actualReflectanceTimesNDotL);
//            actualPeakHalfway = mix(actualPeakHalfway, halfway, max(0, sign(grayscaleReflectanceTimesNDotL - actualPeak)));
//            actualPeak = max(actualPeak, grayscaleReflectanceTimesNDotL);
        }
    }

    vec3 normalObjSpace;

//    if (actualPeak > estimatedPeak && length(actualPeakHalfway) > 0)
//    {
//        normalObjSpace = actualPeakHalfway;
//    }
//    else
    if (determinant(mATA) > 0)
    {
        normalObjSpace = inverse(mATA) * vATb;
    }
    else
    {
        discard;
    }

    float normalLength = length(normalObjSpace);
    if (normalLength > 0)
    {
        normalTS = vec4(transpose(tangentToObject) * normalObjSpace / normalLength * 0.5 + 0.5, 1.0);
    }
    else
    {
        discard;
    }
}
