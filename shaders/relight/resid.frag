#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 residual;

#include <shaders/colorappearance/colorappearance_multi_as_single.glsl>
#include <shaders/colorappearance/imgspace.glsl>
#include "resid.glsl"

#line 17 0

void main()
{
    residual = computeResidual();
}
