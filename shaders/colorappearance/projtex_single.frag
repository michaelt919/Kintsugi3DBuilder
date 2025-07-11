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
layout(location = 2) out vec4 projTexCoord;

#include "colorappearance_single.glsl"
#include "imgspace_single.glsl"

#line 29 1010

uniform bool lightIntensityCompensation;

void main()
{
    projTexCoord = cameraProjection * cameraPose * vec4(fPosition, 1.0);
    projTexCoord /= projTexCoord.w;
    projTexCoord = (projTexCoord + vec4(1)) / 2;

    if (projTexCoord.x < 0 || projTexCoord.x > 1 || projTexCoord.y < 0 || projTexCoord.y > 1
        || projTexCoord.z < 0 || projTexCoord.z > 1)
    {
        discard;
    }
    else
    {

#if VISIBILITY_TEST_ENABLED
        float imageDepth = texture(depthImage, projTexCoord.xy).r;
        if (abs(projTexCoord.z - imageDepth) > occlusionBias)
        {
            // Occluded
            discard;
        }
#endif

        vec3 view = normalize(getViewVector());
        LightInfo lightInfo = getLightInfo();
        vec3 light = lightInfo.normalizedDirection;
        vec3 halfway = normalize(light + view);
        vec3 normal = normalize(fNormal);
        shadingInfo = vec4(dot(normal, light), dot(normal, view), dot(normal, halfway), dot(halfway, view));

        if (lightIntensityCompensation)
        {
            fragColor = vec4(linearToSRGB(getLinearColor().rgb / lightInfo.attenuatedIntensity), 1.0);
        }
        else
        {
            fragColor = vec4(texture(viewImage, projTexCoord.xy).rgb, 1.0);
        }
    }
}
