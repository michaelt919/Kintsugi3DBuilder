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

#ifndef REFLECTANCEEQUATIONS_GLSL
#define REFLECTANCEEQUATIONS_GLSL

#line 5 3001

#ifndef PHYSICALLY_BASED_MASKING_SHADOWING
#define PHYSICALLY_BASED_MASKING_SHADOWING 0
#endif

#ifndef SMITH_MASKING_SHADOWING
#define SMITH_MASKING_SHADOWING 0
#endif

vec3 computeFresnelReflectivityActual(vec3 specularColor, vec3 grazingColor, float hDotV)
{
    float maxLuminance = dot(grazingColor, vec3(0.2126, 0.7152, 0.0722));
    float f0 = clamp(dot(specularColor, vec3(0.2126, 0.7152, 0.0722)) / maxLuminance, 0.001, 0.999);
    float sqrtF0 = sqrt(f0);
    float ior = (1 + sqrtF0) / (1 - sqrtF0);
    float g = sqrt(ior*ior + hDotV * hDotV - 1);
    float fresnel = 0.5 * pow(g - hDotV, 2) / pow(g + hDotV, 2)
        * (1 + pow(hDotV * (g + hDotV) - 1, 2) / pow(hDotV * (g - hDotV) + 1, 2));

    return specularColor + (grazingColor - specularColor) * max(0, fresnel - f0) / (1.0 - f0);
}

vec3 computeFresnelReflectivitySchlick(vec3 specularColor, vec3 grazingColor, float hDotV)
{
    return max(specularColor,
        specularColor + (grazingColor - specularColor) * pow(max(0.0, 1.0 - hDotV), 5.0));
}

vec3 fresnel(vec3 specularColor, vec3 grazingColor, float hDotV)
{
    //return specularColor;
    //return computeFresnelReflectivityActual(specularColor, grazingColor, hDotV);
    return computeFresnelReflectivitySchlick(specularColor, grazingColor, hDotV);
}

float computeGeometricAttenuationVCavity(float nDotH, float nDotV, float nDotL, float hDotV)
{
    return min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV);
}

float computeLambdaBeckmann(float roughness, float cosine)
{
    float a = min(1.6, cosine / (roughness * sqrt(1.0 - cosine * cosine)));
    float aSq = a * a;

    return max(1.0, (1.0 + 2.276 * a + 2.577 * aSq) / (3.535 * a + 2.181 * aSq) - 1.0);
    // ^ See Walter et al. "Microfacet Models for Refraction through Rough Surfaces"
    // for this formula (and use G1 = 1 / (1 + Lambda) )
}

float computeLambdaGGX(float roughness, float cosine)
{
     return -0.5 + 0.5 * sqrt(1 + roughness * roughness * (1 / (cosine * cosine) - 1.0));
    // ^ See Walter et al. "Microfacet Models for Refraction through Rough Surfaces"
    // for this formula (and use G1 = 1 / (1 + Lambda) )
}

float lambda(float roughness, float cosine)
{
    //return 1.0 / cosine - 1.0;
    //return computeLambdaBeckmann(roughness, cosine);
    return computeLambdaGGX(roughness, cosine);
}

float computeGeometricAttenuationSeparableSmith(float roughness, float nDotV, float nDotL)
{
    return 1 / ((1 + lambda(roughness, nDotV)) * (1 + lambda(roughness, nDotL)));
}

float computeGeometricAttenuationHeightCorrelatedSmith(float roughness, float nDotV, float nDotL)
{
    return 1 / (1 + lambda(roughness, nDotV) + lambda(roughness, nDotL));
}

float geom(float roughness, float nDotH, float nDotV, float nDotL, float hDotV)
{
    float result;
#if !PHYSICALLY_BASED_MASKING_SHADOWING
    result = nDotL * nDotV;
#elif SMITH_MASKING_SHADOWING
    //result = computeGeometricAttenuationSeparableSmith(roughness, nDotV, nDotL);
    result = computeGeometricAttenuationHeightCorrelatedSmith(roughness, nDotV, nDotL);
#else
    result = computeGeometricAttenuationVCavity(nDotH, nDotV, nDotL, hDotV);
#endif
    return result;
}

vec3 computeMicrofacetDistributionGGX(float nDotH, vec3 roughness)
{
    vec3 roughnessSquared = roughness * roughness;
    vec3 sqrtDenominator = (roughnessSquared - 1) * nDotH * nDotH + 1;

    // Assume scaling by pi
    return roughnessSquared / (sqrtDenominator * sqrtDenominator);
}

vec3 computeMicrofacetDistributionBeckmann(float nDotH, vec3 roughness)
{
    float nDotHSquared = nDotH * nDotH;
    vec3 roughnessSquared = roughness * roughness;

    // Assume scaling by pi
    return exp((nDotHSquared - 1.0) / (nDotHSquared * roughnessSquared))
            / (nDotHSquared * nDotHSquared * roughnessSquared);
}

vec3 computeMicrofacetDistributionPhong(float nDotH, vec3 roughness)
{
    float nDotHSquared = nDotH * nDotH;
    vec3 roughnessSquared = roughness * roughness;

    // Assume scaling by pi
    return max(vec3(0.0), pow(vec3(nDotH), 2 / roughnessSquared - 2) / (roughnessSquared));
}

vec3 distTimesPi(float nDotH, vec3 roughness)
{
    return computeMicrofacetDistributionGGX(nDotH, roughness);
    //return computeMicrofacetDistributionBeckmann(nDotH, roughness);
    //return computeMicrofacetDistributionPhong(nDotH, roughness);
}

#endif // REFLECTANCEEQUATIONS_GLSL
