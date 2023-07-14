/*
 *  Copyright (c) Michael Tetzlaff 2022
 *  Copyright (c) The Regents of the University of Minnesota 2019
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

uniform vec2 viewportSize;
uniform sampler2D inputImage;

uniform vec2 focalLength;
uniform vec2 opticalCenter;
uniform vec4 coefficientsK;
uniform vec2 coefficientsP;

void main() {
    fragColor = vec4(fTexCoord.xy, 0, 1);
}
