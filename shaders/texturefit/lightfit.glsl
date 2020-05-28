/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#ifndef LIGHTFIT_GLSL
#define LIGHTFIT_GLSL

#include "../colorappearance/colorappearance.glsl"

#line 7 2001

uniform float delta;
uniform int iterations;
uniform int lightIndex;

struct LightFit
{
    vec3 position;
    float intensity;
    float quality;
};

bool validateFit(LightFit fit)
{
    return  ! isnan(fit.position.x) && ! isnan(fit.position.y) && ! isnan(fit.position.z) &&
            ! isinf(fit.position.x) && ! isinf(fit.position.y) && ! isinf(fit.position.z) &&
            ! isnan(fit.intensity)  && ! isnan(fit.intensity) &&
            ! isinf(fit.quality)    && ! isinf(fit.quality);
}

LightFit fitLight()
{
    vec3 normal = normalize(fNormal);
    
    LightFit fit = LightFit(vec3(0), 0, 0);
    float weightedIntensitySum;
    float weightSum;
    
    for (int k = 0; k < iterations; k++)
    {
        weightedIntensitySum = 0;
        weightSum = 0;
    
        mat3 a = mat3(0);
        vec3 b = vec3(0);
        
        for (int i = 0; i < viewCount; i++)
        {
            if(getLightIndex(i) == lightIndex)
            {
                vec3 viewNormal = (cameraPoses[i] * vec4(normal, 0.0)).xyz;
                vec3 surfacePosition = (cameraPoses[i] * vec4(fPosition, 1.0)).xyz;
                float nDotV = dot(viewNormal, normalize(-surfacePosition));

                // Physically plausible values for the color components range from 0 to pi
                // We don't need to scale by pi because it will just cancel out
                // when we divide by the diffuse albedo estimate in the end.
                vec4 color = getLinearColor(i);

                if (color.a * nDotV > 0)
                {
                    vec3 lightUnnorm = fit.position-surfacePosition;
                    float lightSqr = dot(lightUnnorm, lightUnnorm);
                    vec3 scaledNormal = viewNormal * inversesqrt(lightSqr);
                    float nDotL = max(0, dot(scaledNormal, lightUnnorm));
                    scaledNormal /= lightSqr;
                    vec3 sampleVector = vec3(scaledNormal.xy, -dot(scaledNormal, surfacePosition));
                    float intensity = getLuminance(color.rgb);

                    float weight = color.a * nDotV;
                    if (k != 0)
                    {
                        // TODO: think more about whether error should be divided by fit.intensity
                        float error = intensity - fit.intensity * dot(scaledNormal, lightUnnorm);
                        weight *= exp(-error*error/(2*delta*delta));
                    }

                    a += weight * outerProduct(sampleVector, sampleVector);
                    b += weight * intensity * sampleVector;
                    weightedIntensitySum += weight * intensity;
                    weightSum += weight * nDotL;
                }
            }
        }
        
        vec3 solution = inverse(a) * b;
        fit.position = vec3(solution.xy, 0.0) / solution.z;
        // Intensity is close to:
        // weighted sum of intensity * [n dot l / light log10distance squared]
        // divided by weighted sum of [n dot l squared / light log10distance ^ 4]
        // = estimate of intensity / n dot l * light log10distance squared
        // = estimate of albedo * light log10distance squared
        // = estimate of albedo * light intensity required to have an incident intensity of one
        // at a typical surface position
        // TODO: there are probably more robust ways to estimate the average light log10distance squared
        fit.intensity = solution.z; 
        fit.quality = max(solution.z * determinant(a) / weightSum, 0.0);
    }
    
    if (!validateFit(fit))
    {
        fit.position = vec3(0.0);
        fit.intensity = 0.0;
        fit.quality = 0.0;
    }
    else
    {
        // Effectively divide by a diffuse albedo estimate to get the light intensity alone.
        // The diffuse albedo is computed under the assumption that the incident light intensity is 
        // effectively "one" at the object's position.
        // This is reasonable since the luminance curve is balanced so that a reflectance of "one" 
        // corresponds to 100% of the incident light being reflected diffusely.
        // (Based on the XRite/MacBeth chart, which is at a location essentially the same log10distance
        // from the light source as the object.)
        fit.intensity *= weightSum / weightedIntensitySum;
    }
    
    return fit;
}

#endif // LIGHTFIT_GLSL
