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
#include <shaders/colorappearance/colorappearance_multi_as_single.glsl>
#line 18 0

uniform float errorGamma;

layout(location = 0) out vec4 errorOut;

void main()
{
    vec2 sqrtRoughness_Mask = texture(roughnessEstimate, fTexCoord).ra;
    float filteredMask = sqrtRoughness_Mask[1];

    float roughness = sqrtRoughness_Mask[0] * sqrtRoughness_Mask[0];
    vec3 diffuseColor = pow(clamp(texture(diffuseEstimate, fTexCoord).rgb, 0, 1), vec3(gamma));

    vec3 triangleNormal = normalize(fNormal);

    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(triangleNormal, fBitangent) * triangleNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);

    vec2 fittedNormalXY = texture(normalEstimate, fTexCoord).xy * 2 - vec2(1.0);
    vec3 fittedNormalTS = vec3(fittedNormalXY, sqrt(1 - dot(fittedNormalXY, fittedNormalXY)));
    vec3 fittedNormal = tangentToObject * fittedNormalTS;

    vec4 imgColor = getLinearColor();
    vec3 view = normalize(getViewVector());
    float triangleNDotV = max(0.0, dot(triangleNormal, view));

    vec3 lightDisplacement = getLightVector();
    vec3 light = normalize(lightDisplacement);
    vec3 halfway = normalize(light + view);
    float nDotH = max(0.0, dot(fittedNormal, halfway));
    float nDotL = max(0.0, dot(fittedNormal, light));
    float nDotV = max(0.0, dot(fittedNormal, view));

    // "Light intensity" is defined in such a way that we need to multiply by pi to be properly normalized.
    vec3 incidentRadiance = PI * lightIntensity / dot(lightDisplacement, lightDisplacement);

    vec3 actualReflectanceTimesNDotL = pow(imgColor.rgb / incidentRadiance, vec3(1 / errorGamma));

    float error;

    if (nDotH > 0.0 && nDotL > 0.0 && nDotV > 0.0 && filteredMask > 0.0)
    {
        float hDotV = max(0.0, dot(halfway, view));
        float maskingShadowing = geom(roughness, nDotH, nDotV, nDotL, hDotV);
        vec3 specular = getMFDEstimate(nDotH) * maskingShadowing / (4 * nDotV);
        vec3 reflectanceEstimateTimesNDotL = pow(diffuseColor * nDotL / PI + specular, vec3(1 / errorGamma));

        vec3 diff = actualReflectanceTimesNDotL - reflectanceEstimateTimesNDotL;
        error = dot(diff, diff);
    }
    else
    {
        error = dot(actualReflectanceTimesNDotL, actualReflectanceTimesNDotL);
    }

    errorOut = vec4(vec3(imgColor.a * error / 3), imgColor.a);
}
