#ifndef ENV_SVD_UNPACK_GLSL
#define ENV_SVD_UNPACK_GLSL

#include "../colorappearance/svd_unpack.glsl"

#define HIGH_QUALITY_EIGENTEXTURE_COUNT 8

#define EIGENTEXTURE_RETURN_COUNT HIGH_QUALITY_EIGENTEXTURE_COUNT

#if HIGH_QUALITY_EIGENTEXTURE_COUNT > 2
#define SECONDARY_EIGENTEXTURE_COUNT (HIGH_QUALITY_EIGENTEXTURE_COUNT - 2)
#else
#define SECONDARY_EIGENTEXTURE_COUNT 0
#endif

#if HIGH_QUALITY_EIGENTEXTURE_COUNT > 0
#include "environmentweights.glsl"
#endif

#line 21 3005

uniform sampler2DArray environmentWeightsTexture;

struct LinearFilteringInfo
{
    ivec2 coords000;
    ivec2 coords001;
    ivec2 coords010;
    ivec2 coords011;
    ivec2 coords100;
    ivec2 coords101;
    ivec2 coords110;
    ivec2 coords111;
    vec2 interpolantsFloorLevel;
    vec2 interpolantsCeilLevel;
};

LinearFilteringInfo getFilteringInfo(vec2 texCoord, ivec3 floorLevelSize, ivec3 ceilLevelSize)
{
    LinearFilteringInfo result;

    vec2 texCoordsFloorLevel = texCoord * floorLevelSize.xy - 0.5;
    result.coords000 = clamp(ivec2(floor(texCoordsFloorLevel)), ivec2(0), floorLevelSize.xy - ivec2(1));
    result.coords110 = result.coords000 + 1;
    result.coords010 = ivec2(result.coords000.x, result.coords110.y);
    result.coords100 = ivec2(result.coords110.x, result.coords000.y);
    result.interpolantsFloorLevel = texCoordsFloorLevel - result.coords000;

    vec2 texCoordsCeilLevel = texCoord * ceilLevelSize.xy - 0.5;
    result.coords001 = clamp(ivec2(floor(texCoordsCeilLevel)), ivec2(0), ceilLevelSize.xy - ivec2(1));
    result.coords111 = result.coords001 + 1;
    result.coords011 = ivec2(result.coords001.x, result.coords111.y);
    result.coords101 = ivec2(result.coords111.x, result.coords001.y);
    result.interpolantsCeilLevel = texCoordsCeilLevel - result.coords001;

    return result;
}

vec3[4] getWeights(vec2 weightTexCoords[4], int layer)
{
    ivec2 weightTexSize = textureSize(environmentWeightsTexture, 0).xy;
    vec3 returnValues[4];

    for (int i = 0; i < 4; i++)
    {
        ivec2 truncatedTexCoords = min(ivec2(floor(weightTexCoords[i])), weightTexSize - 1);
        vec2 interpolants = weightTexCoords[i] - truncatedTexCoords;

        vec3 weight00 = texelFetch(environmentWeightsTexture, ivec3(truncatedTexCoords, layer), 0).rgb;
        vec3 weight01 = texelFetch(environmentWeightsTexture, ivec3(truncatedTexCoords, layer + 32), 0).rgb;
        vec3 weight10 = texelFetch(environmentWeightsTexture, ivec3(truncatedTexCoords, layer + 64), 0).rgb;
        vec3 weight11 = texelFetch(environmentWeightsTexture, ivec3(truncatedTexCoords, layer + 96), 0).rgb;

        returnValues[i] = mix(
            mix(weight00, weight01, interpolants.y),
            mix(weight10, weight11, interpolants.y),
            interpolants.x);
    }

    return returnValues;
}

vec3 getScaledEnvironmentShadingFromSVD(vec3 normalDir, vec3 specularColorRGB, vec3 roughness)
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

    vec3 roughnessSq = roughness * roughness;

    LinearFilteringInfo linearFiltering = getFilteringInfo(fTexCoord, eigentexturesFloorLevelSize, eigentexturesCeilLevelSize);

    vec2 weightCoords[4];
    weightCoords[0] = linearFiltering.coords000 / floorToEnv;
    weightCoords[1] = linearFiltering.coords010 / floorToEnv;
    weightCoords[2] = linearFiltering.coords100 / floorToEnv;
    weightCoords[3] = linearFiltering.coords110 / floorToEnv;

    vec3[] weights;

#if HIGH_QUALITY_EIGENTEXTURE_COUNT == 0
    weights = getWeights(weightCoords, 0);

    vec3 mfdGeomRoughnessSq =
        mix(mix(weights[0], weights[2], linearFiltering.interpolantsFloorLevel.x),
            mix(weights[1], weights[3], linearFiltering.interpolantsFloorLevel.x),
            linearFiltering.interpolantsFloorLevel.y);

    weights = getWeights(weightCoords, 1);

    vec3 mfdGeomRoughnessSqFresnelFactor =
        mix(mix(weights[0], weights[2], linearFiltering.interpolantsFloorLevel.x),
            mix(weights[1], weights[3], linearFiltering.interpolantsFloorLevel.x),
            linearFiltering.interpolantsFloorLevel.y);
#else
    float roughnessMono = sqrt(1.0 / (getLuminance(1.0 / roughness * roughness)));
    EnvironmentResult[HIGH_QUALITY_EIGENTEXTURE_COUNT] shading = computeSVDEnvironmentShading(1, fPosition, normalDir, roughnessMono);

    vec3 mfdGeomRoughnessSq = shading[0].baseFresnel.rgb;
    vec3 mfdGeomRoughnessSqFresnelFactor = shading[0].fresnelAdjustment.rgb;

    ivec3 viewWeightSize = textureSize(viewWeightTextures, 0) / ivec3(VIEW_WEIGHT_PACKING_X, VIEW_WEIGHT_PACKING_Y, 1);
    ivec2 block = min(ivec2(floor(fTexCoord * viewWeightSize.xy)), viewWeightSize.xy - 1);

    vec2 floorPixPerBlock = vec2(eigentexturesFloorLevelSize.xy) / vec2(viewWeightSize.xy);
    vec2 ceilPixPerBlock = vec2(eigentexturesCeilLevelSize.xy) / vec2(viewWeightSize.xy);

    for (int k = 0; k < HIGH_QUALITY_EIGENTEXTURE_COUNT - 1; k++)
    {
        vec4 tex000 = getSignedTexel(
            ivec3(clamp(linearFiltering.coords000, ivec2(block * floorPixPerBlock), ivec2((block + 1) * floorPixPerBlock - 1)), k),
            mipmapLevelFloor);

        vec4 tex001 = getSignedTexel(
            ivec3(clamp(linearFiltering.coords001, ivec2(block * ceilPixPerBlock),  ivec2((block + 1) * ceilPixPerBlock  - 1)), k),
            mipmapLevelCeil);

        vec4 tex010 = getSignedTexel(
            ivec3(clamp(linearFiltering.coords010, ivec2(block * floorPixPerBlock), ivec2((block + 1) * floorPixPerBlock - 1)), k),
            mipmapLevelFloor);

        vec4 tex011 = getSignedTexel(
            ivec3(clamp(linearFiltering.coords011, ivec2(block * ceilPixPerBlock),  ivec2((block + 1) * ceilPixPerBlock  - 1)), k),
            mipmapLevelCeil);

        vec4 tex100 = getSignedTexel(
            ivec3(clamp(linearFiltering.coords100, ivec2(block * floorPixPerBlock), ivec2((block + 1) * floorPixPerBlock - 1)), k),
            mipmapLevelFloor);

        vec4 tex101 = getSignedTexel(
            ivec3(clamp(linearFiltering.coords101, ivec2(block * ceilPixPerBlock),  ivec2((block + 1) * ceilPixPerBlock  - 1)), k),
            mipmapLevelCeil);

        vec4 tex110 = getSignedTexel(
            ivec3(clamp(linearFiltering.coords110, ivec2(block * floorPixPerBlock), ivec2((block + 1) * floorPixPerBlock - 1)), k),
            mipmapLevelFloor);

        vec4 tex111 = getSignedTexel(
            ivec3(clamp(linearFiltering.coords111, ivec2(block * ceilPixPerBlock),  ivec2((block + 1) * ceilPixPerBlock  - 1)), k),
            mipmapLevelCeil);

        vec4 tex = mix(mix(mix(tex000, tex100, linearFiltering.interpolantsFloorLevel.x),
                           mix(tex010, tex110, linearFiltering.interpolantsFloorLevel.x),
                           linearFiltering.interpolantsFloorLevel.y),
                       mix(mix(tex001, tex101, linearFiltering.interpolantsCeilLevel.x),
                           mix(tex011, tex111, linearFiltering.interpolantsCeilLevel.x),
                           linearFiltering.interpolantsCeilLevel.y),
                       mipmapLevelInterpolant);

        vec4 blendedTerm = vec4(shading[k + 1].baseFresnel.rgb, 1.0) * tex;
        vec3 blendedTermFresnel = shading[k + 1].fresnelAdjustment.rgb * tex.rgb;

        if (blendedTerm.a > 0)
        {
            mfdGeomRoughnessSq += blendedTerm.rgb / blendedTerm.a;
            mfdGeomRoughnessSqFresnelFactor += blendedTermFresnel.rgb / blendedTerm.a;
        }
    }
#endif

#if HIGH_QUALITY_EIGENTEXTURE_COUNT == 0
    for (int k = 0; k < 15; k++)
#else
    for (int k = HIGH_QUALITY_EIGENTEXTURE_COUNT - 1; k < 0*15; k++)
#endif
    {
        vec4 tex000 = getSignedTexel(ivec3(linearFiltering.coords000, k), mipmapLevelFloor);
        vec4 tex001 = getSignedTexel(ivec3(linearFiltering.coords001, k), mipmapLevelCeil);
        vec4 tex010 = getSignedTexel(ivec3(linearFiltering.coords010, k), mipmapLevelFloor);
        vec4 tex011 = getSignedTexel(ivec3(linearFiltering.coords011, k), mipmapLevelCeil);
        vec4 tex100 = getSignedTexel(ivec3(linearFiltering.coords100, k), mipmapLevelFloor);
        vec4 tex101 = getSignedTexel(ivec3(linearFiltering.coords101, k), mipmapLevelCeil);
        vec4 tex110 = getSignedTexel(ivec3(linearFiltering.coords110, k), mipmapLevelFloor);
        vec4 tex111 = getSignedTexel(ivec3(linearFiltering.coords111, k), mipmapLevelCeil);

        weights = getWeights(weightCoords, 2 * k + 2);

        vec4 blendedTerm =
            mix(mix(mix(vec4(weights[0] * 2.0 - 1.0, 1.0) * tex000,
                        vec4(weights[2] * 2.0 - 1.0, 1.0) * tex100,
                        linearFiltering.interpolantsFloorLevel.x),
                    mix(vec4(weights[1] * 2.0 - 1.0, 1.0) * tex010,
                        vec4(weights[3] * 2.0 - 1.0, 1.0) * tex110,
                        linearFiltering.interpolantsFloorLevel.x),
                    linearFiltering.interpolantsFloorLevel.y)  ,
                mix(mix(vec4(weights[0] * 2.0 - 1.0, 1.0) * tex001,
                        vec4(weights[2] * 2.0 - 1.0, 1.0) * tex101,
                        linearFiltering.interpolantsCeilLevel.x),
                    mix(vec4(weights[1] * 2.0 - 1.0, 1.0) * tex011,
                        vec4(weights[3] * 2.0 - 1.0, 1.0) * tex111,
                        linearFiltering.interpolantsCeilLevel.x),
                    linearFiltering.interpolantsCeilLevel.y),
                mipmapLevelInterpolant);

        weights = getWeights(weightCoords, 2 * k + 3);

        vec3 blendedTermFresnel =
            mix(mix(mix((weights[0] * 2.0 - 1.0) * tex000.rgb,
                        (weights[2] * 2.0 - 1.0) * tex100.rgb,
                        linearFiltering.interpolantsFloorLevel.x),
                    mix((weights[1] * 2.0 - 1.0) * tex010.rgb,
                        (weights[3] * 2.0 - 1.0) * tex110.rgb,
                        linearFiltering.interpolantsFloorLevel.x),
                    linearFiltering.interpolantsFloorLevel.y),
                mix(mix((weights[0] * 2.0 - 1.0) * tex001.rgb,
                        (weights[2] * 2.0 - 1.0) * tex101.rgb,
                        linearFiltering.interpolantsCeilLevel.x),
                    mix((weights[1] * 2.0 - 1.0) * tex011.rgb,
                        (weights[3] * 2.0 - 1.0) * tex111.rgb,
                        linearFiltering.interpolantsCeilLevel.x),
                    linearFiltering.interpolantsCeilLevel.y),
                mipmapLevelInterpolant);

        if (blendedTerm.w > 0)
        {
            mfdGeomRoughnessSq += blendedTerm.rgb / blendedTerm.a;
            mfdGeomRoughnessSqFresnelFactor += blendedTermFresnel.rgb / blendedTerm.a;
        }
    }

#if FRESNEL_EFFECT_ENABLED
    return mix(mfdGeomRoughnessSqFresnelFactor, mfdGeomRoughnessSq, specularColorRGB);
#else
    return mfdGeomRoughnessSq * specularColorRGB;
#endif
}

#endif // ENV_SVD_UNPACK_GLSL
