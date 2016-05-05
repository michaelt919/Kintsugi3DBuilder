#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#include "../reflectance/reflectance.glsl"

#line 7 2004

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D roughnessEstimate;

vec3 getDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

float getRoughness()
{
    return texture(roughnessEstimate, fTexCoord).r;
}

vec4 removeDiffuse(vec4 originalColor, vec3 diffuseColor, float maxLuminance,
    vec3 light, vec3 attenuatedLightIntensity, vec3 normal)
{
    float nDotL = max(0, dot(light, normal));
    if (nDotL == 0.0)
    {
        return vec4(0);
    }
    else
    {
        vec3 diffuseContrib = diffuseColor * nDotL * attenuatedLightIntensity;
        float cap = maxLuminance - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
        vec3 remainder = clamp(originalColor.rgb - diffuseContrib, 0, cap);
        return vec4(remainder, originalColor.a);
    }
}

vec4 fitSpecular()
{
    vec3 geometricNormal = normalize(fNormal);
    vec3 diffuseNormal = getDiffuseNormalVector();
    vec3 diffuseColor = getDiffuseColor();
    float roughness = getRoughness();
    float roughnessSquared = roughness * roughness;
    float maxLuminance = getMaxLuminance();
    
    vec4 sum = vec4(0);
    
    for (int i = 0; i < viewCount; i++)
    {
        vec3 view = normalize(getViewVector(i));
        float nDotV = dot(geometricNormal, view);
        
        // Values of 1.0 for this color would correspond to the expected reflectance
        // for an ideal diffuse reflector (diffuse albedo of 1)
        // Hence, the maximum possible physically plausible reflectance is pi 
        // (for a perfect specular surface reflecting all the incident light in the mirror direction)
        // However, for the Torrance-Sparrow model with a Beckmann microfacet model,
        // the maximum possible reflectance is 1 / (4 pi m^2), where m is the microfacet roughness.
        // Therefore, we don't need to actually scale by 1 / pi now
        // because we would just need to multiply by pi again at the end to get a specular albedo value.
        vec4 color = getLinearColor(i);
        
        if (color.a * nDotV > 0)
        {
            vec3 lightPreNormalized = getLightVector(i);
            vec3 attenuatedLightIntensity = infiniteLightSources ? 
                getLightIntensity(i) : 
                getLightIntensity(i) / (dot(lightPreNormalized, lightPreNormalized));
            vec3 light = normalize(lightPreNormalized);
            
            vec4 colorRemainder = removeDiffuse(color, diffuseColor, maxLuminance, 
                    light, attenuatedLightIntensity, diffuseNormal);
            
            vec3 half = normalize(view + light);
            float nDotH = dot(half, diffuseNormal);
            
            if (nDotH > 0.0)
            {
                float nDotHSquared = nDotH * nDotH;
                
                float mfdEval = exp((nDotHSquared - 1.0) / (nDotHSquared * roughnessSquared)) 
                    / (nDotHSquared * nDotHSquared);
                    
                // // TODO debug code remove this
                // colorRemainder.rgb = 
                    // vec3(PI * pow(0.5, 2.2) * exp((nDotHSquared - 1.0) / (nDotHSquared * roughnessSquared))
                        // / (PI * roughnessSquared * nDotHSquared * nDotHSquared));
                
                // Multiply by 4 n dot v to cancel out the denominator in the Cook-Torrance BRDF
                sum += colorRemainder.a * mfdEval * vec4(colorRemainder.rgb * 4 * nDotV, mfdEval);
            }
        }
    }
    
    // Dividing by the sum of weights to get the weighted average.
    // We'll put a lower cap of 1.0 on the alpha we divide by so that noise doesn't get amplified
    // for texels where there isn't enough information at the specular peak.
    // The reflectance is already scaled by pi (see comment above)
    // but needs to be scaled by an additional factor of m^2 (where m is the microfacet roughness)
    // to get a specular albedo ranging from 0 to 1.
    return vec4(sum.rgb * roughnessSquared / max(1.0, sum.a), 1.0);
}

#endif // SPECULARFIT_GLSL
