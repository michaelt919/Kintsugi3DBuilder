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

#ifndef SRGB_ENCODING_ENABLED
#define SRGB_ENCODING_ENABLED 1
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
    // Step 1: convert to CIE luminance
    float luminance = getLuminance(color);
    float maxLuminance = getMaxLuminance();
    if (luminance >= maxLuminance)
    {
#if SRGB_ENCODING_ENABLED
        tonemappedColor = linearToSRGB(color / maxLuminance);
#else
        tonemappedColor = pow(color / maxLuminance, vec3(1.0 / renderGamma));
#endif
    }
    else
    {
        float delta = 0.00001;
        float clampedLuminance = max(luminance, delta);

        // Clamp to 1 so that the ratio computed in step 3 is well defined
        // if the luminance value somehow exceeds 1.0
        float scaledLuminance = min(1.0, clampedLuminance / maxLuminance);

        // Step 2: determine the ratio between the tonemapped and linear luminance
        // Remove implicit gamma correction from the lookup table
        int inverseLuminanceMapSize = textureSize(inverseLuminanceMap, 0);
        float texCoord = (0.5 + scaledLuminance * (inverseLuminanceMapSize - 1)) / inverseLuminanceMapSize; // adjust for how linear interpolation is performed
        float tonemappedGammaCorrected = texture(inverseLuminanceMap, texCoord).r;
#if SRGB_ENCODING_ENABLED
        float pseudoLuminance = sRGBToLinear(vec3(tonemappedGammaCorrected))[0];
#else
        float pseudoLuminance = pow(tonemappedGammaCorrected, renderGamma);
#endif

        // Step 3: return the color, scaled to have the correct luminance,
        // but the original saturation and hue.
        float scale = pseudoLuminance / clampedLuminance * clamp(luminance / delta, 0, 1); // ensure seamlessness if luminance < delta
        vec3 pseudoLinear = color * scale;

        // Step 4: apply gamma correction
#if SRGB_ENCODING_ENABLED
        tonemappedColor = linearToSRGB(pseudoLinear);
#else
        tonemappedColor = pow(pseudoLinear, vec3(1.0 / renderGamma));
#endif
    }
#elif SRGB_ENCODING_ENABLED
    tonemappedColor = linearToSRGB(color);
#else
    tonemappedColor = pow(color, vec3(1.0 / renderGamma));
#endif

    return tonemappedColor;
}

vec4 tonemapFromLuminanceMap(vec3 color, float alpha)
{
    return vec4(tonemapFromLuminanceMap(color), alpha);
}

#endif // TONEMAP_GLSL
