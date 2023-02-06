#version 330

/*
 *  Copyright (c) Michael Tetzlaff 2023
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#line 16 0

layout(location = 0) out vec4 specularColor;
layout(location = 1) out vec4 sqrtRoughness;
layout(location = 2) out vec4 dampingOut;

void main()
{
    mat4 mJTJ = mat4(0);
    vec4 vJTb = vec4(0);

    float dampingFactor = texture(dampingTex, fTexCoord)[0];
    vec3 reflectivityGammaPrev = texture(specularEstimate, fTexCoord).rgb;
    vec3 reflectivity = pow(reflectivityGammaPrev, vec3(gamma));
    float sqrtRoughnessPrev = texture(roughnessEstimate, fTexCoord)[0];
    float roughness = sqrtRoughnessVal * sqrtRoughnessVal;
    float roughnessSquared = roughness * roughness;
    vec3 diffuse = pow(texture(diffuseColor, fTexCoord).rgb, vec3(gamma));

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

        vec3 gammaDerivative = gammaInv * pow(brdfTimesPi, gammaInv - 1);

        vec3 reflectivityGradients = gammaDerivative * mdfTimesPi;
        vec3 roughnessGradients = gammaDerivative * mfdDerivativeTimesPi * reflectivity;

        float weight = nDotH * nDotH; // cosine weighting

        // Calculate LHS
        // treat each color channel as a separate observation
        vec4 redGradient = vec4(reflectivityGradients.r, 0, 0, roughnessGradients.r);
        vec4 greenGradient = vec4(0, reflectivityGradients.g, 0, roughnessGradients.g);
        vec4 blueGradient = vec4(0, 0, reflectivityGradients.b, roughnessGradients.b);
        mJTJ += weight * redGradient * transpose(redGradient) + mat2(dampingFactor);
        mJTJ += weight * greenGradient * transpose(greenGradient) + mat2(dampingFactor);
        mJTJ += weight * blueGradient * transpose(blueGradient) + mat2(dampingFactor);

        // Calculate RHS
        vec3 target = vec3(0);
        for (int b = 0; b < BASIS_COUNT; b++)
        {
            target += weights[b] * texelFetch(basisFunctions, ivec2(m, b), 0).rgb;
        }

        vec3 diff = pow(4 * diffuse.r + PI * target.r, gammaInv) - pow(brdfTimes4Pi.r, gammaInv);

        vJTb += weight * vec4(reflectivityGradients * diff, dot(roughnessGradients, diff));
    }

    if (determinant(mJTJ) > 0)
    {
        vec4 delta = inverse(mJTJ) * vJTb;// gamma corrected RGB reflectivity, sqrt(roughness)

        vec3 newReflectivity = reflectivityGammaPrev + delta.rgb;
        float newSqrtRoughness = sqrtRoughnessPrev + delta.a;

        float newError = calculateError(triangleNormal, tangentToObject * newNormalTS);

        if (!isnan(newReflectivity.r) && !isnan(newReflectivity.g) && !isnan(newReflectivity.b)
        && !isnan(newReflectivity.a) && newError < prevError)
        {
            // Accept iteration
            specularColor = vec4(newReflectivity, 1.0);
            sqrtRoughness = vec4(vec3(newSqrtRoughness), 1.0);
            dampingOut = vec4(vec3(max(MIN_DAMPING, dampingFactor / 2.0)), 1.0);
            return;
        }
    }

    // Reject iteration if not accepted
    specularColor = vec4(reflectivityGamma, 1.0);
    sqrtRoughness = vec4(vec3(sqrtRoughnessPrev), 1.0);
    dampingOut = vec4(vec3(dampingFactor * 2.0), 1.0);
}
