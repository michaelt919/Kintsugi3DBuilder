/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#ifndef COLOR_APPEARANCE_SINGLE_GLSL
#define COLOR_APPEARANCE_SINGLE_GLSL

#include "linearize.glsl"

#line 7 1001

#define PI 3.1415926535897932384626433832795 // For convenience

uniform bool infiniteLightSource;

uniform mat4 cameraPose;
uniform vec3 lightPosition;
uniform vec3 lightIntensity;

vec3 getViewVector()
{
    return transpose(mat3(cameraPose)) * -cameraPose[3].xyz - fPosition;
}

vec3 getLightVector()
{
    return transpose(mat3(cameraPose)) * (lightPosition.xyz - cameraPose[3].xyz) - fPosition;
}

vec4 getColor(); // Defined by imgspace_single.glsl or texspace_single.glsl

vec4 getLinearColor()
{
    return linearizeColor(getColor());
}

#endif // COLOR_APPEARANCE_SINGLE_GLSL
