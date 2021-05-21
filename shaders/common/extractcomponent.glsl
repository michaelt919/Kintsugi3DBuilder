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

#ifndef EXTRACT_COMPONENT_GLSL
#define EXTRACT_COMPONENT_GLSL

#line 5 9902

float extractComponentByIndex(vec4 packedVector, int componentIndex)
{
    if (componentIndex == 0)
    {
        return packedVector[0];
    }
    else if (componentIndex == 1)
    {
        return packedVector[1];
    }
    else if (componentIndex == 2)
    {
        return packedVector[2];
    }
    else
    {
        return packedVector[3];
    }
}

int extractComponentByIndex(ivec4 packedVector, int componentIndex)
{
    if (componentIndex == 0)
    {
        return packedVector[0];
    }
    else if (componentIndex == 1)
    {
        return packedVector[1];
    }
    else if (componentIndex == 2)
    {
        return packedVector[2];
    }
    else
    {
        return packedVector[3];
    }
}

uint extractComponentByIndex(uvec4 packedVector, int componentIndex)
{
    if (componentIndex == 0)
    {
        return packedVector[0];
    }
    else if (componentIndex == 1)
    {
        return packedVector[1];
    }
    else if (componentIndex == 2)
    {
        return packedVector[2];
    }
    else
    {
        return packedVector[3];
    }
}

#endif // EXTRACT_COMPONENT_GLSL
