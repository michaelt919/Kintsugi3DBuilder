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

#ifndef STANDARD_GLSL
#define STANDARD_GLSL

#include "subject.glsl"

uniform vec3 defaultDiffuseColor;

#ifndef DEFAULT_DIFFUSE_COLOR
#if DIFFUSE_TEXTURE_ENABLED
#define DEFAULT_DIFFUSE_COLOR (vec3(0.0))
#else
#define DEFAULT_DIFFUSE_COLOR (defaultDiffuseColor)
#endif // !SPECULAR_TEXTURE_ENABLED
#endif // DEFAULT_DIFFUSE_COLOR

#ifndef DEFAULT_SPECULAR_COLOR
#if SPECULAR_TEXTURE_ENABLED
#define DEFAULT_SPECULAR_COLOR (vec3(0.0))
#else
#define DEFAULT_SPECULAR_COLOR (vec3(0.04))
#endif // DIFFUSE_TEXTURE_ENABLED
#endif // DEFAULT_SPECULAR_COLOR

#ifndef DEFAULT_SPECULAR_ROUGHNESS
#define DEFAULT_SPECULAR_ROUGHNESS (0.1); // TODO pass in a default?
#endif

#include <colorappearance/material.glsl>

#line 43 3100

vec3 global(ViewingParameters v, Material m)
{
    return getEnvironmentDiffuse(v.normalDir) * m.occlusion * min(vec3(1.0), m.diffuseColor + m.specularColor);
}

vec3 specular(LightingParameters l, Material m)
{
#if FRESNEL_EFFECT_ENABLED
    vec3 mfdFresnelBase = m.specularColor * distTimesPi(l.nDotH, vec3(m.roughness));
    return fresnel(mfdFresnelBase, vec3(getLuminance(mfdFresnelBase) / getLuminance(m.specularColor)), l.hDotV);
#else // !FRESNEL_EFFECT_ENABLED
    return m.specularColor * distTimesPi(l.nDotH, vec3(m.roughness));
#endif // FRESNEL_EFFECT_ENABLED
}

vec3 diffuse(LightingParameters l, Material m)
{
    return m.diffuseColor;
}

vec3 emissive(Material m)
{
    return vec3(0.0);
}

#include "subjectMain.glsl"

// Prevents shader link errors when declaration from colorappearance.glsl is not defined
vec4 getColor(int virtualIndex)
{
    return vec4(0);
}

#endif // STANDARD_GLSL