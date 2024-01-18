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

#include "../specularFit.glsl"
#line 17 0

uniform vec3 reconstructionCameraPos;
uniform vec3 reconstructionLightPos;
// gamma defined in colorappearance.glsl

layout(location = 0) out vec4 fragColor;

uniform sampler2D specularEstimate;

void main()
{
    vec2 sqrtRoughness_Mask = texture(roughnessMap, fTexCoord).ra;
    float filteredMask = sqrtRoughness_Mask[1];

    if (filteredMask == 0.0)
    {
        fragColor = vec4(0, 0, 0, 1);
        return;
    }

    float roughness = sqrtRoughness_Mask[0] * sqrtRoughness_Mask[0] / (filteredMask * filteredMask);
    vec3 diffuseColor = pow(texture(diffuseMap, fTexCoord).rgb / filteredMask, vec3(gamma));
    vec3 specularColor = pow(texture(specularEstimate, fTexCoord).rgb / filteredMask, vec3(gamma));

    // Constant term for pseudo-translucency
    vec3 constant = pow(getConstantTerm(), vec3(gamma)) / PI;

    LightingParameters l = calculateLightingParameters(reconstructionCameraPos, reconstructionLightPos);
    vec3 specular = distTimesPi(l.nDotH, vec3(roughness))
        * geom(roughness, l.nDotH, l.nDotV, l.nDotL, l.hDotV)
        * fresnel(specularColor.rgb, vec3(1), l.hDotV) / (4 * l.nDotV * PI);

    // Reflectance is implicitly multiplied by n dot l.
    // Divide constant term by PI since it's fit on the same scale as diffuse
    // Gamma correction intentionally omitted for error calculation.
    fragColor = vec4((diffuseColor * l.nDotL + constant) / PI + specular, 1.0);
}
