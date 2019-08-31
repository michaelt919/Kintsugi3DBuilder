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

#ifndef USE_DEFERRED_GLSL
#define USE_DEFERRED_GLSL

#line 17 9901

uniform sampler2D positionMap;
uniform sampler2D normalMap;

vec3 getPosition(vec2 texCoord)
{
    return texture(positionMap, texCoord).xyz;
}

vec3 getNormal(vec2 texCoord)
{
    vec3 normal = texture(normalMap, texCoord).xyz;

    if (normal == vec3(0))
    {
        return vec3(0);
    }
    else
    {
        return normalize(normal);
    }
}

#endif // USE_DEFERRED_GLSL
