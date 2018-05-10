#ifndef ENV_SVD_UNPACK_GLSL
#define ENV_SVD_UNPACK_GLSL

#include "../colorappearance/svd_unpack.glsl"

#line 7 3005

uniform sampler2DArray environmentWeightsTexture;

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

    ivec2 weightCoords00 = ivec2(round(coords000 / floorToEnv));
    ivec2 weightCoords01 = ivec2(round(coords010 / floorToEnv));
    ivec2 weightCoords10 = ivec2(round(coords100 / floorToEnv));
    ivec2 weightCoords11 = ivec2(round(coords110 / floorToEnv));

    vec3 weights000 = texelFetch(environmentWeightsTexture, ivec3(weightCoords00, 0), 0).xyz;
    vec3 weights001 = texelFetch(environmentWeightsTexture, ivec3(weightCoords00, 0), 0).xyz;
    vec3 weights010 = texelFetch(environmentWeightsTexture, ivec3(weightCoords01, 0), 0).xyz;
    vec3 weights011 = texelFetch(environmentWeightsTexture, ivec3(weightCoords01, 0), 0).xyz;
    vec3 weights100 = texelFetch(environmentWeightsTexture, ivec3(weightCoords10, 0), 0).xyz;
    vec3 weights101 = texelFetch(environmentWeightsTexture, ivec3(weightCoords10, 0), 0).xyz;
    vec3 weights110 = texelFetch(environmentWeightsTexture, ivec3(weightCoords11, 0), 0).xyz;
    vec3 weights111 = texelFetch(environmentWeightsTexture, ivec3(weightCoords11, 0), 0).xyz;

    vec3 roughnessSq = roughness * roughness;

    vec3 mfdGeomRoughnessSq = roughnessSq *
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


    weights000 = texelFetch(environmentWeightsTexture, ivec3(weightCoords00, 4), 0).xyz;
    weights001 = texelFetch(environmentWeightsTexture, ivec3(weightCoords00, 4), 0).xyz;
    weights010 = texelFetch(environmentWeightsTexture, ivec3(weightCoords01, 4), 0).xyz;
    weights011 = texelFetch(environmentWeightsTexture, ivec3(weightCoords01, 4), 0).xyz;
    weights100 = texelFetch(environmentWeightsTexture, ivec3(weightCoords10, 4), 0).xyz;
    weights101 = texelFetch(environmentWeightsTexture, ivec3(weightCoords10, 4), 0).xyz;
    weights110 = texelFetch(environmentWeightsTexture, ivec3(weightCoords11, 4), 0).xyz;
    weights111 = texelFetch(environmentWeightsTexture, ivec3(weightCoords11, 4), 0).xyz;

    vec3 mfdGeomRoughnessSqFresnelFactor = roughnessSq *
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

    for (int k = 0; k < 3; k++)
    {
        weights000 = texelFetch(environmentWeightsTexture, ivec3(weightCoords00, k + 1), 0).xyz * 2.0 - 1.0;
        weights001 = texelFetch(environmentWeightsTexture, ivec3(weightCoords00, k + 1), 0).xyz * 2.0 - 1.0;
        weights010 = texelFetch(environmentWeightsTexture, ivec3(weightCoords01, k + 1), 0).xyz * 2.0 - 1.0;
        weights011 = texelFetch(environmentWeightsTexture, ivec3(weightCoords01, k + 1), 0).xyz * 2.0 - 1.0;
        weights100 = texelFetch(environmentWeightsTexture, ivec3(weightCoords10, k + 1), 0).xyz * 2.0 - 1.0;
        weights101 = texelFetch(environmentWeightsTexture, ivec3(weightCoords10, k + 1), 0).xyz * 2.0 - 1.0;
        weights110 = texelFetch(environmentWeightsTexture, ivec3(weightCoords11, k + 1), 0).xyz * 2.0 - 1.0;
        weights111 = texelFetch(environmentWeightsTexture, ivec3(weightCoords11, k + 1), 0).xyz * 2.0 - 1.0;

        vec3 fresnelFactorWeights000 = texelFetch(environmentWeightsTexture, ivec3(weightCoords00, k + 5), 0).xyz * 2.0 - 1.0;
        vec3 fresnelFactorWeights001 = texelFetch(environmentWeightsTexture, ivec3(weightCoords00, k + 5), 0).xyz * 2.0 - 1.0;
        vec3 fresnelFactorWeights010 = texelFetch(environmentWeightsTexture, ivec3(weightCoords01, k + 5), 0).xyz * 2.0 - 1.0;
        vec3 fresnelFactorWeights011 = texelFetch(environmentWeightsTexture, ivec3(weightCoords01, k + 5), 0).xyz * 2.0 - 1.0;
        vec3 fresnelFactorWeights100 = texelFetch(environmentWeightsTexture, ivec3(weightCoords10, k + 5), 0).xyz * 2.0 - 1.0;
        vec3 fresnelFactorWeights101 = texelFetch(environmentWeightsTexture, ivec3(weightCoords10, k + 5), 0).xyz * 2.0 - 1.0;
        vec3 fresnelFactorWeights110 = texelFetch(environmentWeightsTexture, ivec3(weightCoords11, k + 5), 0).xyz * 2.0 - 1.0;
        vec3 fresnelFactorWeights111 = texelFetch(environmentWeightsTexture, ivec3(weightCoords11, k + 5), 0).xyz * 2.0 - 1.0;

        vec4 tex000 = getSignedTexel(ivec3(coords000, k), mipmapLevelFloor);
        vec4 tex001 = getSignedTexel(ivec3(coords001, k), mipmapLevelCeil);
        vec4 tex010 = getSignedTexel(ivec3(coords010, k), mipmapLevelFloor);
        vec4 tex011 = getSignedTexel(ivec3(coords011, k), mipmapLevelCeil);
        vec4 tex100 = getSignedTexel(ivec3(coords100, k), mipmapLevelFloor);
        vec4 tex101 = getSignedTexel(ivec3(coords101, k), mipmapLevelCeil);
        vec4 tex110 = getSignedTexel(ivec3(coords110, k), mipmapLevelFloor);
        vec4 tex111 = getSignedTexel(ivec3(coords111, k), mipmapLevelCeil);

        vec4 blendedTerm =
            mix(mix(mix(vec4(weights000, 1.0) * tex000,
                        vec4(weights100, 1.0) * tex100,
                        interpolantsFloorLevel.x),
                    mix(vec4(weights010, 1.0) * tex010,
                        vec4(weights110, 1.0) * tex110,
                        interpolantsFloorLevel.x),
                    interpolantsFloorLevel.y)  ,
                mix(mix(vec4(weights001, 1.0) * tex001,
                        vec4(weights101, 1.0) * tex101,
                        interpolantsCeilLevel.x),
                    mix(vec4(weights011, 1.0) * tex011,
                        vec4(weights111, 1.0) * tex111,
                        interpolantsCeilLevel.x),
                    interpolantsCeilLevel.y),
                mipmapLevelInterpolant);

        vec3 blendedTermFresnel =
            mix(mix(mix(fresnelFactorWeights000 * tex000.xyz,
                        fresnelFactorWeights100 * tex100.xyz,
                        interpolantsFloorLevel.x),
                    mix(fresnelFactorWeights010 * tex010.xyz,
                        fresnelFactorWeights110 * tex110.xyz,
                        interpolantsFloorLevel.x),
                    interpolantsFloorLevel.y),
                mix(mix(fresnelFactorWeights001 * tex001.xyz,
                        fresnelFactorWeights101 * tex101.xyz,
                        interpolantsCeilLevel.x),
                    mix(fresnelFactorWeights011 * tex011.xyz,
                        fresnelFactorWeights111 * tex111.xyz,
                        interpolantsCeilLevel.x),
                    interpolantsCeilLevel.y),
                mipmapLevelInterpolant);

        if (blendedTerm.w > 0)
        {
            mfdGeomRoughnessSq += blendedTerm.xyz / blendedTerm.w;
//            mfdGeomRoughnessSqFresnelFactor += blendedTermFresnel.xyz / blendedTerm.w;
        }
    }

    return mfdGeomRoughnessSq * specularColorXYZ;//mix(mfdGeomRoughnessSqFresnelFactor, mfdGeomRoughnessSq, specularColorXYZ);
}

#endif // ENV_SVD_UNPACK_GLSL
