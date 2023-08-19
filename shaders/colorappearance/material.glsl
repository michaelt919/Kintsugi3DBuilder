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

#ifndef MATERIAL_GLSL
#define MATERIAL_GLSL

#line 17 1020

in vec2 fTexCoord;

#ifndef DIFFUSE_TEXTURE_ENABLED
#define DIFFUSE_TEXTURE_ENABLED 0
#endif

#ifndef SPECULAR_TEXTURE_ENABLED
#define SPECULAR_TEXTURE_ENABLED 0
#endif

#ifndef ROUGHNESS_TEXTURE_ENABLED
#define ROUGHNESS_TEXTURE_ENABLED 0
#endif

#ifndef NORMAL_TEXTURE_ENABLED
#define NORMAL_TEXTURE_ENABLED 0
#endif

#ifndef TANGENT_SPACE_NORMAL_MAP
#define TANGENT_SPACE_NORMAL_MAP 1
#endif

#ifndef UV_SCALE_ENABLED
#define UV_SCALE_ENABLED 0
#endif

#if UV_SCALE_ENABLED
#ifndef UV_SCALE
#define UV_SCALE 1.0
#endif
#endif

#ifndef NORMAL_MAP_SCALE_ENABLED
#define NORMAL_MAP_SCALE_ENABLED 0
#endif

#if NORMAL_MAP_SCALE_ENABLED
#ifndef NORMAL_MAP_SCALE
#define NORMAL_MAP_SCALE 1.0
#endif
#endif

#ifndef DEFAULT_DIFFUSE_COLOR
#define DEFAULT_DIFFUSE_COLOR (vec3(0.5))
#endif // DEFAULT_DIFFUSE_COLOR

#ifndef DEFAULT_SPECULAR_COLOR
#define DEFAULT_SPECULAR_COLOR (vec3(0.04))
#endif // DEFAULT_SPECULAR_COLOR

#ifndef DEFAULT_SPECULAR_ROUGHNESS
#define DEFAULT_SPECULAR_ROUGHNESS (0.5) // TODO pass in a default?
#endif

#if DIFFUSE_TEXTURE_ENABLED
uniform sampler2D diffuseMap;
#endif

#if SPECULAR_TEXTURE_ENABLED
uniform sampler2D specularMap;
#endif

#if ROUGHNESS_TEXTURE_ENABLED
uniform sampler2D roughnessMap;
#endif

#ifndef SMITH_MASKING_SHADOWING
#if ROUGHNESS_TEXTURE_ENABLED
#define SMITH_MASKING_SHADOWING 1
#endif
#endif

vec2 getTexCoords()
{
#if UV_SCALE_ENABLED
    vec2 scaledTexCoord = UV_SCALE * fTexCoord;
    return scaledTexCoord - floor(scaledTexCoord); // force texture repeat (TODO do this using texture configuration?)
#else
    return fTexCoord;
#endif
}

struct Material
{
    vec3 diffuseColor;
    vec3 specularColor;
    float roughness;
    float roughnessSq;
};

Material getMaterial()
{
    vec2 texCoords = getTexCoords();
    Material m;

#if DIFFUSE_TEXTURE_ENABLED
    m.diffuseColor = pow(texture(diffuseMap, texCoords).rgb, vec3(gamma));
#else
    m.diffuseColor = DEFAULT_DIFFUSE_COLOR;
#endif

#if SPECULAR_TEXTURE_ENABLED
    m.specularColor = max(vec3(0.04), pow(texture(specularMap, texCoords).rgb, vec3(gamma)));
#else
    m.specularColor = DEFAULT_SPECULAR_COLOR;
#endif

#if ROUGHNESS_TEXTURE_ENABLED
    float sqrtRoughness = texture(roughnessMap, texCoords)[0];
    m.roughness = sqrtRoughness * sqrtRoughness;
#else
    m.roughness = DEFAULT_SPECULAR_ROUGHNESS;
#endif
    m.roughnessSq = m.roughness * m.roughness;

    return m;
}

#if NORMAL_TEXTURE_ENABLED
uniform sampler2D normalMap;

#if TANGENT_SPACE_NORMAL_MAP

in vec3 fTangent;
in vec3 fBitangent;

vec3 getTangentSpaceNormal(vec2 texCoord)
{
    vec2 normalXY = texture(normalMap, texCoord).xy * 2 - 1;
    return vec3(normalXY, 1.0 - dot(normalXY, normalXY));
}
#endif // TANGENT_SPACE_NORMAL_MAP
#endif // NORMAL_TEXTURE_ENABLED

vec3 getRefinedWorldSpaceNormal(vec3 triangleNormal)
{
#if TANGENT_SPACE_NORMAL_MAP && NORMAL_TEXTURE_ENABLED
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
    - dot(triangleNormal, fBitangent) * triangleNormal
    - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);
#endif

    vec2 texCoords = getTexCoords();

    vec3 normalDir;
#if NORMAL_TEXTURE_ENABLED
#if (TANGENT_SPACE_NORMAL_MAP && NORMAL_MAP_SCALE_ENABLED)
    vec3 normalDirTS = normalize(getTangentSpaceNormal(texCoords) * vec3(NORMAL_MAP_SCALE, NORMAL_MAP_SCALE, 1.0));
    normalDir = tangentToObject * normalDirTS;
#elif TANGENT_SPACE_NORMAL_MAP
    vec2 normalDirXY = texture(normalMap, texCoords).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    normalDir = tangentToObject * normalDirTS;
#else
    normalDir = texture(normalMap, texCoords).xyz * 2 - vec3(1.0);
#endif // TANGENT_SPACE_NORMAL_MAP
#else
    normalDir = triangleNormal;
#endif // NORMAL_TEXTURE_ENABLED

    return normalDir;
}

#include "../colorappearance/reflectanceequations.glsl"

#endif // MATERIAL_GLSL