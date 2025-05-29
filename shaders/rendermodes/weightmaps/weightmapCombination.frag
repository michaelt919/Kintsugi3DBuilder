#version 330

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

#include <colorappearance/material.glsl>
#include <specularfit/evaluateBRDF.glsl>
#include "weightmapCommon.glsl"

layout(location = 0) out vec4 fragColor;

#ifndef WEIGHTMAP_INDEX
#define WEIGHTMAP_INDEX 0
#endif

#ifndef WEIGHTMAP_COUNT
#define WEIGHTMAP_COUNT 8
#endif

void main() {
    fragColor = vec4(0);
    for (int i = WEIGHTMAP_INDEX; i < WEIGHTMAP_COUNT; i++)
    {
        fragColor += texture(weightMaps, vec3(fTexCoord, i)).r * getWeightmapColor(i);
    }
}