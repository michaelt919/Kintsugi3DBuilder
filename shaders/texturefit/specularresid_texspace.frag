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

layout(location = 0) out vec4 residualXYZ_nDotL;
layout(location = 1) out vec4 geomInfoPacked;

#include "../colorappearance/colorappearance_single.glsl"
#include "../colorappearance/texspace_single.glsl"
#include "specularresid.glsl"

#line 17 0

void main()
{
    SpecularResidualInfo spec = computeSpecularResidualInfo();
    residualXYZ_nDotL = vec4(spec.residualXYZ, spec.nDotL);
    geomInfoPacked = vec4(spec.halfAngleVector.xyz, spec.geomRatio);
}
