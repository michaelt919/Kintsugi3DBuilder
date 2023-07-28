#version 330

/*
 *  Copyright (c) Michael Tetzlaff 2022
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

uniform vec3 reconstructionCameraPos;
uniform vec3 reconstructionLightPos;
uniform vec3 reconstructionLightIntensity;
// gamma defined in colorappearance.glsl

layout(location = 0) out vec4 fragColor;

void main()
{
    mat3 tangentToObject = constructTBNExact();
    vec3 triangleNormal = tangentToObject[2];

    vec2 normalDirXY = texture(normalEstimate, fTexCoord).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    vec3 normal = tangentToObject * normalDirTS;

    vec3 position = getPosition();
    vec3 lightDisplacement = reconstructionLightPos - position;
    vec3 light = normalize(lightDisplacement);
    vec3 view = normalize(reconstructionCameraPos - position);
    vec3 halfway = normalize(light + view);
    float nDotH = max(0.0, dot(normal, halfway));
    float nDotL = max(0.0, dot(normal, light));
    float nDotV = max(0.0, dot(normal, view));
    float hDotV = max(0.0, dot(halfway, view));
    float sqrtRoughness = texture(roughnessEstimate, fTexCoord)[0];
    float roughness = sqrtRoughness * sqrtRoughness;
    float geomRatio;

    if (nDotL > 0.0 && nDotV > 0.0)
    {
        float maskingShadowing = geom(roughness, nDotH, nDotV, nDotL, hDotV);
        geomRatio = maskingShadowing / (4 * nDotL * nDotV);
    }
    else if (nDotL > 0.0)
    {
        geomRatio = 0.5 / (roughness * nDotL); // Limit as n dot v goes to zero.
    }

    vec3 incidentRadiance = PI / linearizeColor(vec3(1));

    if (nDotL > 0.0)
    {
        vec3 brdf = pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma)) / PI + geomRatio * getMFDEstimate(nDotH);
        fragColor = vec4(pow(incidentRadiance * nDotL * brdf, vec3(1.0 / gamma)), 1.0);
    }
    else
    {
        // Limit as n dot l and n dot v both go to zero.
        vec3 mfd = getMFDEstimate(nDotH);
        fragColor = vec4(pow(incidentRadiance * mfd * 0.5 / roughness, vec3(1.0 / gamma)), 1.0);
    }
}
