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

void main()
{
    float sqrtRoughness = texture(roughnessMap, fTexCoord)[0];
    float roughness = sqrtRoughness * sqrtRoughness;
    float geomRatio;

    LightingParameters l = calculateLightingParameters(reconstructionCameraPos, reconstructionLightPos);
    if (l.nDotL > 0.0 && l.nDotV > 0.0)
    {
        float maskingShadowing = geom(roughness, l.nDotH, l.nDotV, l.nDotL, l.hDotV);
        geomRatio = maskingShadowing / (4 * l.nDotL * l.nDotV);
    }
    else if (l.nDotL > 0.0)
    {
        geomRatio = 0.5 / (roughness * l.nDotL); // Limit as n dot v goes to zero.
    }

    // Constant term for pseudo-translucency
    // Division by PI since it's fit on the same scale as diffuse
    vec3 constant = pow(getConstantTerm(), vec3(gamma)) / PI;

    if (l.nDotL > 0.0)
    {
        vec3 brdf = pow(texture(diffuseMap, fTexCoord).rgb, vec3(gamma)) / PI + geomRatio * getMFDEstimate(l.nDotH);

        // Gamma correction intentionally omitted for error calculation.
        fragColor = vec4(l.nDotL * brdf + constant, 1.0);
    }
    else
    {
        // Limit as n dot l and n dot v both go to zero.
        vec3 mfd = getMFDEstimate(l.nDotH);

        // Gamma correction intentionally omitted for error calculation.
        fragColor = vec4(mfd * 0.5 / roughness + constant, 1.0);
    }
}
