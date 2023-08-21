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

#include "specularFit.glsl"
#include "normalError.glsl"
#line 19 0

layout(location = 0) out vec4 normalTS;
layout(location = 1) out vec4 dampingOut;

//uniform float dampingFactor;
uniform sampler2D dampingTex;

#ifndef BASIS_RESOLUTION
#define BASIS_RESOLUTION 90
#endif

#ifndef USE_LEVENBERG_MARQUARDT
#define USE_LEVENBERG_MARQUARDT 1
#endif

#ifndef MIN_DAMPING
#define MIN_DAMPING 1.0
#endif

vec3 getMFDGradient(float nDotH)
{
    vec3 estimate = vec3(0);
    float wMid = sqrt(max(0.0, acos(nDotH) * 3.0 / PI));
    float wLow = wMid - 1.0 / BASIS_RESOLUTION;
    float wHigh = wMid + 1.0 / BASIS_RESOLUTION;

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        estimate += texture(weightMaps, vec3(fTexCoord, b))[0]
            * (texture(basisFunctions, vec2(wHigh, b)).rgb - texture(basisFunctions, vec2(wLow, b)).rgb);
    }

    float nDotH_Low = cos(wLow * wLow * PI / 3.0);
    float nDotH_High = cos(wHigh * wHigh * PI / 3.0);

    return estimate / (nDotH_High - nDotH_Low);
}

vec2 getLambdaGradient(float roughness, vec3 direction)
{
    // Derivatives of:
    // -0.5 + 0.5 * sqrt(1 + roughness * roughness * (1 / (cosine * cosine) - 1.0))

    float roughnessSq = roughness * roughness;
    float cosineSq = direction.z * direction.z;

    return -0.5 * roughnessSq * direction.xy / (cosineSq * direction.z * sqrt(1 + roughnessSq * (1 / (cosineSq) - 1.0)));
}

vec2 getHeightCorrelatedSmithGradient(float roughness, vec3 light, vec3 view)
{
    // Derivatives of:
    // 1 / (1 + lambda(roughness, nDotV) + lambda(roughness, nDotL))

    float denominator = (1 + lambda(roughness, view.z) + lambda(roughness, light.z));
    return -(getLambdaGradient(roughness, view) + getLambdaGradient(roughness, light)) / (denominator * denominator);
}

void main()
{
    vec3 position = getPosition();

    mat3 tangentToObject = constructTBNExact();
    vec3 triangleNormal = tangentToObject[2];

    vec2 prevNormalXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
    vec3 prevNormalTS = vec3(prevNormalXY, sqrt(1 - dot(prevNormalXY, prevNormalXY)));

    vec3 fittingTangent = normalize(vec3(1, 0, 0) - prevNormalTS.x * prevNormalTS);
    vec3 fittingBitangent = normalize(vec3(0, 1, 0) - prevNormalTS.y * prevNormalTS - fittingTangent.y * fittingTangent);
    mat3 fittingToTangent = mat3(fittingTangent, fittingBitangent, prevNormalTS);

    mat3 objectToFitting = transpose(fittingToTangent) * transpose(tangentToObject);

#if USE_LEVENBERG_MARQUARDT
    mat2 mJTJ = mat2(0);
    vec2 vJTb = vec2(0);
#else
    mat3 mATA = mat3(0);
    vec3 vATb = vec3(0);
#endif

    float estimatedPeak = getLuminance(getBRDFEstimate(1.0, 0.25));
    float dampingFactor = texture(dampingTex, fTexCoord)[0];

    for (int k = 0; k < CAMERA_POSE_COUNT; k++)
    {
        vec4 imgColor = getLinearColor(k);
        vec3 lightDisplacement = objectToFitting * getLightVector(k, position);
        vec3 light = normalize(lightDisplacement);
        vec3 view = objectToFitting * normalize(getViewVector(k, position));
        vec3 halfway = normalize(light + view);
        float nDotH = max(0.0, halfway.z);
        float nDotL = max(0.0, light.z);
        float nDotV = max(0.0, view.z);
        float triangleNDotV = max(0.0, dot(objectToFitting * triangleNormal, view));

        if (imgColor.a > 0.0 && nDotH > COSINE_CUTOFF && nDotL > COSINE_CUTOFF && nDotV > COSINE_CUTOFF && triangleNDotV > COSINE_CUTOFF)
        {
            float hDotV = max(0.0, dot(halfway, view));

            // "Light intensity" is defined in such a way that we need to multiply by pi to be properly normalized.
            vec3 incidentRadiance = PI * getLightIntensity(k) / dot(lightDisplacement, lightDisplacement);

            float roughness = texture(roughnessMap, fTexCoord)[0];
            float maskingShadowing = geom(roughness, nDotH, nDotV, nDotL, hDotV);

            vec3 actualReflectanceTimesNDotL = imgColor.rgb / incidentRadiance;
            vec3 reflectanceEstimate = getBRDFEstimate(nDotH, maskingShadowing / (4 * nDotL * nDotV));

            // n dot l is already incorporated by virtue of the fact that radiance is being optimized, not reflectance.
            float weight = triangleNDotV * sqrt(max(0, 1 - nDotH * nDotH));

#if USE_LEVENBERG_MARQUARDT
            mat3x2 mfdGradient = outerProduct(halfway.xy, getMFDGradient(nDotH)); // (d NdotH / dN) * (dD / d NdotH)
            mat3x2 specularGradient;

#if SMITH_MASKING_SHADOWING
            vec3 mfdEstimate = getMFDEstimate(nDotH); // Also includes Fresnel
            vec2 geomGradient = getHeightCorrelatedSmithGradient(roughness, light, view);
            mat3x2 mfdGeomGradient = maskingShadowing * mfdGradient + outerProduct(geomGradient, mfdEstimate); // product rule
            specularGradient = 0.25 * (nDotV * mfdGeomGradient - outerProduct(view.xy, mfdEstimate * maskingShadowing)) / (nDotV * nDotV); // quotient rule
#else
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
#endif

            mat3x2 diffuseGradient = outerProduct(light.xy, getDiffuseEstimate() / PI);

            // fullGradient is essentially a portion of A-transpose
            // The columns of fullGradient are R/G/B.
            // The rows correspond to the components of N.
            mat3x2 fullGradient = diffuseGradient + specularGradient;

            mJTJ += weight * (fullGradient * transpose(fullGradient) + mat2(dampingFactor));
            vJTb += weight * fullGradient * (actualReflectanceTimesNDotL - reflectanceEstimate * nDotL);
#else

            mATA += weight * dot(reflectanceEstimate, reflectanceEstimate) * outerProduct(light, light);
            vATb += weight * dot(reflectanceEstimate, actualReflectanceTimesNDotL) * light;
#endif
        }
    }

#if USE_LEVENBERG_MARQUARDT

    vec2 normalFittingSpace;
    float prevError = calculateError(position, triangleNormal, tangentToObject * prevNormalTS);

    if (determinant(mJTJ) > 0)
    {
        normalFittingSpace = inverse(mJTJ) * vJTb;

        float tangentLengthSq = dot(normalFittingSpace, normalFittingSpace);
        float maxTangentLength = 0.5;
        float tangentScale = min(1.0, maxTangentLength / sqrt(tangentLengthSq));
        normalFittingSpace = normalFittingSpace * tangentScale;

        vec3 newNormalTS = fittingToTangent * vec3(normalFittingSpace, sqrt(max(0, 1 - tangentLengthSq * tangentScale)));

        float newError = calculateError(position, triangleNormal, tangentToObject * newNormalTS);

        if (!isnan(newNormalTS.x) && !isnan(newNormalTS.y) && !isnan(newNormalTS.z) && newError < prevError)
        {
            // Map to the correct range for a texture.
            normalTS = vec4(newNormalTS * 0.5 + vec3(0.5), 1.0);
            dampingOut = vec4(vec3(max(MIN_DAMPING, dampingFactor / 2.0)), 1.0);
        }
        else
        {
            normalTS = vec4(prevNormalTS * 0.5 + vec3(0.5), 1.0);
            dampingOut = vec4(vec3(dampingFactor * 2.0), 1.0);
        }
    }
    else
    {
        normalTS = vec4(prevNormalTS * 0.5 + vec3(0.5), 1.0);
        dampingOut = vec4(vec3(dampingFactor * 2.0), 1.0);
    }

#else
    vec3 normalFittingSpace;

    if (determinant(mATA) > 0)
    {
        normalFittingSpace = inverse(mATA) * vATb;

        if (length(normalFittingSpace) > 0)
        {
            normalTS = vec4(fittingToTangent * normalize(normalFittingSpace) * 0.5 + vec3(0.5), 1.0);
        }
        else
        {
            normalTS = vec4(prevNormalTS * 0.5 + vec3(0.5), 1.0);
        }
    }
    else
    {
        normalTS = vec4(prevNormalTS * 0.5 + vec3(0.5), 1.0);
    }
#endif
}
