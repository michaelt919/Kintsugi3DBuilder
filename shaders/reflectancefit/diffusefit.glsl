/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#ifndef DIFFUSEFIT_GLSL
#define DIFFUSEFIT_GLSL

#include "../colorappearance/colorappearance.glsl"

#line 20 2000

uniform float delta;
uniform int iterations;
uniform float fit1Weight;
uniform float fit3Weight;

#define SQRT2 1.4142135623730950488016887242097

struct DiffuseFit
{
    vec3 color;
    vec3 normalTS;
};

bool validateFit(DiffuseFit fit)
{
    return ! isnan(fit.color.r) && ! isnan(fit.color.g) && ! isnan(fit.color.b) &&
            ! isinf(fit.color.r) && ! isinf(fit.color.g) && ! isinf(fit.color.b);
}

// Performs the diffuse estimation algorithm.
DiffuseFit fitDiffuse()
{
    vec3 geometricNormal = normalize(fNormal);
    
    DiffuseFit fit = DiffuseFit(vec3(0), vec3(0));
    vec3 fitNormal;

    // Iteratively reweighted least squares.
    for (int k = 0; k < iterations; k++)
    {

        // Perform a linear regression to fit the function:
        // reflected radiance / incident radiance = (albedo * nx) * lx + (albedo * ny) * ly + (albedo * nz) * lz
        // where (lx, ly, lz) is the normalized light direction and we are solving for the albedo and the surface normal (nx, ny, nz).
        mat3 a = mat3(0);
        mat3 b = mat3(0);
        vec4 weightedRadianceSum = vec4(0.0);
        vec3 weightedIrradianceSum = vec3(0.0);
        vec3 directionSum = vec3(0);

        for (int i = 0; i < VIEW_COUNT; i++)
        {
            vec3 view = normalize(getViewVector(i));

            // Physically plausible values for the color components range from 0 to pi
            // We don't need to scale by 1 / pi because we would just need to multiply by pi again
            // at the end to get a diffuse albedo value.
            vec4 color = getLinearColor(i);

            float nDotV = dot(geometricNormal, view);
            if (color.a * nDotV > 0)
            {
                LightInfo lightInfo = getLightInfo(i);
                vec3 light = lightInfo.normalizedDirection;

                float weight = color.a * nDotV;
                if (k != 0)
                {
                    vec3 error = color.rgb - fit.color * dot(fitNormal, light) * lightInfo.attenuatedIntensity;
                    weight *= exp(-dot(error,error)/(2*delta*delta));
                }

                float attenuatedLuminance = getLuminance(lightInfo.attenuatedIntensity);

                a += weight * outerProduct(light, light);
                b += weight * outerProduct(light, color.rgb / lightInfo.attenuatedIntensity);

                float nDotL = max(0, dot(geometricNormal, light));
                weightedRadianceSum += weight * vec4(color.rgb, 1.0) * nDotL;
                weightedIrradianceSum += weight * lightInfo.attenuatedIntensity * nDotL * nDotL;

                directionSum += light;
            }
        }

        // Use the weights provided to regularize the solution if there isn't enough data or if the least squares problem is singular.
        if (fit3Weight > 0.0)
        {
            mat3 m = inverse(a) * b;
            vec3 rgbFit = vec3(length(m[0]), length(m[1]), length(m[2]));
            vec3 rgbScale = weightedRadianceSum.rgb / rgbFit;

            if (rgbFit.r == 0.0)
            {
                rgbScale.r = 0.0;
            }
            if (rgbFit.g == 0.0)
            {
                rgbScale.g = 0.0;
            }
            if (rgbFit.b == 0.0)
            {
                rgbScale.b = 0.0;
            }

            vec3 solution = m * rgbScale;

            float fit3Quality = clamp(fit3Weight * determinant(a) / weightedRadianceSum.a *
                                    clamp(dot(normalize(solution.xyz), geometricNormal), 0, 1), 0.0, 1.0);

            vec3 geometricNormal = normalize(fNormal);

            fit.color = clamp(weightedRadianceSum.rgb / max(max(rgbScale.r, rgbScale.g), rgbScale.b), 0, 1)
                                * fit3Quality
                            + clamp(weightedRadianceSum.rgb / weightedIrradianceSum, 0, 1)
                                * clamp(fit1Weight * weightedIrradianceSum, 0, 1 - fit3Quality);
            vec3 diffuseNormalEstimate = normalize(normalize(solution.xyz) * fit3Quality + geometricNormal * (1 - fit3Quality));

            float directionScale = length(directionSum);
            vec3 averageDirection = directionSum / max(1, directionScale);

            // sqrt2 / 2 corresponds to 45 degrees between the geometric surface normal and the average view direction.
            // This is the point at which there is essentially no information that can be obtained about the true surface normal in one dimension
            // since all of the views are presumably on the same side of the surface normal.
            float diffuseNormalFidelity = max(0, (dot(averageDirection, geometricNormal) - SQRT2 / 2) / (1 - SQRT2 / 2));

            // Compare to the average direction of all samples in case the sampling was biased.
            // If bias is found, use a heuristic to adjust the surface normal to account for this bias.
            vec3 certaintyDirectionUnnormalized = cross(averageDirection - diffuseNormalFidelity * geometricNormal, geometricNormal);
            vec3 certaintyDirection = certaintyDirectionUnnormalized
                / max(1, length(certaintyDirectionUnnormalized));

            float diffuseNormalCertainty =
                min(1, directionScale) * dot(diffuseNormalEstimate, certaintyDirection);
            vec3 scaledCertaintyDirection = diffuseNormalCertainty * certaintyDirection;
            fitNormal = normalize(
                scaledCertaintyDirection
                    + sqrt(1 - diffuseNormalCertainty * diffuseNormalCertainty
                                * dot(certaintyDirection, certaintyDirection))
                        * normalize(mix(geometricNormal, normalize(diffuseNormalEstimate - scaledCertaintyDirection),
                            min(1, directionScale) * diffuseNormalFidelity)));
        }
        else
        {
            // If the weight for the refined surface normal was zero, just compute the color as a weighted average of color samples
            // and use the triangle normal as the surface normal.
            fit.color = clamp(weightedRadianceSum.rgb / weightedIrradianceSum, 0, 1)
                                * clamp(fit1Weight * weightedIrradianceSum, 0, 1);
            fitNormal = fNormal;
        }
    }

    if (!validateFit(fit))
    {
        fit.color = vec3(0.0);
    }
    else
    {
        // Transform the surface normal from object space into tangent space.
        vec3 tangent = normalize(fTangent - dot(geometricNormal, fTangent));
        vec3 bitangent = normalize(fBitangent
            - dot(geometricNormal, fBitangent) * geometricNormal 
            - dot(tangent, fBitangent) * tangent);
            
        mat3 tangentToObject = mat3(tangent, bitangent, geometricNormal);
        mat3 objectToTangent = transpose(tangentToObject);
    
        fit.normalTS = objectToTangent * fitNormal;
    }
    
    return fit;
}

#endif // DIFFUSEFIT_GLSL
