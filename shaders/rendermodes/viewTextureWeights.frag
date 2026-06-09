#version 330

uniform sampler2DArray weightMaps;

#ifndef WEIGHTMAP_INDEX
#define WEIGHTMAP_INDEX 0
#endif

#define SAMPLE_VIEW_TEX(VIEW_TEX_UV) ( texture(weightMaps, vec3(VIEW_TEX_UV, WEIGHTMAP_INDEX) ) )

#include "viewTextureCommon.glsl"
