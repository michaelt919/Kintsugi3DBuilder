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

layout(location = 0) out vec2 errorResultOut;
layout(location = 1) out float mask;

#include "../colorappearance/colorappearance.glsl"
#include "../colorappearance/imgspace.glsl"
#include "errorcalc.glsl"

#line 15 0

void main()
{
    ErrorResult errorResult = calculateError();
    if (errorResult.mask)
    {
        errorResultOut = vec2(errorResult.dampingFactor, errorResult.sumSqError);
        mask = 1;
    }
    else
    {
        errorResultOut = vec2(errorResult.dampingFactor, errorResult.sumSqError);
        mask = 0;
    }
}
