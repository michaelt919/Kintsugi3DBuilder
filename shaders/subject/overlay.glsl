#ifndef OVERLAY_GLSL
#define OVERLAY_GLSL

#include "../colorappearance/material.glsl"
#include "overlayModes.glsl"

#line 8 3103

#if OVERLAY_MODE == OVERLAY_MODE_WEIGHTMAP

#include "../specularfit/evaluateBRDF.glsl"
#line 13 3103

vec3 emissive(Material m)
{
    vec2 texCoords = getTexCoords();
    vec4 weightmapTex = texture(weightMaps, vec3(texCoords, OVERLAY_WEIGHTMAP_INDEX));
    return weightmapTex.r * vec3(1.0, 0.0, 1.0);
}

#else // default OVERLAY_MODE

vec3 emissive(Material m)
{
    return vec3(0.0, 0.0, 0.0);
}

#endif // OVERLAY_MODE

#endif // OVERLAY_GLSL
