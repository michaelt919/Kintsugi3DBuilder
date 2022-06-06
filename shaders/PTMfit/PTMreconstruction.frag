#version 330

/*
 *  Copyright (c) Zhangchi (Josh) Lyu, Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#include "PTMfit.glsl"

#line 18 0

uniform sampler2DArray weightMaps;
uniform int width;
uniform int length;

#ifndef BASIS_COUNT
#define BASIS_COUNT 10
#endif

uniform vec3 reconstructionLightPos;
uniform vec3 reconstructionLightIntensity;

layout(location = 0) out vec4 result;

void main()
{
    result=vec4(0,0,0,1);

    vec3 lightDisplacement = reconstructionLightPos - fPosition;
    vec3 lightDir=normalize(lightDisplacement); // object space

    vec3 triangleNormal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
    - dot(triangleNormal, fBitangent) * triangleNormal
    - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);

    vec3 lightDirTS = transpose(tangentToObject) * lightDir; // tangent space


    float u=lightDirTS.x;
    float v=lightDirTS.y;
    float w=lightDirTS.z;

    vec3 weights[BASIS_COUNT];

    float row[BASIS_COUNT];
    row[0] = 1.0;
    row[1] = u;
    row[2] = v;
    row[3] = w;
    //row[4] = u*u;
    //row[5] = v*v;
    //row[6] = w*w;
    //row[7] = v*u;
    //row[8] = w*u;
    //row[9] = v*w;
    row[4] = v*u;
    row[5] = u*u + v*v;
//    row[6] = v*v;

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        weights[b] = clamp(texture(weightMaps, vec3(fTexCoord, b)).xyz, -1/PI, 1/PI);
        result=result+vec4(weights[b]*row[b],0);
    }

    vec3 incidentRadiance = PI * reconstructionLightIntensity / dot(lightDisplacement, lightDisplacement);

    result = pow(result * vec4(incidentRadiance, 1)*vec4(vec3(step(0, lightDirTS.z)), 1),vec4(1/2.2));
}
