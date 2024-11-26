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
#line 13 4000

#if !GEOMETRY_TEXTURES_ENABLED // position not needed if geometry textures are being used
in vec3 fPosition;
#endif
in vec2 fTexCoord;
// normal and tangent will be declared by constructTBN.glsl

uniform sampler2D diffuseMap;
uniform sampler2D roughnessMap;

#ifndef USE_CONSTANT_MAP
#define USE_CONSTANT_MAP 0
#endif

#if USE_CONSTANT_MAP
uniform sampler2D constantMap;

vec3 getConstantTerm()
{
    return texture(constantMap, fTexCoord).rgb;
}

#else

vec3 getConstantTerm()
{
    return vec3(0);
}

#endif

#include <colorappearance/colorappearance_dynamic.glsl>
#include <colorappearance/reflectanceequations.glsl>

#define COSINE_CUTOFF 0.0

#ifndef BASIS_RESOLUTION
#define BASIS_RESOLUTION 90
#endif

#include "evaluateBRDF.glsl"
#include "lightingParameters.glsl"