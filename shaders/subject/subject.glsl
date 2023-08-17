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

#ifndef SUBJECT_GLSL
#define SUBJECT_GLSL

#line 14 3000

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out int fragObjectID;

uniform mat4 model_view;
uniform mat4 fullProjection;
uniform vec3 viewPos;


#ifndef RELIGHTING_ENABLED
#define RELIGHTING_ENABLED 1
#endif

#ifndef SPOTLIGHTS_ENABLED
#define SPOTLIGHTS_ENABLED 0
#endif

#ifndef SHADOWS_ENABLED
#define SHADOWS_ENABLED 0
#endif

#ifndef FRESNEL_EFFECT_ENABLED
#define FRESNEL_EFFECT_ENABLED 0
#endif

#ifndef ENVIRONMENT_ILLUMINATION_ENABLED
#define ENVIRONMENT_ILLUMINATION_ENABLED 1
#endif

#ifndef VIRTUAL_LIGHT_COUNT
#if RELIGHTING_ENABLED
#define VIRTUAL_LIGHT_COUNT 4
#else
#define VIRTUAL_LIGHT_COUNT 1
#endif
#endif

#include "tonemap.glsl"

uniform int objectID;

#if VIRTUAL_LIGHT_COUNT > 0

uniform vec3 lightIntensityVirtual[VIRTUAL_LIGHT_COUNT];

#if RELIGHTING_ENABLED
uniform vec3 lightPosVirtual[VIRTUAL_LIGHT_COUNT];
uniform vec3 lightOrientationVirtual[VIRTUAL_LIGHT_COUNT];

#if SPOTLIGHTS_ENABLED
uniform float lightSpotSizeVirtual[VIRTUAL_LIGHT_COUNT];
uniform float lightSpotTaperVirtual[VIRTUAL_LIGHT_COUNT];
#endif // SPOTLIGHTS_ENABLED

#if SHADOWS_ENABLED
uniform sampler2DArray shadowMaps;
uniform mat4 lightMatrixVirtual[VIRTUAL_LIGHT_COUNT];
#endif // SHADOWS_ENABLED

#endif // RELIGHTING_ENABLED
#endif // VIRTUAL_LIGHT_COUNT > 0

#endif // SUBJECT_GLSL
