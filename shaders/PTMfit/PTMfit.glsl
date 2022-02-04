in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

uniform sampler2D normalEstimate;
uniform sampler2D roughnessEstimate;

#ifndef MATERIAL_EXPLORATION_MODE
#define MATERIAL_EXPLORATION_MODE 0
#endif

#if MATERIAL_EXPLORATION_MODE
// For debugging or generating comparisons and figures.
#undef NORMAL_TEXTURE_ENABLED
#define NORMAL_TEXTURE_ENABLED 1
#include <shaders/colorappearance/textures.glsl>
#include <shaders/colorappearance/analytic.glsl>
#else
#include <shaders/colorappearance/imgspace.glsl>
#endif

#include <shaders/relight/reflectanceequations.glsl>
