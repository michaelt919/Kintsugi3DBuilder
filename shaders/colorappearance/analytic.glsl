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
 
#ifndef COLOR_APPEARANCE_ANALYTIC_GLSL
#define COLOR_APPEARANCE_ANALYTIC_GLSL

#include "material.glsl"
#include "../common/constructTBN.glsl"
#include "colorappearance.glsl"
#include "reflectanceequations.glsl"

#line 20 1109

#ifndef ANALYTIC_BUMP_HEIGHT
#define ANALYTIC_BUMP_HEIGHT 1.0
#endif

#ifndef ANALYTIC_UV_SCALE
#define ANALYTIC_UV_SCALE 1.0
#endif

vec4 getColor(int index)
{
    Material m = getMaterial();

    mat3 tbn = constructTBNExact();

    vec3 normal = tbn[2];
    vec3 view = normalize(getViewVector(index));
    float nDotV = max(0, dot(normal, view));

    if (nDotV > 0)
    {
        vec2 scaledTexCoord = ANALYTIC_UV_SCALE * fTexCoord;

        vec3 shadingNormal = normalize(tbn * (getTangentSpaceNormal(scaledTexCoord - floor(scaledTexCoord))
            * vec3(ANALYTIC_BUMP_HEIGHT, ANALYTIC_BUMP_HEIGHT, 1.0)));

        LightInfo lightInfo = getLightInfo(index);
        vec3 light = lightInfo.normalizedDirection;
        float nDotL = max(0, dot(light, shadingNormal));
        nDotV = max(0, dot(view, shadingNormal));

        vec3 halfway = normalize(view + light);
        float nDotH = dot(halfway, shadingNormal);

        if (nDotV > 0.0 && nDotH > 0.0)
        {
            float hDotV = max(0, dot(halfway, view));

            return vec4(pow((m.diffuseColor * nDotL
                        + fresnel(m.specularColor, vec3(1.0), hDotV) * distTimesPi(nDotH, vec3(m.roughness))
                            * geom(m.roughness, nDotH, nDotV, nDotL, hDotV) / (4 * nDotV))
                    * lightInfo.attenuatedIntensity,
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

#endif // COLOR_APPEARANCE_ANALYTIC_GLSL
