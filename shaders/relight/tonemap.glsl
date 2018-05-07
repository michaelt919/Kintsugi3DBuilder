#ifndef TONEMAP_GLSL
#define TONEMAP_GLSL

#include "../colorappearance/linearize.glsl"

#line 7 3004

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
    vec4 tonemappedColor;

#if INVERSE_LUMINANCE_MAP_ENABLED
    if (color.r <= 0.000001 && color.g <= 0.000001 && color.b <= 0.000001)
    {
        tonemappedColor = vec4(0.0, 0.0, 0.0, 1.0);
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
            tonemappedColor = vec4(linearToSRGB(color / maxLuminance), alpha);
#else
            tonemappedColor = vec4(pow(color / maxLuminance, vec3(1.0 / renderGamma)), alpha);
#endif
        }
        else
        {
            float scaledLuminance = min(1.0, luminance / maxLuminance);
             // Step 2: determine the ratio between the tonemapped and linear luminance
            // Remove implicit gamma correction from the lookup table
            float tonemappedGammaCorrected = texture(inverseLuminanceMap, scaledLuminance).r;
            float tonemappedNoGamma = sRGBToLinear(vec3(tonemappedGammaCorrected))[0];
            float scale = tonemappedNoGamma / luminance;
             // Step 3: return the color, scaled to have the correct luminance,
            // but the original saturation and hue.
            // Step 4: apply gamma correction
            vec3 colorScaled = color * scale;
#if SRGB_TONEMAPPING_ENABLED
            tonemappedColor = vec4(linearToSRGB(colorScaled), alpha);
#else
            tonemappedColor = vec4(pow(colorScaled, vec3(1.0 / renderGamma)), alpha);
#endif
        }
    }
#elif SRGB_TONEMAPPING_ENABLED
       tonemappedColor = vec4(linearToSRGB(color), alpha);
#else
       tonemappedColor = vec4(pow(color / getMaxLuminance(), vec3(1.0 / renderGamma)), alpha);
#endif

    return tonemappedColor;
}

#endif // TONEMAP_GLSL
