#ifndef TONEMAP_GLSL
#define TONEMAP_GLSL

#include "../colorappearance/linearize.glsl"

#line 7 3004

uniform float renderGamma;
uniform bool useInverseLuminanceMap;
uniform sampler1D inverseLuminanceMap;

vec4 tonemap(vec3 color, float alpha)
{
//     if (useInverseLuminanceMap)
//     {
//         if (color.r <= 0.000001 && color.g <= 0.000001 && color.b <= 0.000001)
//         {
//             return vec4(0.0, 0.0, 0.0, 1.0);
//         }
//         else
//         {
//             // Step 1: convert to CIE luminance
//             // Clamp to 1 so that the ratio computed in step 3 is well defined
//             // if the luminance value somehow exceeds 1.0
//             float luminance = getLuminance(color);
//             float maxLuminance = getMaxLuminance();
//             if (luminance >= maxLuminance)
//             {
//                 return vec4(linearToSRGB(color / maxLuminance), alpha);
//             }
//             else
//             {
//                 float scaledLuminance = min(1.0, luminance / maxLuminance);
//
//                 // Step 2: determine the ratio between the tonemapped and linear luminance
//                 // Remove implicit gamma correction from the lookup table
//                 float tonemappedGammaCorrected = texture(inverseLuminanceMap, scaledLuminance).r;
//                 float tonemappedNoGamma = sRGBToLinear(vec3(tonemappedGammaCorrected))[0];
//                 float scale = tonemappedNoGamma / luminance;
//
//                 // Step 3: return the color, scaled to have the correct luminance,
//                 // but the original saturation and hue.
//                 // Step 4: apply gamma correction
//                 vec3 colorScaled = color * scale;
//                 return vec4(linearToSRGB(colorScaled), alpha);
//             }
//         }
//     }
//     else
//    {
//        return vec4(linearToSRGB(color), alpha);
        return vec4(pow(color / getMaxLuminance(), vec3(1.0 / renderGamma)), alpha);
//    }
}

#endif // TONEMAP_GLSL
