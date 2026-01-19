#version 330

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

#include <colorappearance/linearize.glsl>
#line 17 0

#ifndef OCCLUSION_TEXTURE_ENABLED
#define OCCLUSION_TEXTURE_ENABLED 0
#endif

#ifndef CONSTANT_TEXTURE_ENABLED
#define CONSTANT_TEXTURE_ENABLED 0
#endif

in vec2 fTexCoord;

#if OCCLUSION_TEXTURE_ENABLED
uniform sampler2D occlusionTexture; // pass-through occlusion
#endif

uniform sampler2D diffuseEstimate;
uniform sampler2D roughnessEstimate;
uniform sampler2D specularEstimate;

#if CONSTANT_TEXTURE_ENABLED
uniform sampler2D constantTexture; // pass-through occlusion // TODO figure this out after implementing Godot shader
#endif

layout(location = 0) out vec4 totalAlbedoOut;
layout(location = 1) out vec4 ormOut;

void main()
{
    float sqrtRoughness = texture(roughnessEstimate, fTexCoord)[0];

    vec3 diffuseGamma = texture(diffuseEstimate, fTexCoord).rgb;
    vec3 specularGamma = texture(specularEstimate, fTexCoord).rgb;
    vec3 diffuseLinearRaw = sRGBToLinear(diffuseGamma);
    vec3 specularLinearRaw = sRGBToLinear(specularGamma);

    // Enforse a minimum specular value of 0.04 otherwise weird things will happen
    // Typically diffuse and specular are conflated (leading to <0.04 specular) when a surface is extremely rough
    // In that scenario, for backscattering observations (i.e. flash-on-camera) specular reflectance is effectively
    // one quarter of what diffuse reflectance would be for the same albedo color.
    vec3 diffuseLinear = max(vec3(0.0), diffuseLinearRaw - max(vec3(0.0), 0.01 - 0.25 * specularLinearRaw));
    vec3 specularLinear = specularLinearRaw + 4 * diffuseLinearRaw - 4 * diffuseLinear;

    // Total reflectivity
    vec3 diffusePlusSpecular = diffuseLinear + specularLinear;

    float diffusePlusSpecularLuminance = getLuminance(diffusePlusSpecular);
    float diffuseLuminance = getLuminance(diffuseLinear);


//    d = (1 - m) * a
//    d / a = 1 - m
//    m = 1 - d/a
//
//    s = 0.04 * (1 - m) + m * a
//    s = 0.04 d/a + (1 - d/a) * a
//    s = 0.04 d/a + a - d
//    s*a = 0.04d + a^2 - d*a
//    a^2 - (d + s) * a + 0.04*d = 0
//
//    a = 1/2 * (d + s +/- sqrt((d + s)^2 - 4 * 0.04*d))
//      = 0.5 * (d + s) +/- 0.5 * sqrt((d + s)^2 - 0.16*d)

    // Adjust for dielectic specular (i.e. 0.04 for non-metallic)
    // See derivation above
    // Initially, calculate albedo assuming metallicity could be different for each RGB channel (for simplicity)
    // Use greater answer from quadratic formula when diffuse + specular > 0.08 and lower answer when diffuse + specular < 0.08
    // (need to use lower answer when diffuse + specular < 0.08 to avoid having dark materials come out as metallic)
    // When diffuse + specular = 0.08, the square root should come out to zero so this is still continuous.
    float albedoLuminance = 0.5 * diffusePlusSpecularLuminance
        + 0.5 * sign (diffusePlusSpecularLuminance - 0.08) * sqrt(max(0.0, diffusePlusSpecularLuminance * diffusePlusSpecularLuminance - 0.16 * diffuseLuminance));

    // Then calculate metallicity for all channels
    float metallicity = clamp(1.0 - diffuseLuminance / albedoLuminance, 0.0, 1.0);

    float m = sqrtRoughness * sqrtRoughness;
    float mSq = m * m;

    // TODO expose as parameter in UI (maybe as a new task after processing textures?)
    float heuristicWeight = 0.125;

    // Using off-specular reflectance as target for RGB hue: 0.25 * m^2 * S
    // heuristic is a mix of off-specular reflectance (m^2) and mid-to-peak specular (1.0) depending on roughness
    // (division by PI ignored as consistent with other calculations)
    float specularHeuristic = mix(0.25 * mSq, 1.0, heuristicWeight);

    vec3 rgbTarget = diffuseLinear + specularHeuristic * specularLinear;
    rgbTarget /= max(getLuminance(rgbTarget), 0.00001);


    // D + h * S
    // = A * (1 - M) + h * (0.04 * (1 - M) + A * M)
    // = A * (1 - M) + A * h * M + h * 0.04 * (1 - M)
    // = A * mix(1, h, M) + 0.04 * h * (1 - M)

    float dielectricHeuristic = 0.04 * specularHeuristic * (1 - metallicity) / mix(1, specularHeuristic, metallicity);

    // Incorporate RGB, accounting for dielectric specular contribution to the final color.
    vec3 albedoLinear = (albedoLuminance + dielectricHeuristic) * rgbTarget - dielectricHeuristic;

    // Make sure it isn't clipping.
    albedoLinear /= max(max(1.0, albedoLinear.r), max(albedoLinear.g, albedoLinear.b));

    // Adjust metallicity as needed (could end up less metallic if necessary to maintain same backscattering reflectance without albedo clipping)
    metallicity = clamp(1.0 - diffuseLuminance / getLuminance(albedoLinear), 0.0, 1.0);

    totalAlbedoOut = vec4(linearToSRGB(albedoLinear), 1.0);

#if OCCLUSION_TEXTURE_ENABLED
    float occlusion = texture(occlusionTexture, fTexCoord).r;
#else
    float occlusion = 1.0;
#endif

    ormOut = vec4(occlusion, sqrtRoughness, metallicity, 1.0);
}
