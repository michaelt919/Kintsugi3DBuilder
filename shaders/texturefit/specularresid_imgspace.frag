#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 residualXYZ_nDotL;
layout(location = 1) out vec4 geomInfoPacked;

#include "../colorappearance/colorappearance_single.glsl"
#include "../colorappearance/imgspace_single.glsl"
#include "specularresid.glsl"

#line 17 0

void main()
{
    SpecularResidualInfo spec = computeSpecularResidualInfo();
    residualXYZ_nDotL = vec4(spec.residualXYZ, spec.nDotL);
    geomInfoPacked = vec4(spec.halfAngleVector.xyz, spec.geomRatio);
}
