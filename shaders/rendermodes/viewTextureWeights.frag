#version 330

#ifndef WEIGHTMAP_INDEX
#define WEIGHTMAP_INDEX 0
#endif

#define SAMPLE_VIEW_TEX(VIEW_TEX_UV) ( vec4(texture(weightMaps, vec3(VIEW_TEX_UV, WEIGHTMAP_INDEX) ).rrr, 1.0) )

#include "viewTextureCommon.glsl"
