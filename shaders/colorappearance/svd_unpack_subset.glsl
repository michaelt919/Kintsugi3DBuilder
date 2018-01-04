#ifndef SVD_UNPACK_GLSL
#define SVD_UNPACK_GLSL

#include "colorappearance_subset.glsl"

#line 7 1102

uniform sampler2DArray eigentextures;
uniform usampler2DArray viewWeightTextures;
uniform ivec2 blockSize;

#define MAX_EIGENTEXTURES 16
#define VIEW_WEIGHT_PACKING_X 4
#define VIEW_WEIGHT_PACKING_Y 4

ivec2 computeBlockStart(vec2 texCoords, ivec2 textureSize)
{
    return min(ivec2(floor(texCoords * textureSize)), textureSize.xy - ivec2(1)) / blockSize
            * ivec2(VIEW_WEIGHT_PACKING_X, VIEW_WEIGHT_PACKING_Y);
}

vec3 computeSVDViewWeights(ivec3 blockStart, int k)
{
    uint packedViewWeights = texelFetch(viewWeightTextures, blockStart + ivec3(k % VIEW_WEIGHT_PACKING_X, k / VIEW_WEIGHT_PACKING_X, 0), 0)[0];
    uvec3 unpackedViewWeights = uvec3((packedViewWeights >> 9) & 0x7Fu, (packedViewWeights >> 4) & 0x1Fu, packedViewWeights & 0x0Fu);
    if (unpackedViewWeights[0] != 0u)
    {
        vec3 unscaledWeights = vec3(
            int(unpackedViewWeights[2] * 2u + unpackedViewWeights[0]) - 80,
            int(unpackedViewWeights[0]) - 64,
            int(unpackedViewWeights[1] * 2u + unpackedViewWeights[0]) - 96);

        return unscaledWeights / 63.0;
    }
    else
    {
        return vec3(0);
    }
}

float getSignedTexel(ivec3 coords, int mipmapLevel)
{
    float scaledTexel = texelFetch(eigentextures, coords, mipmapLevel)[0] * 255;
    if (scaledTexel > 0.5)
    {
        return (scaledTexel - 127) / 127.0;
    }
    else
    {
        return 0.0;
    }
}

vec4 getColor(int virtualIndex)
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

    for (int k = 0; k < MAX_EIGENTEXTURES; k++)
    {
        vec3 weights000 = computeSVDViewWeights(blockStart000, k);
        vec3 weights001 = computeSVDViewWeights(blockStart001, k);
        vec3 weights010 = computeSVDViewWeights(blockStart010, k);
        vec3 weights011 = computeSVDViewWeights(blockStart011, k);
        vec3 weights100 = computeSVDViewWeights(blockStart100, k);
        vec3 weights101 = computeSVDViewWeights(blockStart101, k);
        vec3 weights110 = computeSVDViewWeights(blockStart110, k);
        vec3 weights111 = computeSVDViewWeights(blockStart111, k);

        float tex000 = getSignedTexel(ivec3(coords000, k), mipmapLevelFloor);
        float tex001 = getSignedTexel(ivec3(coords001, k), mipmapLevelCeil);
        float tex010 = getSignedTexel(ivec3(coords010, k), mipmapLevelFloor);
        float tex011 = getSignedTexel(ivec3(coords011, k), mipmapLevelCeil);
        float tex100 = getSignedTexel(ivec3(coords100, k), mipmapLevelFloor);
        float tex101 = getSignedTexel(ivec3(coords101, k), mipmapLevelCeil);
        float tex110 = getSignedTexel(ivec3(coords110, k), mipmapLevelFloor);
        float tex111 = getSignedTexel(ivec3(coords111, k), mipmapLevelCeil);

        color +=
            mix(mix(mix(weights000 * tex000, weights100 * tex100, interpolantsFloorLevel.x),
                mix(weights010 * tex010, weights110 * tex110, interpolantsFloorLevel.x),
                interpolantsFloorLevel.y),
            mix(mix(weights001 * tex001, weights101 * tex101, interpolantsCeilLevel.x),
                mix(weights011 * tex011, weights111 * tex111, interpolantsCeilLevel.x),
                interpolantsCeilLevel.y),
            mipmapLevelInterpolant);
    }

    return vec4(color + vec3(0.5), 1.0);
}

#endif // SVD_UNPACK_GLSL
