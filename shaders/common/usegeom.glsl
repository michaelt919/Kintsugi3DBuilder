/*
 *  Copyright (c) Michael Tetzlaff 2023
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#ifndef USE_GEOM_GLSL
#define USE_GEOM_GLSL

#line 17 9903

#ifndef GEOMETRY_TEXTURES_ENABLED
#define GEOMETRY_TEXTURES_ENABLED 0
#endif

#if GEOMETRY_TEXTURES_ENABLED
uniform sampler2D positionTex;

vec3 getPosition()
{
    // Assume fTexCoord to be declared previously
    // Assume the texture is set to store floating-point elements.
    return texture(positionTex, fTexCoord);
}

#else
// Assume fPosition is defined previously
vec3 getPosition()
{
    return fPosition;
}
#endif

#endif // USE_GEOM_GLSL