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

#ifndef RESID_GLSL
#define RESID_GLSL

#include "reflectanceequations.glsl"

#line 19 2101

uniform sampler2D diffuseMap;
uniform sampler2D specularMap;
uniform sampler2D roughnessMap;

uniform mat4 model_view;
uniform bool diffuseMode;

#define MAX_SQRT_ROUGHNESS 1.0

vec3 getDiffuseColor(vec2 texCoord)
{
    return pow(texture(diffuseMap, texCoord).rgb, vec3(gamma));
}

vec3 getSpecularColor(vec2 texCoord)
{
    vec3 specularColor = texture(specularMap, texCoord).rgb;
    return sign(specularColor) * pow(abs(specularColor), vec3(gamma));
}

vec3 getSqrtRoughness(vec2 texCoord)
{
    vec3 roughnessLookup = texture(roughnessMap, texCoord).rgb;
    return vec3(
            roughnessLookup.g + roughnessLookup.r - 16.0 / 31.0,
            roughnessLookup.g,
            roughnessLookup.g + roughnessLookup.b - 16.0 / 31.0);
}

vec4 computeResidual(vec2 texCoord, vec3 shadingNormal)
{
    vec3 diffuseColor = getDiffuseColor(texCoord);
    vec3 specularColor = getSpecularColor(texCoord);
    vec3 sqrtRoughness = getSqrtRoughness(texCoord);
    vec3 roughnessRGB = sqrtRoughness * sqrtRoughness;
    vec3 roughnessSquaredRGB = roughnessRGB * roughnessRGB;
    float roughnessSquared = getLuminance(specularColor) / getLuminance(specularColor / roughnessSquaredRGB);
    float roughness = sqrt(roughnessSquared);
    float maxLuminance = getMaxLuminance();
    
    vec3 view = normalize(getViewVector());

    LightInfo lightInfo = getLightInfo();
    vec3 light = lightInfo.normalizedDirection;

    float nDotV = max(0, dot(shadingNormal, view));
    float nDotL = max(0, dot(shadingNormal, light));
    vec4 color = getLinearColor();

    if (nDotV > 0)
    {
        if (nDotL > 0.0)
        {
            vec3 halfway = normalize(view + light);
            float nDotH = max(0, dot(halfway, shadingNormal));
            float nDotHSquared = nDotH * nDotH;
            float hDotV = max(0, dot(halfway, view));

            vec3 colorScaled = color.rgb / lightInfo.attenuatedIntensity;
            vec3 diffuseContrib = diffuseColor * nDotL;

            float maskingShadowing = computeGeometricAttenuationHeightCorrelatedSmith(roughness, nDotV, nDotL);
//            float invGeomRatio = 4 * nDotV / maskingShadowing;
            vec3 specularTerm = max(vec3(0.0), (colorScaled - diffuseContrib));

            vec3 sqrtDenominator = (roughnessSquaredRGB - 1) * nDotH * nDotH + 1;

            vec3 specularRescaled;

            if (color.a > 0)
            {
    //            return vec4(
    //                        pow(
    //                            clamp(mfdFresnel * roughnessSquared / specularColor, 0, 1)
    //                        , vec3(1.0 / 2.2))
    //                        - pow(
    //                            diffuseMode ?
    //                                clamp(nDotL * roughnessSquared * invGeomRatio, 0, 1)
    //                                : clamp(roughnessSquared * roughnessSquared / (sqrtDenominator * sqrtDenominator), 0, 1),
    //                        vec3(1.0 / 2.2))
    //                    , nDotV);

                specularRescaled = 4 * specularTerm / specularColor;
            }
            else
            {
                specularRescaled = roughnessSquaredRGB / (sqrtDenominator * sqrtDenominator) * maskingShadowing / nDotV;
            }

            return clamp(vec4(roughnessSquaredRGB * specularRescaled - 0.5, 1.0), -1, 1);
        }
        else
        {
            return vec4(-0.5, -0.5, -0.5, 1.0);
        }
    }
    else
    {
        return vec4(-0.5, -0.5, -0.5, 1.0);
    }
}

#endif // RESID_GLSL
