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
    vec2 C = opticalCenter;
    vec2 uv = (fTexCoord * viewportSize - C) / focalLength;
    float r = sqrt(dot(uv, uv));

    float K1 = coefficientsK[0];
    float K2 = coefficientsK[1];
    float K3 = coefficientsK[2];
    float K4 = coefficientsK[3];

    float P1 = coefficientsP[0];
    float P2 = coefficientsP[1];

    vec2 uvd = uv;
    // Radial distortion
    uvd.x = uv.x * (1 + K1 * pow(r, 2) + K2 * pow(r, 4) + K3 * pow(r, 6));
    uvd.y = uv.y * (1 + K1 * pow(r, 2) + K2 * pow(r, 4) + K3 * pow(r, 6));

    // Tangential distortion
    //uvd.x = uvd.x + (2 * P1 * uvd.x * uvd.y + P2 * (pow(r, 2) + 2 * pow(uvd.x, 2)));
    //uvd.y = uvd.y + (P1 * (pow(r, 2) + 2 * pow(uvd.y, 2)) + 2 * P2 * uvd.x * uvd.y);

    vec2 uvO = (uvd * focalLength + C) / viewportSize ;
    if (uvO.x > 1.0 || uvO.x < 0.0 || uvO.y > 1.0 || uvO.y < 0.0) {
        fragColor = vec4(0,0,0,1);
    } else {
        fragColor = texture(inputImage, uvO);
    }
}
