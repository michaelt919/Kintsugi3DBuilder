#version 330

/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

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

#line 41 0

void main()
{
    SpecularPeakInfo result = computeSpecularPeakInfo();

    peak = result.peak;
    offPeakSum = result.offPeakSum;
    position = vec4(fPosition, result.maxNDotH);
    threshold = vec4(result.threshold, result.peakNDotV);
    texCoord = fTexCoord;
}
