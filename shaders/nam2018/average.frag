#version 330

/*
 *  Copyright (c) Michael Tetzlaff 2020
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#include "nam2018.glsl"
#line 17 0

layout(location = 0) out vec4 averageLab;

//struct IndexedValue
//{
//    int index;
//    float value;
//};

void main()
{
//    IndexedValue luminances[CAMERA_POSE_COUNT];
//    int visibleCount = 0;
//
//    for (int k = 0; k < CAMERA_POSE_COUNT; k++)
//    {
//        vec4 imgColor = getLinearColor(k);
//        vec3 lightDisplacement = getLightVector(k);
//
//        if (imgColor.a > 0 && dot(fNormal, getViewVector(k)) > 0)
//        {
//            // For now, just getting Y (in CIE-XYZ) is sufficient since it should have the same ordering as L* in L*a*b*.
//            luminances[visibleCount] = IndexedValue(k, getLuminance(imgColor.rgb * dot(lightDisplacement, lightDisplacement) / getLightIntensity(k).rgb));
//            visibleCount++;
//        }
//    }
//
//    if (visibleCount == 0)
//    {
//        discard;
//    }
//
//    int minIndex;
//
//    // Selection sort, but only loop until the middle of the list since we just need the median.
//    for (int i = 0; i <= visibleCount / 2; i++)
//    {
//        // The first i elements are sorted (and are smaller than the median).
//        // Search for the next smallest.
//        minIndex = i;
//        for (int j = i + 1; j < visibleCount; j++)
//        {
//            if (luminances[minIndex].value > luminances[j].value)
//            {
//                minIndex = j;
//            }
//        }
//
//        // Swap between index i (the beginning of the range still being considered) and minIndex where the minimum remaining was found.
//        IndexedValue tmp = luminances[minIndex];
//        luminances[minIndex] = luminances[i];
//        luminances[i] = tmp;
//    }
//
//    // All the values up to and including visibleCount/2 should be sorted now, so we retrieve the median.
//    IndexedValue median = luminances[visibleCount / 2];
//
//    vec4 imgColor = getLinearColor(median.index);
//    vec3 lightDisplacement = getLightVector(median.index);
//    vec3 rgb = imgColor.rgb * dot(lightDisplacement, lightDisplacement) / getLightIntensity(median.index).rgb;
//
//    // Only a simple RGB to L*a*b* is necessary, since this is just for the purposes of initial clustering.
//    averageLab = vec4(xyzToLab(rgbToXYZ(rgb)), 1.0);



    vec4 sum = vec4(0);

    for (int k = 0; k < CAMERA_POSE_COUNT; k++)
    {
        vec4 imgColor = getLinearColor(k);

        if (imgColor.a > 0 && dot(fNormal, getViewVector(k)) > 0)
        {
            vec3 lightDisplacement = getLightVector(k);

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
