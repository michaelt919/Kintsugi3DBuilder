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

in vec2 fTexCoord;

layout(location = 0) out vec4 output0;
layout(location = 1) out vec4 output1;
layout(location = 2) out vec4 output2;
layout(location = 3) out vec4 output3;

uniform sampler2D input0;
uniform sampler2D input1;
uniform sampler2D input2;
uniform sampler2D input3;
uniform sampler2D alphaMask;

void main()
{
    if (texture(alphaMask, fTexCoord).x < 1.0)
    {
        discard;
    }
    else
    {
        output0 = texture(input0, fTexCoord);
        output1 = texture(input1, fTexCoord);
        output2 = texture(input2, fTexCoord);
        output3 = texture(input3, fTexCoord);
    }
}