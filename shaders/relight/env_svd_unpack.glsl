#ifndef ENV_SVD_UNPACK_GLSL
#define ENV_SVD_UNPACK_GLSL

#include "../svd_unpack.glsl"

#line 7 3005

vec4 getColor()
{
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
    ivec2 blockStart000 = computeBlockStart(vec2(coords000) / eigentexturesFloorLevelSize.xy, eigentexturesSize.xy);
    ivec2 blockStart001 = computeBlockStart(vec2(coords001) / eigentexturesCeilLevelSize.xy, eigentexturesSize.xy);
    ivec2 blockStart010 = computeBlockStart(vec2(coords010) / eigentexturesFloorLevelSize.xy, eigentexturesSize.xy);
    ivec2 blockStart011 = computeBlockStart(vec2(coords011) / eigentexturesCeilLevelSize.xy, eigentexturesSize.xy);
    ivec2 blockStart100 = computeBlockStart(vec2(coords100) / eigentexturesFloorLevelSize.xy, eigentexturesSize.xy);
    ivec2 blockStart101 = computeBlockStart(vec2(coords101) / eigentexturesCeilLevelSize.xy, eigentexturesSize.xy);
    ivec2 blockStart110 = computeBlockStart(vec2(coords110) / eigentexturesFloorLevelSize.xy, eigentexturesSize.xy);
    ivec2 blockStart111 = computeBlockStart(vec2(coords111) / eigentexturesCeilLevelSize.xy, eigentexturesSize.xy);

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

    return vec4(color + vec3(0.5), 1.0);
}

#endif // ENV_SVD_UNPACK_GLSL