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

#include "specularFit.glsl"
#line 17 0

layout(location = 0) out vec4 reflectance_visibility;
layout(location = 1) out vec4 halfway_geom_weight;

#define COSINE_CUTOFF 0.0

vec3 getNormalEstimate()
{
    vec3 triangleNormal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(triangleNormal, fBitangent) * triangleNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);

    vec2 normalDirXY = texture(normalEstimate, fTexCoord).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    vec3 normalDir = tangentToObject * normalDirTS;

    return normalDir;
}

void main()
{

    vec4 imgColor = getLinearColor();
    vec3 lightDisplacement = getLightVector();
    vec3 light = normalize(lightDisplacement);
    vec3 view = normalize(getViewVector());
    vec3 halfway = normalize(light + view);
    vec3 normal = getNormalEstimate();
    float nDotL = max(0.0, dot(normal, light));
    float nDotV = max(0.0, dot(normal, view));
    float nDotH = max(0.0, dot(normal, halfway));
    float triangleNDotV = max(0.0, dot(normalize(fNormal), view));

    if (nDotH > COSINE_CUTOFF && nDotL > COSINE_CUTOFF && nDotV > COSINE_CUTOFF && triangleNDotV > COSINE_CUTOFF)
    {
        float hDotV = max(0.0, dot(halfway, view));

        // "Light intensity" is defined in such a way that we need to multiply by pi to be properly normalized.
        vec3 irradiance = nDotL * PI * lightIntensity / dot(lightDisplacement, lightDisplacement);

        float roughness = texture(roughnessEstimate, fTexCoord)[0];
        float maskingShadowing = geom(roughness, nDotH, nDotV, nDotL, hDotV);

        reflectance_visibility = vec4(imgColor.rgb / irradiance, imgColor.a);

        // Halfway component should be 1.0 when the angle is 60 degrees, or pi/3.
        halfway_geom_weight = vec4(
            sqrt(max(0.0, acos(min(1.0, nDotH)) * 3.0 / PI)),
            maskingShadowing / (4 * nDotL * nDotV),
            triangleNDotV * sqrt(max(0, 1 - nDotH * nDotH)),
            nDotL);
    }
    else
    {
        discard;
    }
}
