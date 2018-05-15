#ifndef ENV_SVD_UNPACK_GLSL
#define ENV_SVD_UNPACK_GLSL

#include "../colorappearance/svd_unpack.glsl"

#line 7 3005

uniform sampler2DArray environmentWeightsTexture;

vec3[4] getWeights(vec2 weightTexCoords[4], int layer)
{
    ivec2 weightTexSize = textureSize(environmentWeightsTexture, 0).xy;
    vec3 returnValues[4];

    for (int i = 0; i < 4; i++)
    {
        ivec2 truncatedTexCoords = min(ivec2(floor(weightTexCoords[i])), weightTexSize - 1);
        vec2 interpolants = weightTexCoords[i] - truncatedTexCoords;

        vec3 weight00 = texelFetch(environmentWeightsTexture, ivec3(truncatedTexCoords, layer), 0).xyz;
        vec3 weight01 = texelFetch(environmentWeightsTexture, ivec3(truncatedTexCoords, layer + 8), 0).xyz;
        vec3 weight10 = texelFetch(environmentWeightsTexture, ivec3(truncatedTexCoords, layer + 16), 0).xyz;
        vec3 weight11 = texelFetch(environmentWeightsTexture, ivec3(truncatedTexCoords, layer + 24), 0).xyz;

        returnValues[i] = mix(
            mix(weight00, weight01, interpolants.y),
            mix(weight10, weight11, interpolants.y),
            interpolants.x);
    }

    return returnValues;
}

vec3 getScaledEnvironmentShadingFromSVD(vec3 specularColorXYZ, vec3 roughness)
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
    vec2 floorToEnv = vec2(eigentexturesFloorLevelSize.xy) / vec2(environmentWeightsSize.xy);
    vec2 ceilToEnv = vec2(eigentexturesCeilLevelSize.xy) / vec2(environmentWeightsSize.xy);

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

    vec2 weightCoords[4];
    weightCoords[0] = coords000 / floorToEnv;
    weightCoords[1] = coords010 / floorToEnv;
    weightCoords[2] = coords100 / floorToEnv;
    weightCoords[3] = coords110 / floorToEnv;

    vec3[] weights = getWeights(weightCoords, 0);

    vec3 roughnessSq = roughness * roughness;

    vec3 mfdGeomRoughnessSq = roughnessSq *
        mix(mix(weights[0], weights[2], interpolantsFloorLevel.x),
            mix(weights[1], weights[3], interpolantsFloorLevel.x),
            interpolantsFloorLevel.y);

    weights = getWeights(weightCoords, 4);

    vec3 mfdGeomRoughnessSqFresnelFactor = roughnessSq *
        mix(mix(weights[0], weights[2], interpolantsFloorLevel.x),
            mix(weights[1], weights[3], interpolantsFloorLevel.x),
            interpolantsFloorLevel.y);

    for (int k = 0; k < 3; k++)
    {
        vec4 tex000 = getSignedTexel(ivec3(coords000, k), mipmapLevelFloor);
        vec4 tex001 = getSignedTexel(ivec3(coords001, k), mipmapLevelCeil);
        vec4 tex010 = getSignedTexel(ivec3(coords010, k), mipmapLevelFloor);
        vec4 tex011 = getSignedTexel(ivec3(coords011, k), mipmapLevelCeil);
        vec4 tex100 = getSignedTexel(ivec3(coords100, k), mipmapLevelFloor);
        vec4 tex101 = getSignedTexel(ivec3(coords101, k), mipmapLevelCeil);
        vec4 tex110 = getSignedTexel(ivec3(coords110, k), mipmapLevelFloor);
        vec4 tex111 = getSignedTexel(ivec3(coords111, k), mipmapLevelCeil);

        weights = getWeights(weightCoords, k + 1);

        vec4 blendedTerm =
            mix(mix(mix(vec4(weights[0] * 2.0 - 1.0, 1.0) * tex000,
                        vec4(weights[2] * 2.0 - 1.0, 1.0) * tex100,
                        interpolantsFloorLevel.x),
                    mix(vec4(weights[1] * 2.0 - 1.0, 1.0) * tex010,
                        vec4(weights[3] * 2.0 - 1.0, 1.0) * tex110,
                        interpolantsFloorLevel.x),
                    interpolantsFloorLevel.y)  ,
                mix(mix(vec4(weights[0] * 2.0 - 1.0, 1.0) * tex001,
                        vec4(weights[2] * 2.0 - 1.0, 1.0) * tex101,
                        interpolantsCeilLevel.x),
                    mix(vec4(weights[1] * 2.0 - 1.0, 1.0) * tex011,
                        vec4(weights[3] * 2.0 - 1.0, 1.0) * tex111,
                        interpolantsCeilLevel.x),
                    interpolantsCeilLevel.y),
                mipmapLevelInterpolant);

        weights = getWeights(weightCoords, k + 5);

        vec3 blendedTermFresnel =
            mix(mix(mix((weights[0] * 2.0 - 1.0) * tex000.xyz,
                        (weights[2] * 2.0 - 1.0) * tex100.xyz,
                        interpolantsFloorLevel.x),
                    mix((weights[1] * 2.0 - 1.0) * tex010.xyz,
                        (weights[3] * 2.0 - 1.0) * tex110.xyz,
                        interpolantsFloorLevel.x),
                    interpolantsFloorLevel.y),
                mix(mix((weights[0] * 2.0 - 1.0) * tex001.xyz,
                        (weights[2] * 2.0 - 1.0) * tex101.xyz,
                        interpolantsCeilLevel.x),
                    mix((weights[1] * 2.0 - 1.0) * tex011.xyz,
                        (weights[3] * 2.0 - 1.0) * tex111.xyz,
                        interpolantsCeilLevel.x),
                    interpolantsCeilLevel.y),
                mipmapLevelInterpolant);

        if (blendedTerm.w > 0)
        {
            mfdGeomRoughnessSq += blendedTerm.xyz / blendedTerm.w;
            mfdGeomRoughnessSqFresnelFactor += blendedTermFresnel.xyz / blendedTerm.w;
        }
    }

#if FRESNEL_EFFECT_ENABLED
    return mix(mfdGeomRoughnessSqFresnelFactor, mfdGeomRoughnessSq, specularColorXYZ);
#else
    return mfdGeomRoughnessSq * specularColorXYZ;
#endif
}

#endif // ENV_SVD_UNPACK_GLSL
