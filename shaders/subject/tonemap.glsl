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

#ifndef TONEMAP_GLSL
#define TONEMAP_GLSL

#include "../colorappearance/linearize.glsl"

#line 19 3004

#ifndef INVERSE_LUMINANCE_MAP_ENABLED
#define INVERSE_LUMINANCE_MAP_ENABLED 0
#endif

#ifndef SRGB_TONEMAPPING_ENABLED
#define SRGB_TONEMAPPING_ENABLED 0
#endif

uniform float renderGamma;

#if INVERSE_LUMINANCE_MAP_ENABLED
uniform sampler1D inverseLuminanceMap;
#endif

vec4 tonemap(vec3 color, float alpha)
{
    // return tonemapFromLuminanceMap(color, alpha);
    return vec4(pow(color / getMaxLuminance(), vec3(1.0 / renderGamma)), alpha);
}

vec3 tonemapFromLuminanceMap(vec3 color)
{
    vec3 tonemappedColor;

#if INVERSE_LUMINANCE_MAP_ENABLED
    if (color.r <= 0.000001 && color.g <= 0.000001 && color.b <= 0.000001)
    {
        tonemappedColor = vec3(0.0);
    }
    else
    {
        // Step 1: convert to CIE luminance
        // Clamp to 1 so that the ratio computed in step 3 is well defined
        // if the luminance value somehow exceeds 1.0
        float luminance = getLuminance(color);
        float maxLuminance = getMaxLuminance();
        if (luminance >= maxLuminance)
        {
#if SRGB_TONEMAPPING_ENABLED
            tonemappedColor = linearToSRGB(color / maxLuminance);
#else
            tonemappedColor = pow(color / maxLuminance, vec3(1.0 / renderGamma));
#endif
        }
        else
        {
            float scaledLuminance = min(1.0, luminance / maxLuminance);
            // Step 2: determine the ratio between the tonemapped and linear luminance
            // Remove implicit gamma correction from the lookup table
            float tonemappedGammaCorrected = texture(inverseLuminanceMap, scaledLuminance).r;
            float pseudoLuminance = sRGBToLinear(vec3(tonemappedGammaCorrected))[0];
            float scale = pseudoLuminance / luminance;
            // Step 3: return the color, scaled to have the correct luminance,
            // but the original saturation and hue.
            // Step 4: apply gamma correction
            vec3 pseudoLinear = color * scale;
#if SRGB_TONEMAPPING_ENABLED
            tonemappedColor = linearToSRGB(pseudoLinear);
#else
            tonemappedColor = pow(pseudoLinear, vec3(1.0 / renderGamma));
#endif
        }
    }
#elif SRGB_TONEMAPPING_ENABLED
       tonemappedColor = linearToSRGB(color);
#else
       tonemappedColor = pow(color / getMaxLuminance(), vec3(1.0 / renderGamma));
#endif

    return tonemappedColor;
}

vec4 tonemapFromLuminanceMap(vec3 color, float alpha)
{
    return vec4(tonemapFromLuminanceMap(color), alpha);
}

#endif // TONEMAP_GLSL
