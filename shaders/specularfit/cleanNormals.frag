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
#define CLEAN_THRESHOLD 1.0
#endif

uniform sampler2D origNormalEstimate;
uniform sampler2D prevNormalEstimate;

void main()
{
    // Pass through (do nothing)
    cleanNormalTS = texture(prevNormalEstimate, fTexCoord);

//    vec3 triangleNormal = normalize(fNormal);
//    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
//    vec3 bitangent = normalize(fBitangent
//        - dot(triangleNormal, fBitangent) * triangleNormal
//        - dot(tangent, fBitangent) * tangent);
//    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);
//
//    vec2 origNormalXY = texture(origNormalEstimate, fTexCoord).xy * 2 - vec2(1.0);
//    vec3 origNormalTS = vec3(origNormalXY, sqrt(1 - dot(origNormalXY, origNormalXY)));
//    vec3 origNormal = tangentToObject * origNormalTS;
//
//    vec2 prevNormalXY = texture(prevNormalEstimate, fTexCoord).xy * 2 - vec2(1.0);
//    vec3 prevNormalTS = vec3(prevNormalXY, sqrt(1 - dot(prevNormalXY, prevNormalXY)));
//    vec3 prevNormal = tangentToObject * prevNormalTS;
//
//    vec2 newNormalXY = (textureOffset(prevNormalEstimate, fTexCoord, ivec2(0, 1)).xy
//        + textureOffset(prevNormalEstimate, fTexCoord, ivec2(0, -1)).xy
//        + textureOffset(prevNormalEstimate, fTexCoord, ivec2(1, 0)).xy
//        + textureOffset(prevNormalEstimate, fTexCoord, ivec2(-1, 0)).xy) * 0.5 - vec2(1.0);
//    vec3 newNormalTS = vec3(newNormalXY, sqrt(1 - dot(newNormalXY, newNormalXY)));
//    vec3 newNormal = tangentToObject * newNormalTS;
//
//    float origError = calculateError(triangleNormal, origNormal);
//    float newError = calculateError(triangleNormal, newNormal);
//
//    // 1 if sqrt(newError / originalError) >= 1 + threshold [use estimate from previous iteration]
//    // 0 otherwise [use new, smoothed estimate]
//    float alpha = step((1 + CLEAN_THRESHOLD) * (1 + CLEAN_THRESHOLD), newError / origError);
//
//    // Max alpha is 0.5 since it should also still include the previous normal with 50% weight regardless of error.
//    cleanNormalTS = vec4(normalize(mix(newNormalTS, prevNormalTS, alpha * 0.5 + 0.5)) * 0.5 + vec3(0.5), 1.0);
}
