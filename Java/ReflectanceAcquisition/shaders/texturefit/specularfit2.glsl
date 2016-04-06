#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#include "../reflectance/reflectance.glsl"

#line 7 2004

const float PI = 3.1415926535897932384626433832795;

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

vec4 removeDiffuse(vec4 originalColor, vec3 diffuseColor, vec3 light, vec3 attenuatedLightIntensity, vec3 normal)
{
    float nDotL = max(0, dot(light, normal));
    vec3 diffuseContrib = diffuseColor * nDotL * attenuatedLightIntensity;
    float cap = 1.0 - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
    vec3 remainder = clamp(originalColor.rgb - diffuseContrib, 0, cap);
    return vec4(remainder / nDotL, originalColor.a * nDotL);
}

vec3 fitSpecular()
{
    vec3 geometricNormal = normalize(fNormal);
    vec3 diffuseNormal = getDiffuseNormalVector();
    vec3 diffuseColor = getDiffuseColor();
    float roughness = 0.5;//getRoughness();
    
    vec4 sum = vec4(0);
    
    for (int i = 0; i < viewCount; i++)
    {
        vec3 view = normalize(getViewVector(i));
        vec4 color = getColor(i);
        float nDotV = dot(geometricNormal, view);
        
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
                
                //sum += colorRemainder.a * mfdEval * vec4(colorRemainder.rgb, mfdEval);
                sum = max(sum, colorRemainder.a * vec4(colorRemainder.rgb, mfdEval));
            }
        }
    }
    
    return sum.rgb / sum.a;
}

#endif // SPECULARFIT_GLSL