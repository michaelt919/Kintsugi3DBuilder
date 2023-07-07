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

#ifndef COLOR_APPEARANCE_DYNAMIC_GLSL
#define COLOR_APPEARANCE_DYNAMIC_GLSL

#include "colorappearance.glsl"
#include "colorappearancemode.glsl"
#line 19 1003

#if COLOR_APPEARANCE_MODE == COLOR_APPEARANCE_MODE_ANALYTIC
#include "analytic.glsl"
#line 30 1003
#elif COLOR_APPEARANCE_MODE == COLOR_APPEARANCE_MODE_IMAGE_SPACE
#include "imgspace.glsl"
#line 33 1003
#elif COLOR_APPEARANCE_MODE == COLOR_APPEARANCE_MODE_TEXTURE_SPACE
#include "texspace.glsl"
#line 36 1003
#elif COLOR_APPEARANCE_MODE == COLOR_APPEARANCE_MODE_TEXTURE_SPACE_CROP
#include "texspace_crop.glsl"
#else
vec4 getColor(int index)
{
    // Unrecognized mode; use magenta to indicate an error
    return vec4(1, 0, 1, 1);
}
#endif

#endif // COLOR_APPEARANCE_DYNAMIC_GLSL