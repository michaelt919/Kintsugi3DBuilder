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

#ifndef ERRORCALC_GLSL
#define ERRORCALC_GLSL

#include "../colorappearance/colorappearance.glsl"

#line 19 2006

#define MAX_ERROR 1.0 //3.402822E38 // Max 32-bit floating-point is 3.4028235E38
#define MIN_DAMPING_FACTOR 0.0078125 // 1/256
#define MAX_DAMPING_FACTOR 1024 // 1048576 // 2^20
//#define MIN_SHIFT_FRACTION 0.01171875 // 3/256

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D specularEstimate;
uniform sampler2D roughnessEstimate;
uniform sampler2D errorTexture;

uniform bool ignoreDampingFactor;
uniform float fittingGamma;

vec4 getDiffuseColor()
{
    vec4 textureLookup = texture(diffuseEstimate, fTexCoord);
    return vec4(pow(textureLookup.rgb, vec3(gamma)), textureLookup.a);
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

vec3 getSpecularColor()
{
    vec3 specularColor = texture(specularEstimate, fTexCoord).rgb;
    return sign(specularColor) * pow(abs(specularColor), vec3(gamma));
}

vec4 getRoughness()
{
    return texture(roughnessEstimate, fTexCoord);
}

struct ErrorResult
{
    bool mask;
    float dampingFactor;
    float sumSqError;
};

ErrorResult calculateError()
{
    vec4 prevErrorResult = texture(errorTexture, fTexCoord);

    if (!ignoreDampingFactor && (prevErrorResult.x < MIN_DAMPING_FACTOR || prevErrorResult.x > MAX_DAMPING_FACTOR))
    {
        return ErrorResult(false, 0.0, prevErrorResult.y);
    }
    else
    {
        vec3 normal = normalize(fNormal);

        vec3 tangent = normalize(fTangent - dot(normal, fTangent));
        vec3 bitangent = normalize(fBitangent
            - dot(normal, fBitangent) * normal
            - dot(tangent, fBitangent) * tangent);

        mat3 tangentToObject = mat3(tangent, bitangent, normal);
        vec3 shadingNormal = tangentToObject * getDiffuseNormalVector();

        vec4 diffuseColorRGBA = getDiffuseColor();
        float alpha = diffuseColorRGBA.a;
        vec3 diffuseColor = rgbToXYZ(diffuseColorRGBA.rgb);
        vec3 specularColor = rgbToXYZ(getSpecularColor());
        vec4 roughness = getRoughness();
        vec3 roughnessSquared = roughness.xyz * roughness.xyz;
        float maxLuminance = getMaxLuminance();
        float fittingGammaInv = 1.0 / fittingGamma;

        float sumSqError = 0.0;
        float sumWeight = 0.0;

        if (!isnan(roughness.y) && roughness.w == 1.0)
        {
            for (int i = 0; i < VIEW_COUNT; i++)
            {
                vec3 view = normalize(getViewVector(i));
                float nDotV = max(0, dot(shadingNormal, view));
                vec4 color = getLinearColor(i);

                if (color.a > 0 && nDotV > 0 && dot(normal, view) > 0)
                {
                    LightInfo lightInfo = getLightInfo(i);
                    vec3 light = lightInfo.normalizedDirection;
                    float nDotL = max(0, dot(light, shadingNormal));

                    vec3 halfway = normalize(view + light);
                    float nDotH = dot(halfway, shadingNormal);

                    if (nDotL > 0.0 /*&& nDotH > sqrt(0.5)*/)
                    {
                        float nDotHSquared = nDotH * nDotH;

                        vec3 q1 = roughnessSquared + (1.0 - nDotHSquared) / nDotHSquared;
                        vec3 mfdEval = roughnessSquared / (nDotHSquared * nDotHSquared * q1 * q1);

                        float hDotV = max(0, dot(halfway, view));
                        float geomRatio = min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV) / (4 * nDotV);

                        vec3 colorScaled = pow(rgbToXYZ(color.rgb / lightInfo.attenuatedIntensity), vec3(fittingGammaInv));
                        vec3 currentFit = diffuseColor * nDotL + min(vec3(1), specularColor) * mfdEval * geomRatio;
                        vec3 colorResidual = colorScaled - pow(currentFit, vec3(fittingGammaInv));

                        float weight = nDotV; //clamp(1 / (1 - nDotHSquared), 0, 1000000);
                        sumSqError += weight * dot(colorResidual, colorResidual);
                        sumWeight += weight * 3;
                    }
                }
            }

            if (sumWeight > 0.0)
            {
                float meanSqError = min(sumSqError / sumWeight, MAX_ERROR);

                if (meanSqError < prevErrorResult.y)
                {
                    //return ErrorResult(true, prevErrorResult.x, sumSqError);
                    return ErrorResult(true, prevErrorResult.x / 2, meanSqError);
                }
            }
        }

        //return ErrorResult(false, prevErrorResult.x / 2, prevErrorResult.y);
        return ErrorResult(false, prevErrorResult.x * 2, prevErrorResult.y);
    }
}

#endif // ERRORCALC_GLSL
