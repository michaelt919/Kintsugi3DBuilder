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

#include "linearize.glsl"

#line 19 1020

in vec2 fTexCoord;

#ifndef TEXTURE_DIFFUSE
#define TEXTURE_DIFFUSE 0
#endif

#ifndef TEXTURE_SPECULAR
#define TEXTURE_SPECULAR 0
#endif

#ifndef TEXTURE_ROUGHNESS
#define TEXTURE_ROUGHNESS 0
#endif

#ifndef TEXTURE_NORMAL
#define TEXTURE_NORMAL 0
#endif

#ifndef TEXTURE_OCCLUSION
#define TEXTURE_OCCLUSION 0
#endif

#ifndef TEXTURE_ALBEDO
#define TEXTURE_ALBEDO 0
#endif

#ifndef TEXTURE_ORM
#define TEXTURE_ORM 0
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

#ifndef DEFAULT_AMBIENT_OCCLUSION
#define DEFAULT_AMBIENT_OCCLUSION (1.0)
#endif

#if TEXTURE_DIFFUSE
uniform sampler2D tex_diffuse;
#endif

#if TEXTURE_SPECULAR
uniform sampler2D tex_specular;
#endif

#if TEXTURE_ROUGHNESS
uniform sampler2D tex_roughness;
#endif

#if TEXTURE_OCCLUSION
uniform sampler2D tex_occlusion;
#endif

#if TEXTURE_ALBEDO
uniform sampler2D tex_albedo;
#endif

#if TEXTURE_ORM
uniform sampler2D tex_orm;
#endif

#ifndef SMITH_MASKING_SHADOWING
#if TEXTURE_ROUGHNESS
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
    float occlusion;
    vec3 specularColor;
    float roughness;
};

#define USE_ALBEDO_ORM (TEXTURE_ORM && TEXTURE_ALBEDO && (!TEXTURE_DIFFUSE || !TEXTURE_SPECULAR))
#define USE_ORM (TEXTURE_ORM && (!TEXTURE_OCCLUSION || !TEXTURE_ROUGHNESS || USE_ALBEDO_ORM))

Material getMaterial()
{
    vec2 texCoords = getTexCoords();
    Material m;

#if USE_ORM
#if USE_ALBEDO_ORM
    vec3 albedo = sRGBToLinear(texture(tex_albedo, texCoords).rgb);
#endif
    vec3 orm = texture(tex_orm, texCoords).rgb;
#endif

#if TEXTURE_DIFFUSE
    m.diffuseColor = sRGBToLinear(texture(tex_diffuse, texCoords).rgb);
#elif USE_ALBEDO_ORM
    m.diffuseColor = albedo - albedo * orm[2];
#else
    m.diffuseColor = DEFAULT_DIFFUSE_COLOR;
#endif

#if TEXTURE_SPECULAR
    m.specularColor = sRGBToLinear(texture(tex_specular, texCoords).rgb);
#elif USE_ALBEDO_ORM
    m.specularColor = mix(vec3(0.04), albedo, orm[2]);
#else
    m.specularColor = DEFAULT_SPECULAR_COLOR;
#endif

#if USE_ALBEDO_ORM
    m.roughness = orm[1] * orm[1];
#elif TEXTURE_ROUGHNESS
    float sqrtRoughness = texture(tex_roughness, texCoords)[0];
    m.roughness = sqrtRoughness * sqrtRoughness;
#else
    m.roughness = DEFAULT_SPECULAR_ROUGHNESS;
#endif

#if USE_ALBEDO_ORM
    m.occlusion = orm[0];
#elif TEXTURE_OCCLUSION
    m.occlusion = texture(tex_occlusion, texCoords)[0];
#else
    m.occlusion = DEFAULT_AMBIENT_OCCLUSION;
#endif

    return m;
}

#if TEXTURE_NORMAL
uniform sampler2D tex_normal;

#if TANGENT_SPACE_NORMAL_MAP

in vec3 fTangent;
in vec3 fBitangent;

vec3 getTangentSpaceNormal(vec2 texCoord)
{
    vec2 normalXY = texture(tex_normal, texCoord).xy * 2 - 1;
    return vec3(normalXY, 1.0 - dot(normalXY, normalXY));
}
#endif // TANGENT_SPACE_NORMAL_MAP
#endif // TEXTURE_NORMAL

vec3 getRefinedWorldSpaceNormal(vec3 triangleNormal)
{
#if TANGENT_SPACE_NORMAL_MAP && TEXTURE_NORMAL
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(triangleNormal, fBitangent) * triangleNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);
#endif

    vec2 texCoords = getTexCoords();

    vec3 normalDir;
#if TEXTURE_NORMAL
#if (TANGENT_SPACE_NORMAL_MAP && NORMAL_MAP_SCALE_ENABLED)
    vec3 normalDirTS = normalize(getTangentSpaceNormal(texCoords) * vec3(NORMAL_MAP_SCALE, NORMAL_MAP_SCALE, 1.0));
    normalDir = tangentToObject * normalDirTS;
#elif TANGENT_SPACE_NORMAL_MAP
    vec2 normalDirXY = texture(tex_normal, texCoords).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    normalDir = tangentToObject * normalDirTS;
#else
    normalDir = texture(tex_normal, texCoords).xyz * 2 - vec3(1.0);
#endif // TANGENT_SPACE_NORMAL_MAP
#else
    normalDir = triangleNormal;
#endif // TEXTURE_NORMAL

    return normalDir;
}

#include "../colorappearance/reflectanceequations.glsl"

#endif // MATERIAL_GLSL