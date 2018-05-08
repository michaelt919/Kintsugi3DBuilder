#ifndef ENV_SVD_UNPACK_GLSL
#define ENV_SVD_UNPACK_GLSL

#include "../common/svd_unpack.glsl"
#include "../common/trilinear.glsl"

#line 7 3005

uniform sampler2DArray environmentWeightsTexture;

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

    ivec3 environmentWeightsSize = textureSize(environmentWeightsTexture, 0);
    ivec2 floorToEnv = eigentexturesFloorLevelSize.xy / environmentWeightsSize.xy;
    ivec2 ceilToEnv = eigentexturesCeilLevelSize.xy / environmentWeightsSize.xy;

    ivec2 coords[8];

    vec2 texCoordsFloorLevel = fTexCoord * eigentexturesFloorLevelSize.xy;
    coords[TRILINEAR_000] = min(ivec2(floor(texCoordsFloorLevel)), eigentexturesFloorLevelSize.xy - ivec2(1));
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

    ivec2 weightCoords000 = coords000 / floorToEnv;
    ivec2 weightCoords001 = coords001 / ceilToEnv;
    ivec2 weightCoords010 = coords010 / floorToEnv;
    ivec2 weightCoords011 = coords011 / ceilToEnv;
    ivec2 weightCoords100 = coords100 / floorToEnv;
    ivec2 weightCoords101 = coords101 / ceilToEnv;
    ivec2 weightCoords110 = coords110 / floorToEnv;
    ivec2 weightCoords111 = coords111 / ceilToEnv;

    vec4 weights000 = texelFetch(environmentWeightsTexture, ivec3(weightCoords000, 0), 0).xyz;
    vec4 weights001 = texelFetch(environmentWeightsTexture, ivec3(weightCoords001, 0), 0).xyz;
    vec4 weights010 = texelFetch(environmentWeightsTexture, ivec3(weightCoords010, 0), 0).xyz;
    vec4 weights011 = texelFetch(environmentWeightsTexture, ivec3(weightCoords011, 0), 0).xyz;
    vec4 weights100 = texelFetch(environmentWeightsTexture, ivec3(weightCoords100, 0), 0).xyz;
    vec4 weights101 = texelFetch(environmentWeightsTexture, ivec3(weightCoords101, 0), 0).xyz;
    vec4 weights110 = texelFetch(environmentWeightsTexture, ivec3(weightCoords110, 0), 0).xyz;
    vec4 weights111 = texelFetch(environmentWeightsTexture, ivec3(weightCoords111, 0), 0).xyz;

    vec3 sumMFDGeomRoughnessSq =
        mix(mix(mix(weights000,
                    weights100,
                    interpolantsFloorLevel.x),
                mix(weights010,
                    weights110,
                    interpolantsFloorLevel.x),
                interpolantsFloorLevel.y),
            mix(mix(weights001,
                    weights101,
                    interpolantsCeilLevel.x),
                mix(weights011,
                    weights111,
                    interpolantsCeilLevel.x),
                interpolantsCeilLevel.y),
            mipmapLevelInterpolant);


    weights000 = texelFetch(environmentWeightsTexture, ivec3(weightCoords000, 4), 0).xyz;
    weights001 = texelFetch(environmentWeightsTexture, ivec3(weightCoords001, 4), 0).xyz;
    weights010 = texelFetch(environmentWeightsTexture, ivec3(weightCoords010, 4), 0).xyz;
    weights011 = texelFetch(environmentWeightsTexture, ivec3(weightCoords011, 4), 0).xyz;
    weights100 = texelFetch(environmentWeightsTexture, ivec3(weightCoords100, 4), 0).xyz;
    weights101 = texelFetch(environmentWeightsTexture, ivec3(weightCoords101, 4), 0).xyz;
    weights110 = texelFetch(environmentWeightsTexture, ivec3(weightCoords110, 4), 0).xyz;
    weights111 = texelFetch(environmentWeightsTexture, ivec3(weightCoords111, 4), 0).xyz;

    vec3 sumMFDGeomRoughnessSqFresnelFactor =
        mix(mix(mix(weights000,
                    weights100,
                    interpolantsFloorLevel.x),
                mix(weights010,
                    weights110,
                    interpolantsFloorLevel.x),
                interpolantsFloorLevel.y),
            mix(mix(weights001,
                    weights101,
                    interpolantsCeilLevel.x),
                mix(weights011,
                    weights111,
                    interpolantsCeilLevel.x),
                interpolantsCeilLevel.y),
            mipmapLevelInterpolant);

    for (int k = 0; k < 4; k++)
    {
        vec4 weights000 = texelFetch(environmentWeightsTexture, ivec3(weightCoords000, k), 0).xyz * 2.0 - 1.0;
        vec4 weights001 = texelFetch(environmentWeightsTexture, ivec3(weightCoords001, k), 0).xyz * 2.0 - 1.0;
        vec4 weights010 = texelFetch(environmentWeightsTexture, ivec3(weightCoords010, k), 0).xyz * 2.0 - 1.0;
        vec4 weights011 = texelFetch(environmentWeightsTexture, ivec3(weightCoords011, k), 0).xyz * 2.0 - 1.0;
        vec4 weights100 = texelFetch(environmentWeightsTexture, ivec3(weightCoords100, k), 0).xyz * 2.0 - 1.0;
        vec4 weights101 = texelFetch(environmentWeightsTexture, ivec3(weightCoords101, k), 0).xyz * 2.0 - 1.0;
        vec4 weights110 = texelFetch(environmentWeightsTexture, ivec3(weightCoords110, k), 0).xyz * 2.0 - 1.0;
        vec4 weights111 = texelFetch(environmentWeightsTexture, ivec3(weightCoords111, k), 0).xyz * 2.0 - 1.0;

        vec2 tex000 = getSignedTexel(ivec3(coords000, k), mipmapLevelFloor).yw;
        vec2 tex001 = getSignedTexel(ivec3(coords001, k), mipmapLevelCeil).yw;
        vec2 tex010 = getSignedTexel(ivec3(coords010, k), mipmapLevelFloor).yw;
        vec2 tex011 = getSignedTexel(ivec3(coords011, k), mipmapLevelCeil).yw;
        vec2 tex100 = getSignedTexel(ivec3(coords100, k), mipmapLevelFloor).yw;
        vec2 tex101 = getSignedTexel(ivec3(coords101, k), mipmapLevelCeil).yw;
        vec2 tex110 = getSignedTexel(ivec3(coords110, k), mipmapLevelFloor).yw;
        vec2 tex111 = getSignedTexel(ivec3(coords111, k), mipmapLevelCeil).yw;

        vec4 blendedColor =
            mix(mix(mix(weights000 * tex000[0],
                        weights100 * tex100[0],
                        interpolantsFloorLevel.x),
                    mix(weights010 * tex010[0],
                        weights110 * tex110[0],
                        interpolantsFloorLevel.x),
                    interpolantsFloorLevel.y),
                mix(mix(weights001 * tex001[0],
                        weights101 * tex101[0],
                        interpolantsCeilLevel.x),
                    mix(weights011 * tex011[0],
                        weights111 * tex111[0],
                        interpolantsCeilLevel.x),
                    interpolantsCeilLevel.y),
                mipmapLevelInterpolant);

        float weight =
            mix(mix(mix(tex000[1],
                        tex100[1],
                        interpolantsFloorLevel.x),
                    mix(tex010[1],
                        tex110[1],
                        interpolantsFloorLevel.x),
                    interpolantsFloorLevel.y),
                mix(mix(tex001[1],
                        tex101[1],
                        interpolantsCeilLevel.x),
                    mix(tex011[1],
                        tex111[1],
                        interpolantsCeilLevel.x),
                    interpolantsCeilLevel.y),
                mipmapLevelInterpolant);

        if (weight > 0)
        {
            environmentWeights[k] = blendedColor / weight;
        }
    }

    return vec4(color + vec3(0.5), 1.0);
}

#endif // ENV_SVD_UNPACK_GLSL