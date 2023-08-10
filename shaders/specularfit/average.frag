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

#include "specularFit.glsl"
#line 18 0

layout(location = 0) out vec4 averageLab;

void main()
{
    vec3 position = getPosition();
    vec3 normal = getNormal();

    vec4 sum = vec4(0);

    for (int k = 0; k < CAMERA_POSE_COUNT; k++)
    {
        vec4 imgColor = getLinearColor(k);

        if (imgColor.a > 0 && dot(normal, getViewVector(k, position)) > 0)
        {
            vec3 lightDisplacement = getLightVector(k, position);

            // Cancel out incident radiance, but don't divide out the cosine factor (so this will be essentially cosine-weighted reflectance).
            vec3 rgb = imgColor.rgb * dot(lightDisplacement, lightDisplacement) / getLightIntensity(k).rgb;

            // Only a simple RGB to L*a*b* is necessary, since this is just for the purposes of initial clustering.
            sum += vec4(xyzToLab(rgbToXYZ(rgb)), 1.0);
        }
    }

    if (sum.a == 0)
    {
        discard;
    }

    // Only a simple RGB to L*a*b* is necessary, since this is just for the purposes of initial clustering.
    averageLab = sum / sum.a;
}
