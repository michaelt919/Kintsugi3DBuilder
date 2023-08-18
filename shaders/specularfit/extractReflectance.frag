#version 330

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

#include "specularFit.glsl"
#include <colorappearance/colorappearance_multi_as_single.glsl>
#line 18 0

layout(location = 0) out vec4 reflectance_visibility;
layout(location = 1) out vec4 halfway_geom_weight;

void main()
{
    vec3 position = getPosition();
    vec4 imgColor = getLinearColor();
    vec3 lightDisplacement = getLightVector(position);
    vec3 light = normalize(lightDisplacement);
    vec3 view = normalize(getViewVector(position));
    vec3 halfway = normalize(light + view);

    mat3 tangentToObject = constructTBNExact();
    vec3 triangleNormal = tangentToObject[2];

    vec2 normalDirXY = texture(normalEstimate, fTexCoord).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    vec3 normal = tangentToObject * normalDirTS;

    float nDotL = max(0.0, dot(normal, light));
    float nDotV = max(0.0, dot(normal, view));
    float nDotH = max(0.0, dot(normal, halfway));
    float triangleNDotV = max(0.0, dot(triangleNormal, view));

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
            // n.v accounts for fitting in texture space rather than image space (more samples near grazing angles)
            // (n.l)^2 accounts for fitting reflectance rather than radiance
            // sin(theta_h) prevents bias towards specular (from Nam et al.)
            imgColor.a * triangleNDotV * nDotL * nDotL * sqrt(max(0, 1 - nDotH * nDotH)),
            nDotL);
    }
    else
    {
        discard;
    }
}
