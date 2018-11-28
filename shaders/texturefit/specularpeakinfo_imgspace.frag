#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 peak;
layout(location = 1) out vec4 offPeakSum;
layout(location = 2) out vec4 position;
layout(location = 3) out vec4 threshold;
layout(location = 4) out vec2 texCoord;

#ifndef SINGLE_VIEW_MASKING_ENABLED
#define SINGLE_VIEW_MASKING_ENABLED 0
#endif

#if SINGLE_VIEW_MASKING_ENABLED
#include "../colorappearance/colorappearance_multi_as_single.glsl"
#else
#include "../colorappearance/colorappearance.glsl"
#endif

#include "../colorappearance/imgspace.glsl"
#include "specularpeakinfo.glsl"

#line 23 0

void main()
{
    SpecularPeakInfo result = computeSpecularPeakInfo();

    peak = result.peak;
    offPeakSum = result.offPeakSum;
    position = vec4(fPosition, result.maxNDotH);
    threshold = vec4(result.threshold, result.peakNDotV);
    texCoord = fTexCoord;
}
