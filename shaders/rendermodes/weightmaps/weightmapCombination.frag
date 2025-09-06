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

layout(location = 0) out vec4 fragColor;

#ifndef WEIGHTMAP_INDEX
#define WEIGHTMAP_INDEX 0
#endif

#ifndef WEIGHTMAP_COUNT
#define WEIGHTMAP_COUNT 8
#endif

#define WEIGHTMAP_COLOR_COUNT 8

const vec3 WEIGHTMAP_COLORS[8] = vec3[](
    vec3(0.2,0.2,1),
    vec3(1,1,0),
    vec3(1,0,0),
    vec3(0,1,1),
    vec3(0.5,0.25,1),
    vec3(0,0.5,0),
    vec3(1,0.5,0),
    vec3(1,1,1)
);

//const vec3 WEIGHTMAP_COLORS[WEIGHTMAP_COLOR_COUNT] = vec3[](
//vec3(0.9, 0.1, 0.1),
//vec3(0.0, 0.6, 0.2),
//vec3(0.0, 0.45, 0.85),
//vec3(0.95, 0.9, 0.25),
//vec3(0.6, 0.2, 0.7),
//vec3(0.95, 0.55, 0.0),
//vec3(0.0, 0.8, 0.8)
//);

vec4 getWeightmapColor(int index)
{
    return vec4(WEIGHTMAP_COLORS[index], 1);
}

void main() {
    fragColor = vec4(0);
    for (int i = WEIGHTMAP_INDEX; i < min(WEIGHTMAP_COLOR_COUNT, WEIGHTMAP_COUNT); i++)
    {
        fragColor += texture(weightMaps, vec3(fTexCoord, i)).r * getWeightmapColor(i);
    }
}