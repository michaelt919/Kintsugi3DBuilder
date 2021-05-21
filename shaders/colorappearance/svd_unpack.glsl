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

#ifndef SVD_UNPACK_GLSL
#define SVD_UNPACK_GLSL

#include "colorappearance.glsl"

#line 7 1102

uniform sampler2DArray eigentextures;
uniform sampler2DArray viewWeightTextures;
uniform ivec2 blockSize;
uniform ivec2 viewWeightPacking;

vec4 getColor(int index)
{
    ivec3 eigentexturesSize = textureSize(eigentextures);
    ivec3 blockStart = ivec(min(floor(fTexCoord * eigentexturesSize.xy), eigentexturesSize.xy - ivec2(1))
                            / blockSize * viewWeightPacking, index);

    vec3 color = vec3(0.0);

    for (int k = 0; k < eigentexturesSize[2]; k++)
    {
        color += texelFetch(viewWeightTextures, blockStart + ivec3(k % viewWeightPacking[0], k / viewWeightPacking[0], 0))
                    * texture(eigentextures, vec3(transformedTexCoord, k));
    }

    return vec4(color, 1.0);
}

#endif // SVD_UNPACK_GLSL