/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#version 330

in vec2 fTexCoord;
layout(location = 0) out vec4 fragColor;

uniform sampler2DArray weightMaps;
uniform int weightIndex;
uniform int weightStride;

void main() {
    vec4 color = vec4(0, 0, 0, 1);
    for (int i = 0; i < weightStride; i++)
    {
        color[i] = texture(weightMaps, vec3(fTexCoord.xy, weightIndex)).r;
    }
    //color = vec4(vec3(fTexCoord.xy, weightIndex), 1.0);
    fragColor = color;
}
