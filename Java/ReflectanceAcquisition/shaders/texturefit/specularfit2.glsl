#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#include "../reflectance/reflectance.glsl"

#line 7 2004

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D roughnessEstimate;

vec3 getDiffuseColor()
{
    // Maximum possible diffuse reflectance is 1 / pi.
    return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma)) / PI;
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

float getRoughness()
{
    return texture(roughnessEstimate, fTexCoord).r;
}

vec4 removeDiffuse(vec4 originalColor, vec3 diffuseColor, vec3 light, 
    vec3 attenuatedLightIntensity, vec3 normal)
{
    float nDotL = max(0, dot(light, normal));
    if (nDotL == 0.0)
    {
        return vec4(0);
    }
    else
    {
        vec3 diffuseContrib = diffuseColor * nDotL * attenuatedLightIntensity;
        float cap = 1.0 - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
        vec3 remainder = clamp(originalColor.rgb - diffuseContrib, 0, cap);
        return vec4(remainder / nDotL, originalColor.a * nDotL);
    }
}

vec4 fitSpecular()
{
    vec3 geometricNormal = normalize(fNormal);
    vec3 diffuseNormal = getDiffuseNormalVector();
    vec3 diffuseColor = getDiffuseColor();
    float roughness = getRoughness();
    
    vec4 sum = vec4(0);
    
    for (int i = 0; i < viewCount; i++)
    {
        vec3 view = normalize(getViewVector(i));
        float nDotV = dot(geometricNormal, view);
        
        // Values of 1.0 for this color would correspond to the expected reflectance
        // for an ideal diffuse reflector (diffuse albedo of 1)
        // Hence, the maximum possible physically plausible reflectance is pi 
        // (for a perfect specular surface reflecting all the incident light in the mirror direction)
        // We should scale this by 1/pi to give values in the range [0, 1],
        // but we'll wait until the end, since removeDiffuse() depends on luminance values being 
        // in the same scale as in the original photographs.
        vec4 color = getLinearColor(i);
        
        if (color.a * nDotV > 0)
        {
            vec3 lightPreNormalized = getLightVector(i);
            vec3 attenuatedLightIntensity = infiniteLightSources ? 
                getLightIntensity(i) : 
                getLightIntensity(i) / (dot(lightPreNormalized, lightPreNormalized));
            vec3 light = normalize(lightPreNormalized);
            
            vec4 colorRemainder = 
                removeDiffuse(color, diffuseColor, light, attenuatedLightIntensity, diffuseNormal);
            
            vec3 half = normalize(view + light);
            float nDotH = dot(half, diffuseNormal);
            
            if (nDotH > 0.0)
            {
                float nDotHSquared = nDotH * nDotH;
                float roughnessSquared = roughness * roughness;
                
                float mfdEval = exp((nDotHSquared - 1.0) / (nDotHSquared * roughnessSquared)) 
                    / (nDotHSquared * nDotHSquared);
                
                sum += colorRemainder.a * mfdEval * vec4(colorRemainder.rgb, mfdEval);
            }
        }
    }
    
    // Dividing by the sum of weights to get the weighted average,
    // and by pi to get the specular reflectivity in the correct scale (see comment above).
    return sum / (PI * sum.a);
}

#endif // SPECULARFIT_GLSL
