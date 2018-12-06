/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#version 330

uniform sampler2D input0;
uniform sampler2D input1;
uniform sampler2D input2;
uniform sampler2D input3;
uniform sampler2D input4;
uniform sampler2D input5;
uniform sampler2D input6;
uniform sampler2D input7;

uniform float minFillAlpha;

in vec2 fTexCoord;

layout(location=0) out vec4 output0;
layout(location=1) out vec4 output1;
layout(location=2) out vec4 output2;
layout(location=3) out vec4 output3;
layout(location=4) out vec4 output4;
layout(location=5) out vec4 output5;
layout(location=6) out vec4 output6;
layout(location=7) out vec4 output7;

// Copies the color values of adjacent pixels into the current pixel if the current pixel has an alpha value less than 1.0.
// inputTexture is a texture sampler that contains the reference pixels.
// The function returns the new pixel after potentially filling it in in this way.
vec4 fill(sampler2D inputTexture)
{
    vec4 central = texture(inputTexture, fTexCoord);

    if (central.a >= 1.0)
    {
        // Return the inputTexture pixel if it already has an alpha of 1.0
        return central;
    }
    else
    {
        // Sample the neighboring pixels
        vec4 sum = vec4(0.0);
        vec4 north = textureOffset(inputTexture, fTexCoord, ivec2(0, 1));
        if (north.a >= minFillAlpha)
        {
            sum += north.a * vec4(north.rgb, 1.0);
        }

        vec4 south = textureOffset(inputTexture, fTexCoord, ivec2(0, -1));
        if (south.a >= minFillAlpha)
        {
            sum += south.a * vec4(south.rgb, 1.0);
        }

        vec4 east = textureOffset(inputTexture, fTexCoord, ivec2(1, 0));
        if (east.a >= minFillAlpha)
        {
            sum += east.a * vec4(east.rgb, 1.0);
        }

        vec4 west = textureOffset(inputTexture, fTexCoord, ivec2(-1, 0));
        if (west.a >= minFillAlpha)
        {
            sum += west.a * vec4(west.rgb, 1.0);
        }

        if (sum.a >= (1.0 - central.a))
        {
            return vec4(central.a * central.rgb + (1.0 - central.a) * sum.rgb / sum.a, 1.0);
        }
        else if (central.a + sum.a > 0)
        {
            return vec4((central.a * central.rgb + sum.rgb) / (central.a + sum.a), central.a + sum.a);
        }
        else
        {
            return vec4(0);
        }
    }
}

void main()
{
    output0 = fill(input0);
    output1 = fill(input1);
    output2 = fill(input2);
    output3 = fill(input3);
    output4 = fill(input4);
    output5 = fill(input5);
    output6 = fill(input6);
    output7 = fill(input7);
}
