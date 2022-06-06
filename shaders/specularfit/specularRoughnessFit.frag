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

in vec2 fTexCoord;
layout(location = 0) out vec4 specularColor;
layout(location = 1) out vec4 sqrtRoughness;

uniform sampler2DArray weightMaps;
uniform sampler2D weightMask;
uniform sampler1DArray basisFunctions;
uniform float fittingGamma;

#ifndef PI
#define PI 3.1415926535897932384626433832795
#endif

#ifndef BASIS_COUNT
#define BASIS_COUNT 8
#endif

#ifndef MICROFACET_DISTRIBUTION_RESOLUTION
#define MICROFACET_DISTRIBUTION_RESOLUTION 90
#endif

#include "../colorappearance/linearize.glsl"
#line 37 0

void main()
{
    if (texture(weightMask, fTexCoord)[0] < 1.0)
    {
        discard;
    }

    vec3 f0 = vec3(0);

    float weights[BASIS_COUNT];

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        weights[b] = texture(weightMaps, vec3(fTexCoord, b))[0];
        f0 += weights[b] * texelFetch(basisFunctions, ivec2(0, b), 0).rgb;
    }

    vec3 sqrtF0 = sqrt(f0);

    vec3 sumNumerator = vec3(0);
    vec3 sumDenominator = vec3(0);

    for (int m = 1; m < MICROFACET_DISTRIBUTION_RESOLUTION; m++)
    {
        vec3 f = vec3(0);
        for (int b = 0; b < BASIS_COUNT; b++)
        {
            f += weights[b] * texelFetch(basisFunctions, ivec2(m, b), 0).rgb;
        }

        float sqrtAngle = float(m) / float(MICROFACET_DISTRIBUTION_RESOLUTION);
        float nDotH = cos(sqrtAngle * sqrtAngle * PI / 3.0);
        float nDotHSq = nDotH * nDotH;

        vec3 numerator = (1.0 - nDotHSq) * sqrt(f);
        vec3 denominator = sqrt(f0) - nDotHSq * sqrt(f);

        sumNumerator += pow(numerator * denominator, vec3(1.0 / fittingGamma));
        sumDenominator += pow(denominator * denominator, vec3(1.0 / fittingGamma));
    }

    vec3 fresnel = PI * f0 * pow(sumNumerator / sumDenominator, vec3(fittingGamma));
    float roughnessSq = getLuminance(fresnel) / getLuminance(PI * f0);
    float roughness = sqrt(roughnessSq);

    specularColor = vec4(pow(fresnel, vec3(1.0 / gamma)), 1.0);
    sqrtRoughness = vec4(vec3(sqrt(roughness)), 1.0);
}
