#version 330

/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Atlas Collins, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

#include <subject/subject.glsl>

#ifndef DEFAULT_DIFFUSE_COLOR
#define DEFAULT_DIFFUSE_COLOR (vec3(0.0))
#endif // DEFAULT_DIFFUSE_COLOR

#ifndef DEFAULT_SPECULAR_COLOR
#define DEFAULT_SPECULAR_COLOR (vec3(0.0))
#endif // DEFAULT_SPECULAR_COLOR

#ifndef DEFAULT_SPECULAR_ROUGHNESS
#define DEFAULT_SPECULAR_ROUGHNESS (0.1); // TODO pass in a default?
#endif

#ifndef WEIGHTMAP_INDEX
#define WEIGHTMAP_INDEX 0
#endif

#include <colorappearance/material.glsl>
#include <specularfit/evaluateBRDF.glsl>

#line 37 0

vec3 global(ViewingParameters v, Material m)
{
    return vec3(0.0);
}

vec3 specular(LightingParameters l, Material m)
{
    float w = getMFDLookupCoord(l.nDotH);
    return PI * texture(basisFunctions, vec2(w, WEIGHTMAP_INDEX)).rgb;
}

vec3 diffuse(LightingParameters l, Material m)
{
    return diffuseColors[WEIGHTMAP_INDEX].rgb;
}

vec3 emissive(Material m)
{
    return vec3(0.0);
}

#include <subject/subjectMain.glsl>

// Prevents shader link errors when declaration from colorappearance.glsl is not defined
vec4 getColor(int virtualIndex)
{
    return vec4(0);
}
