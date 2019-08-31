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

layout(location = 0) out vec2 errorResultOut;
layout(location = 1) out float mask;

uniform sampler2D oldErrorTexture;
uniform sampler2D currentErrorTexture;

void main()
{
    vec4 currentError = texture(currentErrorTexture, fTexCoord);
    vec4 oldError = texture(oldErrorTexture, fTexCoord);

    if ((currentError.w == 1.0 || currentError.w > oldError.w) && oldError.y > currentError.y)
    {
        errorResultOut = vec2(0.0, currentError);
        mask = 1;
    }
    else
    {
        errorResultOut = vec2(0.0, oldError);
        mask = 0;
    }
}
