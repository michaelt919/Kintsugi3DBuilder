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

in vec2 fTexCoord;

layout(location = 0) out vec4 residual;

uniform vec2 minTexCoord;
uniform vec2 maxTexCoord;

#include "../common/use_deferred.glsl"

#define fPosition ( getPosition(fTexCoord) )

#include "../colorappearance/colorappearance_multi_as_single.glsl"
#include "../colorappearance/imgspace.glsl"
#include "resid.glsl"

#line 31 0

void main()
{
    vec3 normal = getNormal(fTexCoord);
    if (normal == vec3(0))
    {
        discard;
    }
    else
    {
        residual = computeResidual(mix(minTexCoord, maxTexCoord, fTexCoord), normal);
    }
}
