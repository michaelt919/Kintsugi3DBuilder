#version 330

#ifdef VIEW_TEX

uniform sampler2D VIEW_TEX;
#define SAMPLE_VIEW_TEX(VIEW_TEX_UV) ( texture(VIEW_TEX, VIEW_TEX_UV) )

#else // VIEW_TEX undefined -- show UV coords

#define SAMPLE_VIEW_TEX(VIEW_TEX_UV) ( vec4(VIEW_TEX_UV, 0.0, 1.0) )

#endif

#include "viewTextureCommon.glsl"
