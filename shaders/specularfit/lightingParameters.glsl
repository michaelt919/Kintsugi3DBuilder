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
#line 13 4002

uniform sampler2D normalMap;
#include "../common/constructTBN.glsl"
#line 17 4002

struct LightingParameters
{
    float nDotH;
    float nDotL;
    float nDotV;
    float hDotV;
};

LightingParameters calculateLightingParameters(vec3 lightPos, vec3 cameraPos, vec3 normal)
{
    vec3 position = getPosition();
    vec3 lightDisplacement = lightPos - position;
    vec3 light = normalize(lightDisplacement);
    vec3 view = normalize(cameraPos - position);
    vec3 halfway = normalize(light + view);

    LightingParameters l;
    l.nDotH = max(0.0, dot(normal, halfway));
    l.nDotL = max(0.0, dot(normal, light));
    l.nDotV = max(0.0, dot(normal, view));
    l.hDotV = max(0.0, dot(halfway, view));
    return l;
}

LightingParameters calculateLightingParameters(vec3 lightPos, vec3 cameraPos)
{
    mat3 tangentToObject = constructTBNExact();
    vec3 triangleNormal = tangentToObject[2];

    vec2 normalDirXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    vec3 normal = tangentToObject * normalDirTS;

    return calculateLightingParameters(lightPos, cameraPos, normal);
}
