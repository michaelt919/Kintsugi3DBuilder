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

layout(location = 0) out vec4 fragColor;

uniform sampler2D diffuseEstimate;
uniform sampler2D specularEstimate;

#ifndef BASIS_COUNT
#define BASIS_COUNT 8
#endif

void main()
{
    vec2 sqrtRoughness_Mask = texture(roughnessEstimate, fTexCoord).ra;
    float filteredMask = sqrtRoughness_Mask[1];

    if (filteredMask == 0.0)
    {
        fragColor = vec4(0, 0, 0, 1);
        return;
    }

    float roughness = sqrtRoughness_Mask[0] * sqrtRoughness_Mask[0] / (filteredMask * filteredMask);

    vec3 diffuseColor = pow(texture(diffuseEstimate, fTexCoord).rgb / filteredMask, vec3(gamma));
    vec3 specularColor = pow(texture(specularEstimate, fTexCoord).rgb / filteredMask, vec3(gamma));

    vec3 triangleNormal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(triangleNormal, fBitangent) * triangleNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);

    vec2 normalDirXY = texture(normalEstimate, fTexCoord).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    vec3 normal = tangentToObject * normalDirTS;

    vec3 lightDisplacement = getLightVector();
    vec3 light = normalize(lightDisplacement);
    vec3 view = normalize(getViewVector());
    vec3 halfway = normalize(light + view);
    float nDotH = max(0.0, dot(normal, halfway));
    float nDotL = max(0.0, dot(normal, light));
    float nDotV = max(0.0, dot(normal, view));
    float hDotV = max(0.0, dot(halfway, view));
    vec3 incidentRadianceOverPi = lightIntensity / dot(lightDisplacement, lightDisplacement);

    vec3 specular = incidentRadianceOverPi * distTimesPi(nDotH, vec3(roughness))
        * geom(roughness, nDotH, nDotV, nDotL, hDotV)
        * fresnel(specularColor.rgb, vec3(1), hDotV) / (4 * nDotV);

    // Reflectance is implicitly multiplied by n dot l.
    fragColor = vec4(pow(diffuseColor * nDotL * incidentRadianceOverPi + specular, vec3(1.0 / gamma)), 1.0);
}