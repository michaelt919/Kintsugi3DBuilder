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

#line 17 1200

#ifndef LUMINANCE_MAP_ENABLED
#define LUMINANCE_MAP_ENABLED 0
#endif

#if LUMINANCE_MAP_ENABLED
uniform sampler1D luminanceMap;
#endif

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
    float maxLuminance;

#if LUMINANCE_MAP_ENABLED
    maxLuminance = texture(luminanceMap, 1.0).r;
#else
    maxLuminance = 1.0;
#endif

    return maxLuminance;
}

float getMaxTonemappingScale()
{
    return 5.0;
}

vec3 linearizeColor(vec3 nonlinearColor)
{
    vec3 linearColor;

#if LUMINANCE_MAP_ENABLED
    if (nonlinearColor.r <= 0.0 && nonlinearColor.g <= 0.0 && nonlinearColor.b <= 0.0)
    {
        linearColor = vec3(0);
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
            linearColor = colorGamma * maxLuminance;
        }
        else
        {
            // Step 3: determine the ratio between the linear and nonlinear luminance
            // Reapply gamma correction to the single luminance value
            float scale = min(getMaxTonemappingScale() * maxLuminance,
                texture(luminanceMap, pow(luminanceNonlinear, 1.0 / gamma)).r / luminanceNonlinear);

            // Step 4: return the color, scaled to have the correct luminance,
            // but the original saturation and hue.
            linearColor = colorGamma * scale;
        }
    }
#else
    linearColor = pow(nonlinearColor, vec3(gamma));
#endif

    return linearColor;
}

vec4 linearizeColor(vec4 nonlinearColor)
{
    return vec4(linearizeColor(nonlinearColor.rgb), nonlinearColor.a);
}

vec3 linearToSRGB(vec3 color)
{
    vec3 sRGBColor;

    if(color.r <= 0.0031308)
    {
        sRGBColor.r = 12.92 * color.r;
    }
    else
    {
        sRGBColor.r = (1.055) * pow(color.r, 1.0/2.4) - 0.055;
    }

    if(color.g <= 0.0031308)
    {
        sRGBColor.g = 12.92 * color.g;
    }
    else
    {
        sRGBColor.g = (1.055) * pow(color.g, 1.0/2.4) - 0.055;
    }

    if(color.b <= 0.0031308)
    {
        sRGBColor.b = 12.92 * color.b;
    }
    else
    {
        sRGBColor.b = (1.055) * pow(color.b, 1.0/2.4) - 0.055;
    }

    return sRGBColor;
}

vec3 sRGBToLinear(vec3 sRGBColor)
{
     vec3 linearColor;

     if(sRGBColor.r <= 0.04045)
     {
         linearColor.r = sRGBColor.r / 12.92;
     }
     else
     {
         linearColor.r = pow((sRGBColor.r + 0.055) / 1.055, 2.4);
     }

     if(sRGBColor.g <= 0.04045)
     {
         linearColor.g = sRGBColor.g / 12.92;
     }
     else
     {
         linearColor.g = pow((sRGBColor.g + 0.055) / 1.055, 2.4);
     }

     if(sRGBColor.b <= 0.04045)
     {
         linearColor.b = sRGBColor.b / 12.92;
     }
     else
     {
         linearColor.b = pow((sRGBColor.b + 0.055) / 1.055, 2.4);
     }

     return linearColor;
}

vec3 xyzToLab(vec3 xyzColor)
{
    // Assuming illuminant D65
    // https://en.wikipedia.org/wiki/CIELAB_color_space
    vec3 rescaledXYZColor = xyzColor / vec3(0.95047, 1.0, 1.08883);
    return mat3(vec3(0, 5, 0), vec3(1.16, -5, 2), vec3(0, 0, -2))
        * mix(rescaledXYZColor * (841.0 / 108.0) + (4.0 / 29.0), pow(rescaledXYZColor, vec3(1.0 / 3.0)),
                max(vec3(0.0), sign(rescaledXYZColor - (216.0 / 24389.0))))
        - vec3(0.16, 0, 0);
}

vec3 labToXYZ(vec3 labColor)
{
    // Assuming illuminant D65
    // https://en.wikipedia.org/wiki/CIELAB_color_space
    vec3 transformedColor = mat3(vec3(1.0 / 1.16), vec3(0.2, 0, 0), vec3(0, 0, -0.5)) * labColor + vec3(0.16 / 1.16);
    return vec3(0.95047, 1.0, 1.08883)
        * mix((108.0 / 841.0) * (transformedColor - (4.0 / 29.0)), transformedColor * transformedColor * transformedColor,
            max(vec3(0.0), sign(transformedColor - (6.0 / 29.0))));
}

vec3 rgbToYUV(vec3 rgbColor)
{
    return mat3(
        vec3(0.299, -0.14713, 0.615),
        vec3(0.587, -0.28886, -0.51499),
        vec3(0.114, 0.436, -0.10001)) * rgbColor;
}

vec3 yuvToRGB(vec3 yuvColor)
{
    return mat3(
        vec3(1.0),
        vec3(0.0, -0.39465, 2.03211),
        vec3(1.13983, -0.58060, 0.0)) * yuvColor;
}

#endif // LINEARIZE_GLSL