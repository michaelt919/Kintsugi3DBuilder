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

#line 17 3000

in vec3 fPosition;

uniform mat4 model_view;
uniform mat4 fullProjection;
uniform vec3 viewPos;

layout(location = 0) out vec4 fragColor;

#ifndef FRESNEL_EFFECT_ENABLED
#define FRESNEL_EFFECT_ENABLED 0
#else
#ifndef RELIGHTING_ENABLED
#undef FRESNEL_EFFECT_ENABLED
#define FRESNEL_EFFECT_ENABLED 0
#elif !RELIGHTING_ENABLED // RELIGHTING_ENABLED is defined
#undef FRESNEL_EFFECT_ENABLED
#define FRESNEL_EFFECT_ENABLED 0
#endif // ifndef RELIGHTING_ENABLED
#endif // ifndef FRESNEL_EFFECT_ENABLED

#ifndef VIRTUAL_LIGHT_COUNT
#ifdef RELIGHTING_ENABLED
#if RELIGHTING_ENABLED
#define VIRTUAL_LIGHT_COUNT 4
#else // !RELIGHTING_ENABLED
#define VIRTUAL_LIGHT_COUNT 1
#endif // RELIGHTING_ENABLED
#else // RELIGHTING_ENABLED not defined
#define VIRTUAL_LIGHT_COUNT 1
#endif // ifdef RELIGHTING_ENABLED
#endif // ifdef VIRTUAL_LIGHT_COUNT

vec3 getLightVectorVirtual(int lightIndex);

#include "tonemap.glsl"

struct ViewingParameters
{
    vec3 normalDir;
    float nDotV;
    vec3 viewDir;
};

struct LightingParameters
{
    int lightIndex;
    vec3 normalDir;
    float nDotV;
    vec3 viewDir;
    float nDotL;
    vec3 lightDir;
    vec3 lightDirUnnorm;
    float lightDistSquared;
    float nDotH;
    vec3 halfDir;
    float hDotV;
};

ViewingParameters buildViewingParameters(vec3 normalDir, vec3 viewDirUnnorm)
{
    ViewingParameters v;
    v.normalDir = normalDir;
    v.viewDir = normalize(viewDirUnnorm);
    v.nDotV = max(0.0, dot(v.normalDir, v.viewDir));
    return v;
}

void calculateLightingCosines(inout LightingParameters l)
{
    l.nDotH = max(0, dot(l.normalDir, l.halfDir));
    l.nDotL = max(0, dot(l.normalDir, l.lightDir));
    l.nDotV = max(0, dot(l.normalDir, l.viewDir));
    l.hDotV = max(0, dot(l.halfDir, l.viewDir));
}

void calculateLightingParameters(inout LightingParameters l)
{
    l.lightDistSquared = dot(l.lightDirUnnorm, l.lightDirUnnorm);
    l.lightDir = l.lightDirUnnorm * inversesqrt(l.lightDistSquared);
    l.halfDir = normalize(l.viewDir + l.lightDir);
    calculateLightingCosines(l);
}

LightingParameters buildLightingParameters(int lightIndex, vec3 normalDir, vec3 viewDirUnnorm, vec3 lightDirUnnorm)
{
    LightingParameters l;
    l.lightIndex = lightIndex;
    l.normalDir = normalDir;
    l.viewDir = normalize(viewDirUnnorm);
    l.lightDirUnnorm = lightDirUnnorm;
    calculateLightingParameters(l);
    return l;
}

LightingParameters buildLightingParameters(int lightIndex, vec3 lightDirUnnorm, ViewingParameters v)
{
    LightingParameters l;
    l.lightIndex = lightIndex;
    l.normalDir = v.normalDir;
    l.viewDir = v.viewDir;
    l.lightDirUnnorm = lightDirUnnorm;
    calculateLightingParameters(l);
    return l;
}

ViewingParameters getViewingParameters(LightingParameters l)
{
    ViewingParameters v;
    v.viewDir = l.viewDir;
    v.normalDir = l.normalDir;
    v.nDotV = l.nDotV;
    return v;
}

#include "environment.glsl"

#endif // SUBJECT_GLSL
