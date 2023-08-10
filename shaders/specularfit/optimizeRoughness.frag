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

uniform float gamma;
uniform float gammaInv;

#ifndef PI
#define PI 3.1415926535897932384626433832795
#endif

#include "evaluateBRDF.glsl"
#include "basisToGGXError.glsl"

#line 28 0

#ifndef MIN_DAMPING
#define MIN_DAMPING 1.0
#endif

layout(location = 0) out vec4 specularColor;
layout(location = 1) out vec4 sqrtRoughness;
layout(location = 2) out vec4 dampingErrorOut;

uniform sampler2D diffuseEstimate;
uniform sampler2D specularEstimate;
uniform sampler2D roughnessEstimate;
uniform sampler2D dampingTex;

// TODO: This is an attempt at implementing Levenberg-Marquardt for GGX roughness estimation, but it doesn't work and hasn't been debugged.
void main()
{
    mat4 mJTJ = mat4(0);
    vec4 vJTb = vec4(0);

    vec2 dampingError = texture(dampingTex, fTexCoord).rg;
    float dampingFactor = dampingError[0];
    vec3 reflectivityGammaPrev = texture(specularEstimate, fTexCoord).rgb;
    vec3 reflectivity = pow(reflectivityGammaPrev, vec3(gamma));
    float sqrtRoughnessPrev = texture(roughnessEstimate, fTexCoord)[0];
    float roughness = sqrtRoughnessPrev * sqrtRoughnessPrev;
    float roughnessSquared = roughness * roughness;
    vec3 diffuse = pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));

    for (int m = 1; m < MICROFACET_DISTRIBUTION_RESOLUTION; m++)
    {
        float sqrtAngle = float(m) / float(MICROFACET_DISTRIBUTION_RESOLUTION);
        float nDotH = cos(sqrtAngle * sqrtAngle * PI / 3.0);
        float nDotHSq = nDotH * nDotH;
        float sqrtDenominator = (roughnessSquared - 1) * nDotHSq + 1;
        float denominator = sqrtDenominator * sqrtDenominator;

        // pre-multiplied by pi
        float mfdTimesPi = roughnessSquared / denominator;

        // derivative with respect to sqrt(roughness)
        float mfdDerivativeTimesPi = (1 - nDotHSq * (1 + roughnessSquared)) / (denominator * sqrtDenominator)
            * 4 * roughnessSquared * roughness;

        vec3 brdfTimes4Pi = 4 * diffuse + mfdTimesPi * reflectivity; // ignore masking / shadowing

        vec3 gammaDerivative = gammaInv * pow(brdfTimes4Pi, vec3(gammaInv - 1));

        vec3 reflectivityGradients = gammaDerivative * mfdTimesPi;
        vec3 roughnessGradients = gammaDerivative * mfdDerivativeTimesPi * reflectivity;

        float weight = nDotH * nDotH; // cosine weighting

        // Calculate LHS
        // treat each color channel as a separate observation
        vec4 redGradient = vec4(reflectivityGradients.r, 0, 0, roughnessGradients.r);
        vec4 greenGradient = vec4(0, reflectivityGradients.g, 0, roughnessGradients.g);
        vec4 blueGradient = vec4(0, 0, reflectivityGradients.b, roughnessGradients.b);
        mJTJ += weight * outerProduct(redGradient,redGradient) + mat4(dampingFactor);
        mJTJ += weight * outerProduct(greenGradient,greenGradient) + mat4(dampingFactor);
        mJTJ += weight * outerProduct(blueGradient,blueGradient) + mat4(dampingFactor);

        // Calculate RHS
        vec3 target = getMFDEstimateRaw(m); // Use nonlinear encoding of cosine ("m") directly rather than N dot H

        vec3 diff = pow(4 * diffuse + PI * target, vec3(gammaInv)) - pow(brdfTimes4Pi, vec3(gammaInv));

        vJTb += weight * vec4(reflectivityGradients * diff, dot(roughnessGradients, diff));
    }

    float prevError = dampingError[1];
    if (determinant(mJTJ) > 0)
    {
        vec4 delta = inverse(mJTJ) * vJTb;// gamma corrected RGB reflectivity, sqrt(roughness)

        vec3 newReflectivity = reflectivity + delta.rgb;
        float newSqrtRoughness = sqrtRoughnessPrev + delta.a;
        float newError = calculateError(diffuse, newReflectivity, newSqrtRoughness * newSqrtRoughness);

        if (!isnan(newReflectivity.r) && !isnan(newReflectivity.g) && !isnan(newReflectivity.b) && newError < prevError)
        {
            // Accept iteration
            specularColor = vec4(newReflectivity, 1.0);
            sqrtRoughness = vec4(vec3(newSqrtRoughness), 1.0);
            dampingErrorOut = vec4(max(MIN_DAMPING, dampingFactor / 2.0), newError, 0.0, 1.0);
            return;
        }
    }

    // Reject iteration if not accepted
    specularColor = vec4(reflectivityGammaPrev, 1.0);
    sqrtRoughness = vec4(vec3(sqrtRoughnessPrev), 1.0);
    dampingErrorOut = vec4(dampingFactor * 2.0, prevError, 0.0, 1.0);
}
