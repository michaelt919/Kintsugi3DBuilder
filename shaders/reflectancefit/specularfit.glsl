/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#line 5 2004

#define USE_LIGHT_INTENSITIES 1

#define MIN_ROUGHNESS 0.00390625    // 1/256
#define MAX_ROUGHNESS 1.0

#define MIN_SPECULAR_REFLECTIVITY 0.04 // corresponds to dielectric with index of refraction = 1.5
#define MAX_ROUGHNESS_WHEN_CLAMPING MAX_ROUGHNESS

#define ENABLE_DIFFUSE_ADJUSTMENT 1
#define ENABLE_NORMAL_ADJUSTMENT 1

#define CHROMATIC_SPECULAR 1

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;

vec4 getDiffuseColor()
{
    vec4 textureResult = texture(diffuseEstimate, fTexCoord);
    return vec4(pow(textureResult.rgb, vec3(gamma)), textureResult.a);
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

struct ParameterizedFit
{
    vec4 diffuseColor;
    vec4 normal;
    vec4 specularColor;
    vec4 roughness;
    vec4 roughnessStdDev;
};

vec4 removeDiffuse(vec4 originalColor, vec3 diffuseColor, vec3 light,
                    vec3 attenuatedLightIntensity, vec3 normal, float maxLuminance)
{
    vec3 diffuseContrib = diffuseColor * max(0, dot(light, normal)) * attenuatedLightIntensity;

#if CHROMATIC_SPECULAR
    float cap = maxLuminance - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
    vec3 remainder = clamp(originalColor.rgb - diffuseContrib, 0, cap);
    return vec4(remainder, cap);
#else
    vec3 remainder = max(originalColor.rgb - diffuseContrib, vec3(0));
    float remainderMin = min(min(remainder.r, remainder.g), remainder.b);
    return vec4(vec3(remainderMin), 1.0);
#endif
}

// Performs the specular parameter estimation algorithm.
// Because this is a fragment shader, it is assumed throughout that we are dealing with a single position on the reflecting surface.
// This is facilitated by the use of projective texture mapping to determine what pixels map onto the current surface position.
ParameterizedFit fitSpecular()
{
    // Normalize the surface normal vector and build an orthonormal basis for the tangent space.
    vec3 normal = normalize(fNormal);

    vec3 tangent = normalize(fTangent - dot(normal, fTangent) * normal);
    vec3 bitangent = normalize(fBitangent
        - dot(normal, fBitangent) * normal
        - dot(tangent, fBitangent) * tangent);

    // Transform the old diffuse normal from tangent space into object space to allow us to remove the diffuse component.
    mat3 tangentToObject = mat3(tangent, bitangent, normal);
    vec3 diffuseNormalTS = getDiffuseNormalVector();
    vec3 oldDiffuseNormal = tangentToObject * diffuseNormalTS;

    // Get the diffuse color.
    vec4 diffuseColor = getDiffuseColor();

    // Get the brightest representable luminance value given the current tonemapping assumptions.
    float maxLuminance = getMaxLuminance();

    // Search for the brightest direction
    vec3 maxResidual = vec3(0);
    vec2 maxResidualLuminance = vec2(0);
    vec4 maxResidualDirection = vec4(0);
    int maxResidualIndex = -1;

    // Iterate over all the views of the reflecting surface.
    for (int i = 0; i < VIEW_COUNT; i++)
    {
        // Look up the measured luminance in the current view at this surface position.
        vec4 color = getLinearColor(i);

        // Determine the cosine of the angle between the view direction and the surface normal.
        vec3 view = normalize(getViewVector(i));
        float nDotV = dot(view, normal);

        if (color.a * nDotV > 0)
        {
            // Look up the light direction and intensity (with inverse-square law attenuation accounted for).
            LightInfo lightInfo = getLightInfo(i);
            vec3 light = lightInfo.normalizedDirection;

            // Remove the diffuse contribution from the reflectance measurement.
            vec3 colorRemainder;

#if USE_LIGHT_INTENSITIES
            colorRemainder = removeDiffuse(color, diffuseColor.rgb, light, lightInfo.attenuatedIntensity, oldDiffuseNormal, maxLuminance).rgb
                / lightInfo.attenuatedIntensity;
#else
            colorRemainder = removeDiffuse(color, diffuseColor.rgb, light, vec3(1.0), oldDiffuseNormal, maxLuminance).rgb;
#endif

            float luminance = getLuminance(colorRemainder);

            // Find the halfway direction where ideal specular reflection would occur between the current incoming (light) and outgoing (view) directions.
            vec3 halfway = normalize(view + light);

            // Decrease the importance of directions where the reflecting surface is being viewed at a strong angle
            // (where both camera alignment errors and grazing reflectance effects are both more likely.
            float weight = clamp(2 * nDotV, 0, 1);

            // If the luminance is greater than the previous max luminance, update the maximum luminance and the associated information with this sample.
            if (luminance * weight > maxResidualLuminance[0] * maxResidualLuminance[1])
            {
                maxResidualLuminance = vec2(luminance, weight);
                maxResidual = colorRemainder;
                maxResidualDirection = vec4(halfway, 1);
                maxResidualIndex = i;
            }
        }
    }

    vec3 specularNormal;

#if ENABLE_NORMAL_ADJUSTMENT
    specularNormal = maxResidualDirection.xyz; // Set the normal direction to be the direction where we observed maximum specular reflectance.
#else
    specularNormal = oldDiffuseNormal;
#endif

    // Estimate the roughness and specular reflectivity (in an RGB color space).
    vec3 roughness;
    vec3 roughnessSquared;
    vec3 roughnessStdDev;
    vec3 specularColorEstimate;

    // Compute several summations over all the available views simultaneously
    vec3 specularSumA = vec3(0.0); // Weighted sum of sqrt((1 - (n.h)^2) * sqrt(observed luminance at view k * (n.v)))
    vec3 specularSumB = vec3(0.0); // Weighted sum of sqrt(sqrt(maximum luminance) - (n.h)^2 * sqrt(observed luminance at view k * (n.v)))

    vec3 squaredWeightSum = vec3(0.0); // Sum of the squares of the weights (will be used for computing mean squared error later).

    vec4 sumResidual = vec4(0.0); // Keep track of how much total specular reflectance was observed - if this is low, the problem may be underdetermined.

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        vec3 view = normalize(getViewVector(i));

        // Compute cosine of the angle between the triangle surface normal (more robust than the refined surface normal estimate for certain purposes)
        // and the view direction.
        // This is mainly just used to reduce the influence of samples that are being viewed at grazing angles.
        float nDotVTriangle = max(0, dot(view, normal));

        // Values of 1.0 for this color would correspond to the expected reflectance
        // for an ideal diffuse reflector (diffuse albedo of 1), which is a reflectance of 1 / pi.
        // Hence, this color corresponds to the reflectance times pi.
        // Both the Phong model and the Cook Torrance with a Beckmann distribution also have a 1/pi factor.
        // By adopting the convention that all reflectance values are scaled by pi in this shader,
        // We can avoid division by pi here as well as the 1/pi factors in the parameterized models.
        vec4 color = getLinearColor(i);

        if (color.a * nDotVTriangle > 0 && i != maxResidualIndex)
        {
            // Look up the light direction and intensity (with inverse-square law attenuation accounted for).
            LightInfo lightInfo = getLightInfo(i);
            vec3 light = lightInfo.normalizedDirection;

            // Remove the diffuse contribution from the reflectance measurement.
            vec3 colorRemainder;

#if USE_LIGHT_INTENSITIES
            colorRemainder = removeDiffuse(color, diffuseColor.rgb, light, lightInfo.attenuatedIntensity, specularNormal, maxLuminance).rgb
                / lightInfo.attenuatedIntensity;
#else
            colorRemainder = removeDiffuse(color, diffuseColor.rgb, light, vec3(1.0), specularNormal, maxLuminance).rgb;
#endif

            // Compute various cosine factors.
            float nDotL = max(0, dot(light, specularNormal));
            float nDotV = max(0, dot(specularNormal, view));

            vec3 halfway = normalize(view + light);
            float nDotH = dot(halfway, specularNormal);
            float nDotHSquared = nDotH * nDotH;

            if (nDotV > 0 && nDotL > 0 && nDotHSquared > 0.5)
            {
                // Compute the numerator and denominator (the two quantities we are computing summations over).
                vec3 numerator = sqrt(max(vec3(0.0), (1 - nDotHSquared) * sqrt(colorRemainder * nDotV)));
                vec3 denominatorSq = max(vec3(0.0), sqrt(maxResidual) - nDotHSquared * sqrt(colorRemainder * nDotV));
                vec3 denominator = sqrt(denominatorSq);

                // Weight near-normal samples and brighter samples higher than grazing-angle samples or samples that are darker.
                vec3 weight = nDotVTriangle * sqrt(colorRemainder);

                // Weight each sum by the denominator squared.  This avoids a singularity for peak specular observations.
                specularSumA += weight * denominator * numerator;
                specularSumB += weight * denominatorSq;
                squaredWeightSum += weight * weight * denominatorSq * denominatorSq;

                sumResidual += nDotVTriangle * vec4(colorRemainder, 1.0);
            }
        }
    }

    // If no specular samples were available, return.
    if (sumResidual.w == 0.0 || (specularSumB.r == 0.0 && specularSumB.g == 0.0 && specularSumB.b == 0.0))
    {
        return ParameterizedFit(diffuseColor, vec4(diffuseNormalTS, 1), vec4(0), vec4(0), vec4(0));
    }

    // Compute the surface roughness as the ratio between the two summations.
    // Restrict the range of roughness values to ensure that the reflectivity is always at least 0.04 and that the roughness is always less than 1.
    // (These values are configurable at the top of this shader program.)
    roughnessSquared = vec3(clamp(getLuminance(specularSumA * specularSumA / (specularSumB * specularSumB)),
        max(MIN_ROUGHNESS * MIN_ROUGHNESS, MIN_SPECULAR_REFLECTIVITY / (4 * maxResidualLuminance[0])),
        MAX_ROUGHNESS * MAX_ROUGHNESS));
    roughness = sqrt(roughnessSquared);

    // Estimate the specular reflectivity using the constraint that the specular peak must match the maximum observed luminance.
    specularColorEstimate = clamp(4 * maxResidualLuminance[0] * roughnessSquared, MIN_SPECULAR_REFLECTIVITY, 1.0)
        * sumResidual.rgb / max(0.01 * sumResidual.w, getLuminance(sumResidual.rgb));

    // Compute weighted mean squared error of roughness estimates.
    // To do this we first need to calculate the weighted sum of squared error.
    vec3 sumSquaredError = vec3(0.0);

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        vec3 view = normalize(getViewVector(i));
        float nDotVTriangle = max(0, dot(view, normal));

        // Values of 1.0 for this color would correspond to the expected reflectance
        // for an ideal diffuse reflector (diffuse albedo of 1), which is a reflectance of 1 / pi.
        // Hence, this color corresponds to the reflectance times pi.
        // Both the Phong model and the Cook Torrance with a Beckmann distribution also have a 1/pi factor.
        // By adopting the convention that all reflectance values are scaled by pi in this shader,
        // We can avoid division by pi here as well as the 1/pi factors in the parameterized models.
        vec4 color = getLinearColor(i);

        if (color.a * nDotVTriangle > 0 && i != maxResidualIndex)
        {
            LightInfo lightInfo = getLightInfo(i);
            vec3 light = lightInfo.normalizedDirection;

            vec3 colorRemainder;

#if USE_LIGHT_INTENSITIES
            colorRemainder = removeDiffuse(color, diffuseColor.rgb, light, lightInfo.attenuatedIntensity, specularNormal, maxLuminance).rgb
                / lightInfo.attenuatedIntensity;
#else
            colorRemainder = removeDiffuse(color, diffuseColor.rgb, light, vec3(1.0), specularNormal, maxLuminance).rgb;
#endif

            float nDotL = max(0, dot(light, specularNormal));
            float nDotV = max(0, dot(specularNormal, view));

            vec3 halfway = normalize(view + light);
            float nDotH = dot(halfway, specularNormal);
            float nDotHSquared = nDotH * nDotH;

            if (nDotV > 0 && nDotL > 0 && nDotHSquared > 0.5)
            {
                // Calculate the same numerator and denominator as before.
                vec3 numerator = sqrt(max(vec3(0.0), (1 - nDotHSquared) * sqrt(colorRemainder * nDotV)));
                vec3 denominatorSq = max(vec3(0.0), sqrt(maxResidual) - nDotHSquared * sqrt(colorRemainder * nDotV));
                vec3 denominator = sqrt(denominatorSq);
                vec3 weight = nDotVTriangle * sqrt(colorRemainder);

                // Evaluate the difference between the current roughness estimate (numerator / denominator) and the weighted average of roughness estimates.
                vec3 diff = (numerator - roughness.y * denominator);

                // Weight this difference using the same weight as before.
                sumSquaredError += weight * diff * diff;
            }
        }
    }

    // Convert sum of squared error to mean squared error.
    roughnessStdDev = sqrt(specularSumB * sumSquaredError / (specularSumB * specularSumB - squaredWeightSum));



    // Enforce monochrome constraints, and ensure a minimum specular reflectivity in cases where there may be ambiguity between the specular and diffuse terms.
    // The roughness estimate may be updated as necessary for consistency.
    vec3 specularColor;

#if ENABLE_DIFFUSE_ADJUSTMENT
    if (diffuseColor.rgb != vec3(0.0) && MIN_SPECULAR_REFLECTIVITY > 0.0 && getLuminance(specularColorEstimate) < MIN_SPECULAR_REFLECTIVITY)
    {

#if CHROMATIC_SPECULAR
        vec3 specularColorBounded = max(specularColorEstimate,
            min(min(MAX_ROUGHNESS_WHEN_CLAMPING * MAX_ROUGHNESS_WHEN_CLAMPING * (4 * maxResidual),
                    16 * diffuseColor.rgb * maxResidual + specularColorEstimate),
                    vec3(MIN_SPECULAR_REFLECTIVITY)));

        roughnessSquared = vec3(clamp(getLuminance(specularColorBounded) / (4 * maxResidualLuminance[0]),
            MIN_ROUGHNESS * MIN_ROUGHNESS, MAX_ROUGHNESS_WHEN_CLAMPING * MAX_ROUGHNESS_WHEN_CLAMPING));

        specularColor = specularColorBounded;
#else
        specularColor = vec3(max(getLuminance(specularColorEstimate),
            min(min(MAX_ROUGHNESS_WHEN_CLAMPING * MAX_ROUGHNESS_WHEN_CLAMPING * (4 * maxResidualLuminance[0]),
                    16 * getLuminance(diffuseColor.rgb) * maxResidualLuminance[0] + getLuminance(specularColorEstimate)),
                    MIN_SPECULAR_REFLECTIVITY)));

        roughnessSquared = vec3(clamp(specularColor.g / (4 * maxResidualLuminance[0]),
            MIN_ROUGHNESS * MIN_ROUGHNESS, MAX_ROUGHNESS_WHEN_CLAMPING * MAX_ROUGHNESS_WHEN_CLAMPING));
#endif

        roughness = sqrt(roughnessSquared);
    }
    else
#endif
    {

#if CHROMATIC_SPECULAR
        specularColor = specularColorEstimate;
#else
        specularColor = vec3(getLuminance(specularColorEstimate));
#endif

    }

    // Refit the diffuse color using a simple weighted average after subtracting the final specular estimate.
    vec4 adjustedDiffuseColor;

#if ENABLE_DIFFUSE_ADJUSTMENT
    if (diffuseColor.rgb != vec3(0.0))
    {
        vec4 sumDiffuse = vec4(0.0);

        for (int i = 0; i < VIEW_COUNT; i++)
        {
            vec3 view = normalize(getViewVector(i));

            // Values of 1.0 for this color would correspond to the expected reflectance
            // for an ideal diffuse reflector (diffuse albedo of 1), which is a reflectance of 1 / pi.
            // Hence, this color corresponds to the reflectance times pi.
            // Both the Phong model and the Cook Torrance with a Beckmann distribution also have a 1/pi factor.
            // By adopting the convention that all reflectance values are scaled by pi in this shader,
            // We can avoid division by pi here as well as the 1/pi factors in the parameterized models.
            vec4 color = getLinearColor(i);

            if (color.a * dot(view, normal) > 0)
            {
                LightInfo lightInfo = getLightInfo(i);
                vec3 light = lightInfo.normalizedDirection;

                float nDotL = max(0, dot(light, specularNormal));
                float nDotV = max(0, dot(specularNormal, view));

                vec3 halfway = normalize(view + light);
                float nDotH = dot(halfway, specularNormal);
                float nDotHSquared = nDotH * nDotH;

                if (nDotV > 0 && nDotL > 0)
                {
                    // Evaluate the specular BRDF.
                    vec3 q1 = roughnessSquared + (1.0 - nDotHSquared) / nDotHSquared;
                    vec3 mfdEval = roughnessSquared / (nDotHSquared * nDotHSquared * q1 * q1);

                    float hDotV = max(0, dot(halfway, view));
                    float geomRatio = min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV) / (4 * nDotV);

                    vec3 specularTerm = min(vec3(1), specularColor) * mfdEval * geomRatio;

                    // Add the residual after subtracting the specular term to the weighted sum.
#if USE_LIGHT_INTENSITIES
                    sumDiffuse += vec4(max(vec3(0.0), nDotL * (color.rgb / lightInfo.attenuatedIntensity - specularTerm)), nDotL * nDotL);
#else
                    sumDiffuse += vec4(max(vec3(0.0), nDotL * (color.rgb - specularTerm)), nDotL * nDotL);
#endif
                }
            }
        }

        // Compute the average residual to get the adjusted diffuse color.
        if (sumDiffuse.a > 0.0)
        {
            adjustedDiffuseColor = vec4(sumDiffuse.rgb / sumDiffuse.a, 1);
        }
        else
        {
            // Discard and hole fill
            adjustedDiffuseColor = vec4(0.0);
        }
    }
    else
    {
        adjustedDiffuseColor = vec4(0, 0, 0, 1);
    }
#else
    adjustedDiffuseColor = diffuseColor;
#endif

    // Return the final results.
    return ParameterizedFit(adjustedDiffuseColor, vec4(normalize(transpose(tangentToObject) * specularNormal), 1),
        vec4(specularColor, 1), vec4(roughness, 1), vec4(roughnessStdDev, 1));
}

#endif // SPECULARFIT_GLSL
