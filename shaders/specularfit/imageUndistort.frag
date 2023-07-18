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
uniform float skew;

void main() {
    vec2 uv = (fTexCoord * viewportSize - opticalCenter) / focalLength;

    float r2 = dot(uv, uv);
    float r4 = pow(r2, 2);
    float r6 = pow(r4, 2);
    float r8 = pow(r6, 2);

    vec2 uvd = uv;
    // Radial distortion
    // distort = 1 + K1*r^2 + K2*r^4 + K3*r^6 + K4*r^8
    float distort = (1 + coefficientsK[0] * r2 + coefficientsK[1] * r4 + coefficientsK[2] * r6 + coefficientsK[2] * r8);
    uvd.x = uv.x * distort;
    uvd.y = uv.y * distort;

    // Tangential distortion
    float xy = uvd.x * uvd.y;
    vec2 uvd22pr2 = r2 + (uvd * uvd) * 2;
    uvd.x = uvd.x + (2 * coefficientsP[0] * xy + coefficientsP[1] * uvd22pr2.x);
    uvd.y = uvd.y + (coefficientsP[0] * uvd22pr2.y + 2 * coefficientsP[1] * xy);


    vec2 uvO = (uvd * focalLength + opticalCenter + vec2(uvd.y * skew, 0)) / viewportSize;
    if (uvO.x > 1.0 || uvO.x < 0.0 || uvO.y > 1.0 || uvO.y < 0.0) {
        fragColor = vec4(0,0,0,1);
    } else {
        fragColor = texture(inputImage, uvO);
    }
}
