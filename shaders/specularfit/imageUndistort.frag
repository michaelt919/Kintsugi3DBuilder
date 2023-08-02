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

#version 330

in vec2 fTexCoord;
layout(location = 0) out vec4 fragColor;

uniform vec2 viewportSize;
uniform sampler2D inputImage;

uniform vec2 focalLength;
uniform vec2 opticalCenter;
uniform vec4 coefficientsK;
uniform vec2 coefficientsP;
uniform float skew;

void main()
{
    // Flip y-component vertically before applying undistortion equations
    vec2 uv = (vec2(fTexCoord.x, 1 - fTexCoord.y) * viewportSize - opticalCenter) / focalLength;

    float r2 = dot(uv, uv);
    float r4 = r2 * r2;
    float r6 = r4 * r2;
    float r8 = r4 * r4;

    vec2 uvd = uv;
    // Radial distortion
    // distort = 1 + K1*r^2 + K2*r^4 + K3*r^6 + K4*r^8
    float distort = (1 + coefficientsK[0] * r2 + coefficientsK[1] * r4 + coefficientsK[2] * r6 + coefficientsK[2] * r8);
    uvd.x = uv.x * distort;
    uvd.y = uv.y * distort;

    // Tangential distortion
    float xy = uv.x * uv.y;
    vec2 rSqPlus2uvSq = r2 + (uv * uv) * 2;
    uvd.x = uvd.x + (coefficientsP[0] * rSqPlus2uvSq.x + 2 * coefficientsP[1] * xy);
    uvd.y = uvd.y + (coefficientsP[1] * rSqPlus2uvSq.y + 2 * coefficientsP[0] * xy);

    vec2 uvOut = (uvd * focalLength + opticalCenter + vec2(uvd.y * skew, 0)) / viewportSize;
    if (uvOut.x > 1.0 || uvOut.x < 0.0 || uvOut.y > 1.0 || uvOut.y < 0.0)
    {
        fragColor = vec4(0,0,0,1);
    }
    else
    {
        // Flip y-component back before performing texture lookup
        fragColor = texture(inputImage, vec2(uvOut.x, 1 - uvOut.y));
    }
}
