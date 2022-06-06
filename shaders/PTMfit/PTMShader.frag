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
#include <shaders/colorappearance/colorappearance_multi_as_single.glsl>
#line 18 0

layout(location = 0) out vec4 colorInfo;
layout(location = 1) out vec3 lightDirTS;
void main(){
    vec3 lightDisplacement = getViewVector();//getLightVector();
    //get light uv
    vec3 lightDir =normalize(lightDisplacement); // object space

    // physical radiance = PI * numeric radiance
    vec3 incidentRadiance = PI * lightIntensity / dot(lightDisplacement, lightDisplacement);

    vec3 triangleNormal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
    - dot(triangleNormal, fBitangent) * triangleNormal
    - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);

    lightDirTS = transpose(tangentToObject) * lightDir; // tangent space

    float nDotV = dot(normalize(getViewVector()), triangleNormal);

    //get rgb
    colorInfo = vec4(step(0, lightDirTS.z /* n dot l */)) * getLinearColor() / vec4(incidentRadiance, 1); // physical reflectance (analogous to albedo / pi)
}