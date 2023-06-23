/*
 *  Copyright (c) Michael Tetzlaff 2023
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#ifndef CONSTRUCT_TBN_GLSL
#define CONSTRUCT_TBN_GLSL

#line 17 9904

#ifndef GEOMETRY_TEXTURES_ENABLED
#define GEOMETRY_TEXTURES_ENABLED 0
#endif

#if GEOMETRY_TEXTURES_ENABLED
uniform sampler2D normalTex;
uniform sampler2D tangentTex;

vec3 getNormal()
{
    // Assume fTexCoord to be declared previously
    // Assume the texture is set to store floating-point elements that are in the [-1, 1] range.
    return normalize(texture(normalTex, fTexCoord).xyz);
}

mat3 constructTBNApprox()
{
    // Assume fTexCoord to be declared previously
    // Assume the texture is set to store floating-point elements that are in the [-1, 1] range.
    vec3 normal = texture(normalTex, fTexCoord).xyz;
    vec3 tangent = texture(tangentTex, fTexCoord).xyz; // No re-orthogonalization

    // Don't think we really need multiplication by tangent.w
    vec3 bitangent = normalize(cross(normal, tangent.xyz));

    // No re-orthogonalization
    return mat3(normalize(tangent), normalize(bitangent), normalize(normal));
}

mat3 constructTBNExact()
{
    // Assume fTexCoord to be declared previously
    // Assume the texture is set to store floating-point elements that are in the [-1, 1] range.
    vec3 normal = texture(normalTex, fTexCoord).xyz;
    vec3 tangent = texture(tangentTex, fTexCoord).xyz;
    tangent = normalize(tangent - dot(normal, tangent) * normal); // Re-orthogonalize

    // Don't think we really need multiplication by tangent.w
    vec3 bitangent = normalize(cross(normal, tangent.xyz));

    return mat3(tangent, bitangent, normal);
}


#else
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

vec3 getNormal()
{
    return normalize(fNormal);
}

mat3 constructTBNApprox()
{
    // No re-orthogonalization
    return mat3(normalize(fTangent), normalize(fBitangent), normalize(fNormal));
}

mat3 constructTBNExact()
{
    vec3 normal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(normal, fTangent) * normal); // Re-orthogonalize
    vec3 bitangent = normalize(fBitangent
        - dot(normal, fBitangent) * normal
        - dot(tangent, fBitangent) * tangent);
    return mat3(tangent, bitangent, normal);
}
#endif

#endif // CONSTRUCT_TBN_GLSL