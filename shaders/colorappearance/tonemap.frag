#version 330

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

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

uniform mat4 model_view;
uniform mat4 projection;
uniform sampler2D image;

layout(location = 0) out vec4 fragColor;

#include "linearize.glsl"

#line 30 0

uniform vec3 reconstructionLightPos;
uniform vec3 reconstructionLightIntensity;

void main()
{
    // Calculate NDC
    vec4 projTexCoord = projection * model_view * vec4(fPosition, 1.0);
    projTexCoord /= projTexCoord.w;
    projTexCoord = projTexCoord * 0.5 + vec4(0.5);

    vec3 lightDisplacement = reconstructionLightPos - fPosition;

    // Technically, radiance / pi
    vec3 incidentRadiance = reconstructionLightIntensity / dot(lightDisplacement, lightDisplacement);

    // 1. Lookup texture
    // 2. Convert to linear space
    // 3. Convert to cosine-weighted reflectance (divide by incident radiance) -- should more or less preserve average pixel intensity
    //    This basically just eliminates the effect of light attenuation to better normalize RMSE calculations.
    // 4. Divide by decoded reflectance value (technically, reflectance * pi) for the white point to remap pixel values to the range [0, 1]
    // 5. Convert to pseudo-sRGB (gamma corrected).
    fragColor = vec4(pow(linearizeColor(texture(image, projTexCoord.xy).rgb) / (incidentRadiance * linearizeColor(vec3(1))), vec3(1.0 / gamma)), 1.0);
}
