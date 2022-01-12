/*
 *  Copyright (c) Michael Tetzlaff 2020
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

uniform sampler2D normalEstimate;
uniform sampler2D roughnessEstimate;

#ifndef MATERIAL_EXPLORATION_MODE
#define MATERIAL_EXPLORATION_MODE 0
#endif

#if MATERIAL_EXPLORATION_MODE
// For debugging or generating comparisons and figures.
#undef NORMAL_TEXTURE_ENABLED
#define NORMAL_TEXTURE_ENABLED 1
#include <shaders/colorappearance/textures.glsl>
#include <shaders/colorappearance/analytic.glsl>
#else
#include <shaders/colorappearance/imgspace.glsl>
#endif

#include <shaders/colorappearance/colorappearance_multi_as_single.glsl>
#include <shaders/relight/reflectanceequations.glsl>

#define COSINE_CUTOFF 0.0
