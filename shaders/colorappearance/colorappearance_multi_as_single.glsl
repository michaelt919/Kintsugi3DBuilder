/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#ifndef COLOR_APPEARANCE_MULTI_AS_SINGLE_GLSL
#define COLOR_APPEARANCE_MULTI_AS_SINGLE_GLSL

uniform int viewIndex;

#include "colorappearance.glsl"

#line 22 1002

#define INFINITE_LIGHT_SOURCE INFINITE_LIGHT_SOURCES
#define cameraPose             (cameraPoses[viewIndex])
#define lightPosition         (lightPositions[getLightIndex(viewIndex)].xyz)
#define lightIntensity         (getLightIntensity(viewIndex).rgb)

vec3 getViewVector()
{
    return getViewVector(viewIndex);
}

vec3 getLightVector()
{
    return getLightVector(viewIndex);
}

vec4 getColor()
{
    return getColor(viewIndex);
}

vec4 getLinearColor()
{
    return linearizeColor(getColor());
}

LightInfo getLightInfo()
{
    return getLightInfo(viewIndex);
}

#endif // COLOR_APPEARANCE_MULTI_AS_SINGLE_GLSL
