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

layout(location = 0) out vec4 reflectance_visibility;
layout(location = 1) out vec4 halfway_geom;

#include <shaders/colorappearance/imgspace.glsl>
#include <shaders/colorappearance/colorappearance_multi_as_single.glsl>
#include <shaders/relight/reflectanceequations.glsl>

#line 29 0

uniform sampler2D normalMap;
uniform sampler2D roughnessMap;

#define N_DOT_H_CUTOFF 0.0

vec3 getNormalVector()
{
    vec3 triangleNormal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(triangleNormal, fBitangent) * triangleNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);

    vec2 normalDirXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
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
    vec3 normal = getNormalVector();
    float nDotL = max(0.0, dot(normal, light));
    float nDotV = max(0.0, dot(normal, view));
    float nDotH = max(0.0, dot(normal, halfway));
    float hDotV = max(0.0, dot(halfway, view));
    vec3 irradiance = nDotL * lightIntensity / dot(lightDisplacement, lightDisplacement);
    float roughness = texture(roughnessMap, fTexCoord)[0];
    float maskingShadowing = geom(roughness, nDotH, nDotV, nDotL, hDotV);

    if (nDotH > N_DOT_H_CUTOFF)
    {
        reflectance_visibility = vec4(pow(imgColor.rgb / irradiance, vec3(1.0 / gamma)), imgColor.a);
        halfway_geom = vec4(sqrt(acos(nDotH)), maskingShadowing / (4 * nDotL * nDotV), 0, 1);
    }
    else
    {
        discard;
    }
}
