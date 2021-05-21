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

#ifndef LINEARIZE_GLSL
#define LINEARIZE_GLSL

#line 5 1200

uniform sampler1D luminanceMap;
uniform bool useLuminanceMap;

uniform float gamma;

float getLuminance(vec3 rgbColor)
{
    // linearized sRGB to CIE-Y
    return dot(rgbColor, vec3(0.2126729, 0.7151522, 0.0721750));
}

vec3 rgbToXYZ(vec3 rgbColor)
{
    return mat3(vec3(0.4124564, 0.2126729, 0.0193339),
        vec3(0.3575761, 0.7151522, 0.1191920),
        vec3(0.1804375, 0.0721750, 0.9503041)) * rgbColor;
}

vec3 xyzToRGB(vec3 xyzColor)
{
    return mat3(vec3(3.2404542, -0.9692660, 0.0556434),
        vec3(-1.5371385, 1.8760108, -0.2040259),
        vec3(-0.4985314, 0.0415560, 1.0572252)) * xyzColor;
}

float getMaxLuminance()
{
    if (useLuminanceMap)
    {
        return texture(luminanceMap, 1.0).r;
    }
    else
    {
        return 1.0;
    }
}

float getMaxTonemappingScale()
{
    return 5.0;
}

vec3 linearizeColor(vec3 nonlinearColor)
{
    if (useLuminanceMap)
    {
        if (nonlinearColor.r <= 0.0 && nonlinearColor.g <= 0.0 && nonlinearColor.b <= 0.0)
        {
            return vec3(0);
        }
        else
        {
            // Step 1: remove gamma correction
            vec3 colorGamma = pow(nonlinearColor, vec3(gamma));
            
            // Step 2: convert to CIE luminance
            // Clamp to 1 so that the ratio computed in step 3 is well defined
            // if the luminance value somehow exceeds 1.0
            float luminanceNonlinear = getLuminance(colorGamma);

            float maxLuminance = getMaxLuminance();

            if (luminanceNonlinear > 1.0)
            {
                return colorGamma * maxLuminance;
            }
            else
            {
                // Step 3: determine the ratio between the linear and nonlinear luminance
                // Reapply gamma correction to the single luminance value
                float scale = min(getMaxTonemappingScale() * maxLuminance,
                    texture(luminanceMap, pow(luminanceNonlinear, 1.0 / gamma)).r / luminanceNonlinear);

                // Step 4: return the color, scaled to have the correct luminance,
                // but the original saturation and hue.
                return colorGamma * scale;
            }
        }
    }
    else
    {
        return pow(nonlinearColor, vec3(gamma));
    }
}

vec4 linearizeColor(vec4 nonlinearColor)
{
    return vec4(linearizeColor(nonlinearColor.rgb), nonlinearColor.a);
}

#endif // LINEARIZE_GLSL