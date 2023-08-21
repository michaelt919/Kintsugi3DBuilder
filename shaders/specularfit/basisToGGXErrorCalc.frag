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

#include "basisToGGXError.glsl"

#ifndef PI
#define PI 3.1415926535897932384626433832795
#endif

in vec2 fTexCoord;

layout(location = 0) out vec4 errorOut;

uniform float gamma;
uniform float gammaInv;

uniform sampler2D diffuseMap;
uniform sampler2D specularEstimate;
uniform sampler2D roughnessMap;

void main()
{
    vec3 reflectivity = pow(texture(specularEstimate, fTexCoord).rgb, vec3(gamma));
    float sqrtRoughness = texture(roughnessMap, fTexCoord)[0];
    float roughness = sqrtRoughness * sqrtRoughness;
    vec3 diffuse = pow(texture(diffuseColor, fTexCoord).rgb, vec3(gamma));

    errorOut = vec4(vec3(calculateError(diffuse, reflectivity, roughness)),
        BASIS_RESOLUTION - 1);
}
