#version 330

/*
 *  Copyright (c) Michael Tetzlaff 2023
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#include <shaders/colorappearance/colorappearance_multi_as_single.glsl>
#line 17 0

layout(location = 0) out vec4 incidentRadiance;

void main()
{
    vec3 lightDisplacement = getLightVector();

    // "Light intensity" is defined in such a way that we need to multiply by pi to be properly normalized.
    incidentRadiance = vec4(vec3(PI * lightIntensity / dot(lightDisplacement, lightDisplacement)), 1.0);
}
