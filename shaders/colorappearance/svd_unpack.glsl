/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#ifndef SVD_UNPACK_GLSL
#define SVD_UNPACK_GLSL

#include "colorappearance.glsl"

#line 19 1102

uniform sampler2DArray eigentextures;
uniform sampler2DArray viewWeightTextures;

// TODO define BLOCK_WIDTH and BLOCK_HEIGHT externally
#ifndef BLOCK_WIDTH
#define BLOCK_WIDTH 32 // For syntax highlighting
//#error BLOCK_WIDTH must be externally defined!
#endif

#ifndef BLOCK_HEIGHT
#define BLOCK_HEIGHT 32 // For syntax highlighting
//#error BLOCK_HEIGHT must be externally defined!
#endif

#define BLOCK_SIZE (ivec2(BLOCK_WIDTH, BLOCK_HEIGHT))

#ifndef EIGENTEXTURE_COUNT
#define EIGENTEXTURE_COUNT 4 // For syntax highlighting
#error EIGENTEXTURE_COUNT must be externally defined!
#endif

#ifndef VIEW_WEIGHT_PACKING_X
#define VIEW_WEIGHT_PACKING_X 2 // For syntax highlighting
#error VIEW_WEIGHT_PACKING_X must be externally defined!
#endif

#ifndef VIEW_WEIGHT_PACKING_Y
#define VIEW_WEIGHT_PACKING_Y 2 // For syntax highlighting
#error VIEW_WEIGHT_PACKING_Y must be externally defined!
#endif

ivec2 computeBlockStart(vec2 texCoords, ivec2 textureSize)
{
    return min(ivec2(floor(texCoords * textureSize)), textureSize.xy - ivec2(1)) / BLOCK_SIZE
            * ivec2(VIEW_WEIGHT_PACKING_X, VIEW_WEIGHT_PACKING_Y);
}

vec4 computeSVDViewWeights(ivec3 blockStart, int k)
{
    vec3 viewWeights = texelFetch(viewWeightTextures, blockStart + ivec3(k % VIEW_WEIGHT_PACKING_X, k / VIEW_WEIGHT_PACKING_X, 0), 0).xyz;
    if (viewWeights.y > 0)
    {
        return vec4((viewWeights * 255 - vec3(128.0)) / 127.0, 1.0);
    }
    else
    {
        return vec4(0.0);
    }


//    uint packedViewWeights = texelFetch(viewWeightTextures, blockStart + ivec3(k % VIEW_WEIGHT_PACKING_X, k / VIEW_WEIGHT_PACKING_X, 0), 0)[0];
//    uvec3 unpackedViewWeights = uvec3((packedViewWeights >> 9) & 0x7Fu, (packedViewWeights >> 4) & 0x1Fu, packedViewWeights & 0x0Fu);
//    if (unpackedViewWeights[0] != 0u)
//    {
//        vec3 unscaledWeights = vec3(
//            int(unpackedViewWeights[2] * 2u + unpackedViewWeights[0]) - 80,
//            int(unpackedViewWeights[0]) - 64,
//            int(unpackedViewWeights[1] * 2u + unpackedViewWeights[0]) - 96);
//
//        return vec4(unscaledWeights / 63.0, 1.0);
//    }
//    else
//    {
//        return vec4(0.0);
//    }
}

vec4 getSignedTexel(ivec3 coords, int mipmapLevel)
{
    vec3 scaledTexel = vec3(texelFetch(eigentextures, coords, mipmapLevel)[0]) * 255;
    if (scaledTexel.y > 0.5)
    {
        return vec4((scaledTexel - 128) / 127.0, 1.0);
    }
    else
    {
        return vec4(0.0);
    }
}

vec4 getResidual(int virtualIndex)
{
    int viewIndex = getViewIndex(virtualIndex);

    float mipmapLevel;
#ifdef GL_ARB_texture_query_lod
    mipmapLevel = textureQueryLOD(eigentextures, fTexCoord).x;
#else
    mipmapLevel = 0.0; // TODO better support for graphics cards without textureQueryLOD
#endif

    int mipmapLevelFloor = int(floor(mipmapLevel));
    int mipmapLevelCeil = mipmapLevelFloor + 1;
    float mipmapLevelInterpolant = mipmapLevel - mipmapLevelFloor;

    ivec3 eigentexturesFloorLevelSize = textureSize(eigentextures, mipmapLevelFloor);
    ivec3 eigentexturesCeilLevelSize = textureSize(eigentextures, mipmapLevelCeil);

    vec2 texCoordsFloorLevel = fTexCoord * eigentexturesFloorLevelSize.xy;
    ivec2 coords000 = min(ivec2(floor(texCoordsFloorLevel)), eigentexturesFloorLevelSize.xy - ivec2(1));
    ivec2 coords110 = coords000 + 1;
    ivec2 coords010 = ivec2(coords000.x, coords110.y);
    ivec2 coords100 = ivec2(coords110.x, coords000.y);
    vec2 interpolantsFloorLevel = texCoordsFloorLevel - coords000;

    vec2 texCoordsCeilLevel = fTexCoord * eigentexturesCeilLevelSize.xy;
    ivec2 coords001 = min(ivec2(floor(texCoordsCeilLevel)), eigentexturesCeilLevelSize.xy - ivec2(1));
    ivec2 coords111 = coords001 + 1;
    ivec2 coords011 = ivec2(coords001.x, coords111.y);
    ivec2 coords101 = ivec2(coords111.x, coords001.y);
    vec2 interpolantsCeilLevel = texCoordsCeilLevel - coords001;

    ivec3 eigentexturesSize = textureSize(eigentextures, 0);
    ivec3 blockStart000 = ivec3(computeBlockStart(vec2(coords000) / eigentexturesFloorLevelSize.xy, eigentexturesSize.xy), viewIndex);
    ivec3 blockStart001 = ivec3(computeBlockStart(vec2(coords001) / eigentexturesCeilLevelSize.xy, eigentexturesSize.xy), viewIndex);
    ivec3 blockStart010 = ivec3(computeBlockStart(vec2(coords010) / eigentexturesFloorLevelSize.xy, eigentexturesSize.xy), viewIndex);
    ivec3 blockStart011 = ivec3(computeBlockStart(vec2(coords011) / eigentexturesCeilLevelSize.xy, eigentexturesSize.xy), viewIndex);
    ivec3 blockStart100 = ivec3(computeBlockStart(vec2(coords100) / eigentexturesFloorLevelSize.xy, eigentexturesSize.xy), viewIndex);
    ivec3 blockStart101 = ivec3(computeBlockStart(vec2(coords101) / eigentexturesCeilLevelSize.xy, eigentexturesSize.xy), viewIndex);
    ivec3 blockStart110 = ivec3(computeBlockStart(vec2(coords110) / eigentexturesFloorLevelSize.xy, eigentexturesSize.xy), viewIndex);
    ivec3 blockStart111 = ivec3(computeBlockStart(vec2(coords111) / eigentexturesCeilLevelSize.xy, eigentexturesSize.xy), viewIndex);

    vec3 color = vec3(0.0);

    for (int k = 0; k < EIGENTEXTURE_COUNT; k++)
    {
        vec4 weights000 = computeSVDViewWeights(blockStart000, k);
        vec4 weights001 = computeSVDViewWeights(blockStart001, k);
        vec4 weights010 = computeSVDViewWeights(blockStart010, k);
        vec4 weights011 = computeSVDViewWeights(blockStart011, k);
        vec4 weights100 = computeSVDViewWeights(blockStart100, k);
        vec4 weights101 = computeSVDViewWeights(blockStart101, k);
        vec4 weights110 = computeSVDViewWeights(blockStart110, k);
        vec4 weights111 = computeSVDViewWeights(blockStart111, k);

        vec4 tex000 = getSignedTexel(ivec3(coords000, k), mipmapLevelFloor);
        vec4 tex001 = getSignedTexel(ivec3(coords001, k), mipmapLevelCeil);
        vec4 tex010 = getSignedTexel(ivec3(coords010, k), mipmapLevelFloor);
        vec4 tex011 = getSignedTexel(ivec3(coords011, k), mipmapLevelCeil);
        vec4 tex100 = getSignedTexel(ivec3(coords100, k), mipmapLevelFloor);
        vec4 tex101 = getSignedTexel(ivec3(coords101, k), mipmapLevelCeil);
        vec4 tex110 = getSignedTexel(ivec3(coords110, k), mipmapLevelFloor);
        vec4 tex111 = getSignedTexel(ivec3(coords111, k), mipmapLevelCeil);

        vec4 blendedColor =
            mix(mix(mix(weights000 * tex000,
                        weights100 * tex100,
                        interpolantsFloorLevel.x),
                    mix(weights010 * tex010,
                        weights110 * tex110,
                        interpolantsFloorLevel.x),
                    interpolantsFloorLevel.y),
                mix(mix(weights001 * tex001,
                        weights101 * tex101,
                        interpolantsCeilLevel.x),
                    mix(weights011 * tex011,
                        weights111 * tex111,
                        interpolantsCeilLevel.x),
                    interpolantsCeilLevel.y),
                mipmapLevelInterpolant);

        if (blendedColor.w > 0)
        {
            color += blendedColor.xyz / blendedColor.w;
        }
    }

    return vec4(color, 1.0);
}

#endif // SVD_UNPACK_GLSL
