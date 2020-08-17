#version 330

/*
 *  Copyright (c) Michael Tetzlaff 2020
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#include "nam2018.glsl"
#include "evaluateBRDF.glsl"
#line 18 0

layout(location = 0) out vec4 normalTS;

uniform float dampingFactor;

#define COSINE_CUTOFF 0.0

#ifndef MICROFACET_DISTRIBUTION_RESOLUTION
#define MICROFACET_DISTRIBUTION_RESOLUTION 90
#endif

vec3 getMFDGradient(float nDotH)
{
    vec3 estimate = vec3(0);
    float wMid = sqrt(max(0.0, acos(nDotH) * 3.0 / PI));
    float wLow = wMid - 1.0 / MICROFACET_DISTRIBUTION_RESOLUTION;
    float wHigh = wMid + 1.0 / MICROFACET_DISTRIBUTION_RESOLUTION;

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        estimate += texture(weightMaps, vec3(fTexCoord, b))[0]
            * (texture(basisFunctions, vec2(wHigh, b)).rgb - texture(basisFunctions, vec2(wLow, b)).rgb);
    }

    float nDotH_Low = cos(wLow * wLow * PI / 3.0);
    float nDotH_High = cos(wHigh * wHigh * PI / 3.0);

    return estimate / (nDotH_High - nDotH_Low);
}

void main()
{
    vec3 triangleNormal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(triangleNormal, fBitangent) * triangleNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);

    vec2 prevNormalXY = texture(normalEstimate, fTexCoord).xy * 2 - vec2(1.0);
    vec3 prevNormalTS = vec3(prevNormalXY, sqrt(1 - dot(prevNormalXY, prevNormalXY)));
    vec3 prevNormal = tangentToObject * prevNormalTS;

    vec3 fittingTangent = normalize(vec3(1, 0, 0) - prevNormalTS.x * prevNormalTS);
    vec3 fittingBitangent = normalize(vec3(0, 1, 0) - prevNormalTS.y * prevNormalTS - fittingTangent.y * fittingTangent);
    mat3 fittingToTangent = mat3(fittingTangent, fittingBitangent, prevNormalTS);

    mat3 objectToFitting = transpose(fittingToTangent) * transpose(tangentToObject);

    mat2 mATA = mat2(0);
    vec2 vATb = vec2(0);

    float estimatedPeak = getLuminance(getBRDFEstimate(1.0, 0.25));

    for (int k = 0; k < CAMERA_POSE_COUNT; k++)
    {
        vec4 imgColor = getLinearColor(k);
        vec3 lightDisplacement = objectToFitting * getLightVector(k);
        vec3 light = normalize(lightDisplacement);
        vec3 view = objectToFitting * normalize(getViewVector(k));
        vec3 halfway = normalize(light + view);
        float nDotH = max(0.0, halfway.z);
        float nDotL = max(0.0, light.z);
        float nDotV = max(0.0, view.z);
        float triangleNDotV = max(0.0, dot(objectToFitting * triangleNormal, view));
        float triangleNDotL = max(0.0, dot(objectToFitting * triangleNormal, light));

        if (imgColor.a > 0.0 && nDotH > COSINE_CUTOFF && nDotL > COSINE_CUTOFF && nDotV > COSINE_CUTOFF && triangleNDotV > COSINE_CUTOFF)
        {
            float hDotV = max(0.0, dot(halfway, view));

            // "Light intensity" is defined in such a way that we need to multiply by pi to be properly normalized.
            vec3 incidentRadiance = PI * getLightIntensity(k) / dot(lightDisplacement, lightDisplacement);

            float roughness = texture(roughnessEstimate, fTexCoord)[0];
            float maskingShadowing = computeGeometricAttenuationVCavity(nDotH, nDotV, nDotL, hDotV);
//            vec3 reflectanceEstimate = getBRDFEstimate(nDotH, maskingShadowing / (4 * nDotL * nDotV));

            float weight = triangleNDotL;

            mat3x2 mfdGradient = outerProduct(halfway.xy, getMFDGradient(nDotH)); // (d NdotH / dN) * (dD / d NdotH)

            mat3x2 specularGradient;

            if (nDotV * nDotH > 0.5 * hDotV && nDotL * nDotH > 0.5 * hDotV)
            {
                // G = 1.0
                // f * nDotL = DF / (4 * nDotV)
                specularGradient = 0.25 * (nDotV * mfdGradient - outerProduct(view.xy, getMFDEstimate(nDotH))) / (nDotV * nDotV); // quotient rule
            }
            else if (nDotV < nDotL)
            {
                // G = 2 * nDotH * nDotV / hDotV
                // f * nDotL = DF * nDotH / (2 * hDotV)
                specularGradient = 0.5 * (nDotH * mfdGradient + outerProduct(halfway.xy, getMFDEstimate(nDotH))) / hDotV; // product rule
            }
            else
            {
                // G = 2 * nDotH * nDotL / hDotV
                // f * nDotL = DF * nDotH * nDotL / (2 * hDotV * nDotV)
                vec3 mfdEstimate = getMFDEstimate(nDotH);
                mat3x2 mfdNdotLGradient = nDotL * mfdGradient + outerProduct(light.xy, mfdEstimate);
                mat3x2 mfdGeomGradient = 0.5 * (nDotH * mfdNdotLGradient + outerProduct(halfway.xy, nDotL * mfdEstimate)) / hDotV; // product rule
                specularGradient = (nDotV * mfdGeomGradient - outerProduct(view.xy, mfdEstimate * maskingShadowing)) / (nDotV * nDotV); // quotient rule
            }

            mat3x2 diffuseGradient = outerProduct(light.xy, getDiffuseEstimate() / PI);

            // fullGradient is essentially a portion of A-transpose
            // The columns of fullGradient are R/G/B.
            // The rows correspond to the components of N.
            mat3x2 fullGradient = diffuseGradient + specularGradient;

            vec3 actualReflectanceTimesNDotL = imgColor.rgb / incidentRadiance;
            mATA += weight * weight * fullGradient * transpose(fullGradient);
                    //dot(reflectanceEstimate, reflectanceEstimate) * outerProduct(light, light);
            vATb += weight * weight * fullGradient * actualReflectanceTimesNDotL;
                    //dot(reflectanceEstimate, actualReflectanceTimesNDotL) * light;

        }
    }

    vec2 normalFittingSpace;

    if (determinant(mATA) > 0)
    {
        normalFittingSpace = inverse(mATA) * vATb;
    }
    else
    {
        normalTS = vec4(prevNormalTS * 0.5 + vec3(0.5), 1.0);
        return;
    }

    float tangentLengthSq = dot(normalFittingSpace, normalFittingSpace);
    float maxTangentLength = 0.0625;
    float tangentScale = min(dampingFactor, maxTangentLength / sqrt(tangentLengthSq));
    normalFittingSpace = normalFittingSpace * tangentScale;

    // To avoid oscillating divergence, dampen the new estimate by averaging it with the previous estimate.
    vec3 newNormalTS = fittingToTangent * vec3(normalFittingSpace, sqrt(max(0, 1 - tangentLengthSq * tangentScale)));

    // Map to the correct range for a texture.
    normalTS = vec4(newNormalTS * 0.5 + vec3(0.5), 1.0);
}
