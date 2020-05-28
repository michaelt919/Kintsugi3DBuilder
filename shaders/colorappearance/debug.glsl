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

#ifndef COLOR_APPEARANCE_DEBUG_GLSL
#define COLOR_APPEARANCE_DEBUG_GLSL

#include "colorappearance.glsl"

#line 7 1109

//vec3 getSpecularColor();
//vec3 getRoughness();

#define DIFFUSE_COLOR vec3(0)
#define ROUGHNESS_SQUARED (sqrt(rgbToXYZ(fTexCoord.yxy)) / 2)
#define SPECULAR_COLOR (ROUGHNESS_SQUARED * 0.5)
//#define SPECULAR_COLOR (ROUGHNESS_SQUARED * rgbToXYZ(getSpecularColor()) / pow(getRoughness(), vec3(2)))

vec4 getColor(int index)
{
    vec3 normal = normalize(fNormal);
    vec3 view = normalize(getViewVector(index));
    float nDotV = max(0, dot(normal, view));

    vec3 tangent = normalize(fTangent - dot(normal, fTangent));
    vec3 bitangent = normalize(fBitangent
        - dot(normal, fBitangent) * normal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, normal);
    vec3 shadingNormal =
        fNormal;
        //tangentToObject * normalize(vec3(0.5 * cos(8 * 3.14 * fTexCoord), 1.0));

    if (nDotV > 0)
    {
        vec3 lightPreNormalized = getLightVector(index);
        vec3 attenuatedLightIntensity = infiniteLightSources ?
            getLightIntensity(index) :
            getLightIntensity(index) / (dot(lightPreNormalized, lightPreNormalized));
        vec3 light = normalize(lightPreNormalized);
        float nDotL = max(0, dot(light, shadingNormal));
        nDotV = max(0, dot(view, shadingNormal));

        vec3 halfway = normalize(view + light);
        float nDotH = dot(halfway, shadingNormal);

        if (nDotV > 0.0 && nDotH > 0.0)
        {
            float nDotHSquared = nDotH * nDotH;

            // float mfdEval = exp((nDotHSquared - 1.0) / (nDotHSquared * ROUGHNESS_SQUARED))
                // / (ROUGHNESS_SQUARED * nDotHSquared * nDotHSquared);

            vec3 q = ROUGHNESS_SQUARED + (1 - nDotHSquared) / nDotHSquared;
            vec3 mfdEval = ROUGHNESS_SQUARED / (nDotHSquared * nDotHSquared * q * q);

            float hDotV = max(0, dot(halfway, view));

            return vec4(pow(DIFFUSE_COLOR * nDotL + xyzToRGB(SPECULAR_COLOR * mfdEval) / (4 * nDotV)
                    * min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV)
                    * attenuatedLightIntensity / getMaxLuminance(),
                vec3(1.0 / gamma)), 1.0);
        }
        else
        {
            return vec4(0.0);
        }
    }
    else
    {
        return vec4(0.0);
    }
}

#endif // COLOR_APPEARANCE_DEBUG_GLSL
