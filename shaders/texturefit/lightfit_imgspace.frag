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

layout(location = 0) out vec4 lightPosition;
layout(location = 1) out vec4 lightIntensity;

#include "../colorappearance/imgspace.glsl"
#include "lightfit.glsl"

#line 14 0

void main()
{
    LightFit fit = fitLight();
    lightPosition = vec4(fit.position, fit.quality);
    lightIntensity = vec4(vec3(fit.intensity), fit.quality);
}
