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

in vec2 fTexCoord;
layout(location = 0) out vec4 specularColor;
layout(location = 1) out vec4 sqrtRoughness;

uniform sampler2D weightMask;
//uniform float fittingGamma;

#ifndef PI
#define PI 3.1415926535897932384626433832795
#endif

#include "../colorappearance/linearize.glsl"
#include "evaluateBRDF.glsl"
#line 29 0

struct MFDEval
{
    vec3 mfd;
    float thetaH;
    float nDotH;
};

MFDEval evalMFD(int m, float weights[BASIS_COUNT])
{
    MFDEval result;

    float sqrtAngle = float(m) / float(BASIS_RESOLUTION);
    result.thetaH = sqrtAngle * sqrtAngle * PI / 3.0;
    result.nDotH = cos(result.thetaH);

    result.mfd = vec3(0.0);
    for (int b = 0; b < BASIS_COUNT; b++)
    {
        result.mfd += weights[b] * texelFetch(basisFunctions, ivec2(m, b), 0).rgb;
    }

    return result;
}

void main()
{
    if (texture(weightMask, fTexCoord)[0] < 1.0)
    {
        discard;
    }

    float weights[BASIS_COUNT];

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        weights[b] = texture(weightMaps, vec3(fTexCoord, b))[0];
    }

    // if MFD is normalized, fresnel is the integral of cosine-weighted F * D over the hemisphere
    vec4 fresnelOverPi = vec4(0.0);

    // when cos(theta) is 1.0, sin(theta) is 0.0 so the integrand is also zero.
    float thetaPrev = 0.0;
    vec4 integrandOver2PiPrev = vec4(0.0);

//    // Do a separate sum that's biased towards samples that are less likely to have been overexposed in the original image
//    // to avoid the color appearing undersaturated (i.e. biased towards white).
//    float colorWeightPrev = 0.0;
//    vec4 colorSum = vec4(0.0);

    // start with m=1 since m=0 has an integrand of zero.
    for (int m = 1; m <= BASIS_RESOLUTION; m++)
    {
        MFDEval eval = evalMFD(m, weights);

        float dH = eval.thetaH-thetaPrev; // TODO

        // see PBRT p. 346, 538
        // Would include a factor of 2pi as well for the outer integral, but our goal is to calculate Fresnel / pi, so the pi drops out
        // The factor of 2 drops out in implementing the trapezoid rule
        vec4 integrandOver2Pi = vec4(eval.mfd, 1.0) * /* cos(theta): */ eval.nDotH * /* sin(theta): */ sqrt(1 - eval.nDotH * eval.nDotH);

        // Trapezoid rule
        fresnelOverPi += integrandOver2PiPrev * dH + integrandOver2Pi * dH;

        // old current becomes previous
        thetaPrev = eval.thetaH;
        integrandOver2PiPrev = integrandOver2Pi;

//        float colorWeight = (1.0 - getLuminance(eval.mfd));
//        colorSum += integrandOver2PiPrev * dH * colorWeightPrev + integrandOver2Pi * dH * colorWeight;
//        colorWeightPrev = colorWeight;
    }

    // Handle the tail (60 to 90 degree range)
    MFDEval evalTail = evalMFD(BASIS_RESOLUTION, weights);

    // Integral of cos(theta) * sin(theta) from 0 degrees to 90 degrees should be 1/2=0.5
    // Integral of cos(theta) * sin(theta) from 60 degrees to 90 degrees should be 1/8=0.125 (25% of total)
    // but use 0.5 - [integral from 0 to 60] to ensure exact normalization.
    fresnelOverPi += (0.5 - fresnelOverPi.a) * vec4(evalTail.mfd, 1.0);

//    // Use the luminance from the integral but the RGB from the weighted average
//    fresnelOverPi.rgb = vec3(getLuminance(fresnelOverPi.rgb)) * colorSum.rgb / getLuminance(colorSum.rgb);

    // Enforce minimum reflectivity for stability
//    fresnelOverPi = max(fresnelOverPi, 0.04 / PI);

    vec2 sums = vec2(0.0);

    for (int m = 0; m <= BASIS_RESOLUTION; m++)
    {
        MFDEval eval = evalMFD(m, weights);
        float nDotHSq = eval.nDotH * eval.nDotH;

//        eval.mfd = max(eval.mfd, vec3(0.0));

        // Solution from quadratic formula
        // m = [sqrt(fresnel/pi) + sqrt(fresnel/pi - 4 * cos(theta)^2 * sin(theta)^2 * [D*F])] / [2 cos(theta)^2 * sqrt([D*F])]
        // ^ Two solutions, but only the higher roughness (the + in the +/- of the quadratic formula) is reliable
        // The second (the - solution) could predict false specular peaks as it tends to assume the sample is in the tail of the MFD.
        float numerator = dot(vec3(1.0), // Treat each color channel as a separate sample
            sqrt(fresnelOverPi.rgb) + sqrt(max(fresnelOverPi.rgb - 4 * nDotHSq * (1 - nDotHSq) * eval.mfd, 0.0)));
        float denominator = dot(vec3(1.0), 2 * nDotHSq * sqrt(eval.mfd));

        sums += vec2(numerator, denominator) * denominator  * /* sin(theta): */ sqrt(1 - nDotHSq)
            // numerator / denominator <= 1 => numerator <= denominator => denominator - numerator >= 0
            * max(0.0, denominator - numerator); // Ensure roughness estimates are <= 1.
            //* (dot(eval.mfd - fresnelOverPi.rgb, vec3(1.0))); // Prevent off-specular pixels from predicting false specular peaks
    }

//    // cos(60 deg) = 0.5; sin(60 deg) = sqrt(0.75)
//    // 1 / (2 cos(theta)^2) = 1 / (2 * 0.5 * 0.5) = 2
//    float roughnessEstimateTail = dot(vec3(1.0),
//        (sqrt(fresnelOverPi.rgb) - sqrt(max(fresnelOverPi.rgb - 0.75 * evalTail.mfd, 0.0))) * 2.0 / evalTail.mfd);
//
//    // integral of denominator^2 * sin(theta) = sin(theta) * cos(theta)^4 from 0 degrees to 60 degrees is 31/160
//    // integral of denominator^2 * sin(theta) = sin(theta) * cos(theta)^4 from 60 degrees to 90 degrees is 1/160
//    // 1/32 = 0.03125
//    // dot product to average
//    float roughness = mix(sums.x / sums.y, roughnessEstimateTail, 0.03125);

    float roughness = sums.x / sums.y;

    specularColor = vec4(linearToSRGB(fresnelOverPi.rgb * PI), 1.0);
    sqrtRoughness = vec4(vec3(sqrt(roughness)), 1.0);
}
