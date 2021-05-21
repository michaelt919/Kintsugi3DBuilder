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

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 shadingInfo;

#include "colorappearance_multi_as_single.glsl"
#include "imgspace.glsl"

#line 16 1011

uniform bool lightIntensityCompensation;

void main()
{
    vec3 view = normalize(getViewVector());
    vec3 lightPreNormalized = getLightVector();
    vec3 attenuatedLightIntensity = // infiniteLightSources ? lightIntensity :
        lightIntensity / (dot(lightPreNormalized, lightPreNormalized));
    vec3 light = normalize(lightPreNormalized);
    vec3 halfway = normalize(light + view);
    vec3 normal = normalize(fNormal);
    shadingInfo = vec4(dot(normal, light), dot(normal, view), dot(normal, halfway), dot(halfway, view));

    if (lightIntensityCompensation)
    {
        fragColor = vec4(pow(getLinearColor().rgb / attenuatedLightIntensity, vec3(1.0 / gamma)), 1.0);
    }
    else
    {
        fragColor = getColor();
    }
}
