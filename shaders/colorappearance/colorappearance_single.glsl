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

#ifndef COLOR_APPEARANCE_SINGLE_GLSL
#define COLOR_APPEARANCE_SINGLE_GLSL

#include "../common/usegeom.glsl"
#include "linearize.glsl"

#line 20 1001

#ifndef PI
#define PI 3.1415926535897932384626433832795 // For convenience
#endif

#ifndef INFINITE_LIGHT_SOURCE
#define INFINITE_LIGHT_SOURCE 0
#endif

uniform mat4 cameraPose;
uniform vec3 lightPosition;
uniform vec3 lightIntensity;

vec3 getViewVector(vec3 position)
{
    return transpose(mat3(cameraPose)) * -cameraPose[3].xyz - position;
}

vec3 getLightVector(vec3 position)
{
    return transpose(mat3(cameraPose)) * (lightPosition.xyz - cameraPose[3].xyz) - position;
}

vec3 getViewVector()
{
    return getViewVector(getPosition());
}

vec3 getLightVector()
{
    return getLightVector(getPosition());
}

vec4 getColor(); // Defined by imgspace_single.glsl or texspace_single.glsl

vec4 getLinearColor()
{
    return linearizeColor(getColor());
}

struct LightInfo
{
    vec3 attenuatedIntensity;
    vec3 normalizedDirection;
};


LightInfo getLightInfo()
{
    LightInfo result;
    result.normalizedDirection = getLightVector();
    result.attenuatedIntensity = lightIntensity;

    float lightDistSquared = dot(result.normalizedDirection, result.normalizedDirection);
    result.normalizedDirection *= inversesqrt(lightDistSquared);

#if !INFINITE_LIGHT_SOURCES
    result.attenuatedIntensity /= lightDistSquared;
#endif

    return result;
}

#endif // COLOR_APPEARANCE_SINGLE_GLSL
