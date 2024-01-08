#version 330

/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

in vec3 fPosition;

#include "../common/usegeom.glsl"
#line 19 0

#define PI 3.1415926535897932384626433832795

uniform vec3 reconstructionLightPos;
uniform vec3 reconstructionLightIntensity;

layout(location = 0) out vec4 fragColor;

void main()
{
    vec3 lightDisplacement = reconstructionLightPos - getPosition();

    // View set's light intensity is technically radiance / pi, hence multiplication by pi
    // Gamma correction intentionally omitted for error calculation.
    fragColor = vec4(reconstructionLightIntensity * PI / dot(lightDisplacement, lightDisplacement), 1.0);
}
