#ifndef ENV_SVD_UNPACK_GLSL
#define ENV_SVD_UNPACK_GLSL

#include "../colorappearance/svd_unpack.glsl"

#define ACTIVE_EIGENTEXTURE_COUNT (EIGENTEXTURE_COUNT + 1)

#include "environmentweights.glsl"

#line 11 3007

vec3 getScaledEnvironmentShadingFromSVD(vec3 normalDir, vec3 specularColorRGB, vec3 roughnessRGB)
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

    float roughness = 1.0 / sqrt(getLuminance(specularColorRGB / roughnessRGB * roughnessRGB));
    EnvironmentResult[ACTIVE_EIGENTEXTURE_COUNT] shading = computeSVDEnvironmentShading(-1, fPosition, normalDir, roughness);

    vec3 mfdGeomRoughnessSq = shading[0].baseFresnel.rgb;
    vec3 mfdGeomRoughnessSqFresnelFactor = shading[0].fresnelAdjustment.rgb;

    for (int k = 0; k < EIGENTEXTURE_COUNT; k++)
    {
        vec4 tex000 = getSignedTexel(ivec3(coords000, k), mipmapLevelFloor);
        vec4 tex001 = getSignedTexel(ivec3(coords001, k), mipmapLevelCeil);
        vec4 tex010 = getSignedTexel(ivec3(coords010, k), mipmapLevelFloor);
        vec4 tex011 = getSignedTexel(ivec3(coords011, k), mipmapLevelCeil);
        vec4 tex100 = getSignedTexel(ivec3(coords100, k), mipmapLevelFloor);
        vec4 tex101 = getSignedTexel(ivec3(coords101, k), mipmapLevelCeil);
        vec4 tex110 = getSignedTexel(ivec3(coords110, k), mipmapLevelFloor);
        vec4 tex111 = getSignedTexel(ivec3(coords111, k), mipmapLevelCeil);

        vec4 tex = mix(mix(mix(tex000, tex100, interpolantsFloorLevel.x),
                           mix(tex010, tex110, interpolantsFloorLevel.x),
                           interpolantsFloorLevel.y),
                       mix(mix(tex001, tex101, interpolantsCeilLevel.x),
                           mix(tex011, tex111, interpolantsCeilLevel.x),
                           interpolantsCeilLevel.y),
                       mipmapLevelInterpolant);

        vec4 blendedTerm = shading[k + 1].baseFresnel * tex;
        vec3 blendedTermFresnel = shading[k + 1].fresnelAdjustment.rgb * tex.rgb;

        if (blendedTerm.a > 0)
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
