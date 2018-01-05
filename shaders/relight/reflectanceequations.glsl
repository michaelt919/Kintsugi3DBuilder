#ifndef REFLECTANCEEQUATIONS_GLSL
#define REFLECTANCEEQUATIONS_GLSL

#line 5 3001

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

//vec3 computeGeometricAttenuationSmithBeckmann(vec3 roughness, float nDotV, float nDotL)
//{
//    vec3 aV = 1.0 / (roughness * sqrt(1.0 - nDotV * nDotV) / nDotV);
//    vec3 aVSq = aV * aV;
//    vec3 aL = 1.0 / (roughness * sqrt(1.0 - nDotL * nDotL) / nDotL);
//    vec3 aLSq = aL * aL;
//
//    return (aV < 1.6 ? (3.535 * aV + 2.181 * aVSq) / (1 + 2.276 * aV + 2.577 * aVSq) : 1.0)
//            * (aL < 1.6 ? (3.535 * aL + 2.181 * aLSq) / (1 + 2.276 * aL + 2.577 * aLSq) : 1.0);
//        // ^ See Walter et al. "Microfacet Models for Refraction through Rough Surfaces"
//        // for this formula
//}

vec3 computeGeometricAttenuationSmithGGX(vec3 roughness, float nDotV, float nDotL)
{
    vec3 roughnessSq = roughness * roughness;
    return 4 / (1 + sqrt(1 + roughnessSq * (1 / (nDotV * nDotV) - 1.0)))
             / (1 + sqrt(1 + roughnessSq * (1 / (nDotL * nDotL) - 1.0)));
}

vec3 geom(vec3 roughness, float nDotH, float nDotV, float nDotL, float hDotV)
{
    //return nDotV * nDotL;
    return vec3(computeGeometricAttenuationVCavity(nDotH, nDotV, nDotL, hDotV));
    //return computeGeometricAttenuationSmithBeckmann(roughness, nDotV, nDotL);
    //return computeGeometricAttenuationSmithGGX(roughness, nDotV, nDotL);
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

vec3 dist(float nDotH, vec3 roughness)
{
    return computeMicrofacetDistributionGGX(nDotH, roughness);
    //return computeMicrofacetDistributionBeckmann(nDotH, roughness);
    //return computeMicrofacetDistributionPhong(nDotH, roughness);
}

#endif // REFLECTANCEEQUATIONS_GLSL
