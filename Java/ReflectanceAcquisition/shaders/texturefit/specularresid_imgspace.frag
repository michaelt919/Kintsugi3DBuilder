#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 specularResidual;
layout(location = 1) out vec4 halfAngleVector;

#include "../reflectance/reflectance_single.glsl"
#include "../reflectance/imgspace_single.glsl"
#include "specularresid.glsl"

#line 16 0

void main()
{
    SpecularResidualInfo spec = computeSpecularResidualInfo();
    specularResidual = spec.residual;
    halfAngleVector = vec4(spec.halfAngleVector, 1.0);
}
