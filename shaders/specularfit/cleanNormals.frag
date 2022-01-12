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
#include "evaluateBRDF.glsl"
#include "normalError.glsl"
#line 19 0

layout(location = 0) out vec4 cleanNormalTS;

#ifndef MICROFACET_DISTRIBUTION_RESOLUTION
#define MICROFACET_DISTRIBUTION_RESOLUTION 90
#endif

#ifndef CLEAN_THRESHOLD
#define CLEAN_THRESHOLD 0.25
#endif

void main()
{
    vec3 triangleNormal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(triangleNormal, fBitangent) * triangleNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);

    vec2 estNormalXY = texture(normalEstimate, fTexCoord).xy * 2 - vec2(1.0);
    vec3 estNormalTS = vec3(estNormalXY, sqrt(1 - dot(estNormalXY, estNormalXY)));
    vec3 estNormal = tangentToObject * estNormalTS;

    float estError = calculateError(triangleNormal, estNormal);
    float origError = calculateError(triangleNormal, triangleNormal);

    float alpha = clamp((1 - estError / origError) / CLEAN_THRESHOLD, 0, 1);
    cleanNormalTS = vec4(mix(vec3(0, 0, 1), estNormalTS, alpha) * 0.5 + vec3(0.5), 1.0);
}
