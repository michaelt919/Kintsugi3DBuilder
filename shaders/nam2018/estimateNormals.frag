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

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 normalTS;

#include <shaders/colorappearance/imgspace.glsl>
#include <shaders/colorappearance/colorappearance_multi_as_single.glsl>
#include <shaders/relight/reflectanceequations.glsl>

#line 28 0

uniform sampler2D normalMap;
uniform sampler2D roughnessMap;
uniform sampler2DArray weightMaps;
uniform sampler1DArray basisFunctions;

layout(std140) uniform DiffuseColors
{
    vec4 diffuseColors[BASIS_COUNT];
};

#ifndef BASIS_COUNT
#define BASIS_COUNT 8
#endif

#define N_DOT_H_CUTOFF 0.0

vec3 getBRDFEstimate(float nDotH, float geomFactor)
{
    vec3 estimate = vec3(0);
    float w = sqrt(acos(nDotH) * 3.0 / PI);

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

    vec2 normalDirXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    vec3 prevNormal = tangentToObject * normalDirTS;

    mat3 mATA = mat3(0);
    vec3 vATb = vec3(0);

    for (int k = 0; k < CAMERA_POSE_COUNT; k++)
    {
        vec4 imgColor = getLinearColor(k);
        vec3 lightDisplacement = getLightVector(k);
        vec3 light = normalize(lightDisplacement);
        vec3 view = normalize(getViewVector(k));
        vec3 halfway = normalize(light + view);
        float nDotH = max(0.0, dot(prevNormal, halfway));

        if (nDotH > N_DOT_H_CUTOFF)
        {
            float nDotL = max(0.0, dot(prevNormal, light));
            float nDotV = max(0.0, dot(prevNormal, view));
            float hDotV = max(0.0, dot(halfway, view));

            // "Light intensity" is defined in such a way that we need to multiply by pi to be properly normalized.
            vec3 irradiance = nDotL * PI * lightIntensity / dot(lightDisplacement, lightDisplacement);

            float roughness = texture(roughnessMap, fTexCoord)[0];
            float maskingShadowing = geom(roughness, nDotH, nDotV, nDotL, hDotV);

            vec3 reflectance = imgColor.rgb / irradiance;
            vec3 reflectanceEstimate = getBRDFEstimate(nDotH, maskingShadowing / (4 * nDotL * nDotV));

            float weight = nDotL * sqrt(max(0, 1 - nDotH * nDotH));

            mATA += weight * dot(reflectanceEstimate, reflectanceEstimate) * outerProduct(light, light);
            vATb += weight * dot(reflectanceEstimate, reflectance) * light;
        }
    }

    if (determinant(mATA) > 0)
    {
        vec3 normalObjSpace = inverse(mATA) * vATb;
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
    else
    {
        discard;
    }
}