#version 330

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

#include <shaders/colorappearance/linearize.glsl>
#line 17 0

in vec2 fTexCoord;

uniform sampler2D diffuseEstimate;
uniform sampler2D roughnessEstimate;
uniform sampler2D specularEstimate;

layout(location = 0) out vec4 totalAlbedoOut;
layout(location = 1) out vec4 ormOut;

void main()
{
    float sqrtRoughness = texture(roughnessEstimate, fTexCoord)[0];

    vec3 diffuseLinear = pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));
    vec3 diffusePlusSpecular = diffuseLinear + pow(texture(specularEstimate, fTexCoord).rgb, vec3(gamma));

//    d = (1 - m) * a
//    d / a = 1 - m
//    m = 1 - d/a
//
//    s = 0.04 * (1 - m) + m * a
//    s = 0.04 d/a + (1 - d/a) * a
//    s = 0.04 d/a + a - d
//    s*a = 0.04d + a^2 - d*a
//    a^2 - (d + s) * a + 0.04*d = 0
//
//    a = 1/2 * (d + s + sqrt((d + s)^2 - 4 * 0.04*d))
//      = 0.5 * (d + s) + 0.5 * sqrt((d + s)^2 - 0.16*d)

    // Adjust for dielectic specular (i.e. 0.04 for non-metallic)
    // See derivation above
    // Initially, calculate albedo assuming metallicity could be different for each RGB channel (for simplicity)
    vec3 albedoLinear = 0.5 * diffusePlusSpecular + 0.5 * sqrt(max(vec3(0.0), diffusePlusSpecular * diffusePlusSpecular - 0.16 * diffuseLinear));

    // Then calculate metallicity for all channels
    vec3 metallicity = clamp(1.0 - diffuseLinear / albedoLinear, 0.0, 1.0);

    // Take the highest metallicity of all the color channels (most conservative in terms of preserving color information in specular highlights)
    float maxMetallicity = max(metallicity.r, max(metallicity.g, metallicity.b));

    // Recalculate albedo now assuming a single metallicity for the R, G, and B channels
    albedoLinear = diffusePlusSpecular - 0.04 * (1 - maxMetallicity);

    totalAlbedoOut = vec4(pow(albedoLinear, vec3(1.0 / gamma)), 1.0);
    ormOut = vec4(/* occlusion: */ 1.0, sqrtRoughness, maxMetallicity, 1.0);
}
