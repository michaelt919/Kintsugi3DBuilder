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
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 shadingInfo;

#include "colorappearance_multi_as_single.glsl"
#include "imgspace.glsl"

#line 28 1011

uniform bool lightIntensityCompensation;

void main()
{
    if (lightIntensityCompensation)
    {
        LightInfo lightInfo = getLightInfo();
        vec4 linearColor = getLinearColor();
        fragColor = vec4(linearToSRGB(linearColor.rgb / lightInfo.attenuatedIntensity), linearColor.a);
    }
    else
    {
        fragColor = getColor();
    }
}
