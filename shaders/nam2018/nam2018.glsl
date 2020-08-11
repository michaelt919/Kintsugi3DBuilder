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

uniform sampler2D normalMap;
uniform sampler2D roughnessMap;

#include <shaders/colorappearance/imgspace.glsl>

//// For debugging
//#define ANALYTIC_METAL 0
//#define ANALYTIC_BUMP_HEIGHT 0.0
//vec3 getNormal(vec2 texCoord) { return vec3(0, 0, 1); }
//#include <shaders/colorappearance/analytic.glsl>

#include <shaders/colorappearance/colorappearance_multi_as_single.glsl>
#include <shaders/relight/reflectanceequations.glsl>
