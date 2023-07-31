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

#include "evaluateBRDF.glsl"

#line 16 4011

float calculateError(vec3 diffuse, vec3 reflectivity, float roughness)
{
    float sqError = 0.0;

    float roughnessSquared = roughness * roughness;

    for (int m = 1; m < MICROFACET_DISTRIBUTION_RESOLUTION; m++)
    {
        float sqrtAngle = float(m) / float(MICROFACET_DISTRIBUTION_RESOLUTION);
        float nDotH = cos(sqrtAngle * sqrtAngle * PI / 3.0);
        float nDotHSq = nDotH * nDotH;

        // Calculate from parameter fit
        float sqrtDenominator = (roughnessSquared - 1) * nDotHSq + 1;
        float denominator = sqrtDenominator * sqrtDenominator;
        float mfdTimesPi = roughnessSquared / denominator; // pre-multiplied by pi
        vec3 brdfTimes4Pi = 4 * diffuse + mfdTimesPi * reflectivity; // ignore masking / shadowing

        // Calculate from basis functions
        vec3 target = vec3(0);
        for (int b = 0; b < BASIS_COUNT; b++)
        {
            target += texture(weightMaps, vec3(fTexCoord, b))[0] * texelFetch(basisFunctions, ivec2(m, b), 0).rgb;
        }

        float weight = nDotH * nDotH; // cosine weighting

        vec3 diff = pow(4 * diffuse + PI * target, vec3(gammaInv)) - pow(brdfTimes4Pi, vec3(gammaInv));

        sqError += weight * dot(diff, diff);
    }

    return sqError;
}
