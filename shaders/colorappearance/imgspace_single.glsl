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

#ifndef IMGSPACE_SINGLE_GLSL
#define IMGSPACE_SINGLE_GLSL

#include "colorappearance_single.glsl"

#line 7 1111

uniform sampler2D viewImage;
uniform sampler2D depthImage;
uniform sampler2D shadowImage;

uniform bool occlusionEnabled;
uniform bool shadowTestEnabled;
uniform float occlusionBias;

uniform mat4 cameraProjection;
uniform mat4 shadowMatrix;

vec4 getColor()
{
    vec4 projTexCoord = cameraProjection * cameraPose * vec4(fPosition, 1.0);
    projTexCoord /= projTexCoord.w;
    projTexCoord = (projTexCoord + vec4(1)) / 2;

    if (projTexCoord.x < 0 || projTexCoord.x > 1 || projTexCoord.y < 0 || projTexCoord.y > 1)
    {
        return vec4(0);
    }
    else
    {
        if (projTexCoord.z >= 0 && projTexCoord.z <= 1)
        {
            if (occlusionEnabled)
            {
                float imageDepth = texture(depthImage, projTexCoord.xy).r;
                if (abs(projTexCoord.z - imageDepth) > occlusionBias)
                {
                    // Occluded
                    return vec4(0);
                }
            }

            if (shadowTestEnabled)
            {
                vec4 shadowTexCoord = shadowMatrix * vec4(fPosition, 1.0);
                shadowTexCoord /= shadowTexCoord.w;
                shadowTexCoord = (shadowTexCoord + vec4(1)) / 2;

                if (shadowTexCoord.x >= 0 && shadowTexCoord.x <= 1 &&
                     shadowTexCoord.y >= 0 && shadowTexCoord.y <= 1 &&
                     shadowTexCoord.z >= 0 && shadowTexCoord.z <= 1)
                {
                    float shadowImageDepth = texture(shadowImage, shadowTexCoord.xy).r;
                    if (abs(shadowTexCoord.z - shadowImageDepth) > occlusionBias)
                    {
                        // Occluded
                        return vec4(0);
                    }
                }
            }
        }
        
        return texture(viewImage, projTexCoord.xy);
    }
}

#endif // IMGSPACE_SINGLE_GLSL
