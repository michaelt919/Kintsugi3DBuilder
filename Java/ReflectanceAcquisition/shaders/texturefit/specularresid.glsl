#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#include "../reflectance/reflectance_single.glsl"

#line 7 2003

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;

struct SpecularResidualInfo
{
    float residualLuminance;
    float alpha;
    vec3 halfAngleVector;
};

vec3 getDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma)) / PI;
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

vec4 removeDiffuse(vec4 originalColor, vec3 diffuseColor, vec3 light, vec3 attenuatedLightIntensity, vec3 normal)
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

SpecularResidualInfo computeSpecularResidualInfo()
{
    SpecularResidualInfo info;

    vec3 geometricNormal = normalize(fNormal);
    vec3 diffuseNormal = getDiffuseNormalVector();
    vec3 diffuseColor = getDiffuseColor();
    
    vec3 view = normalize(getViewVector());
    
    // Values of 1.0 for this color would correspond to the expected reflectance
    // for an ideal diffuse reflector (diffuse albedo of 1)
    // Hence, the maximum possible physically plausible reflectance is pi 
    // (for a perfect specular surface reflecting all the incident light in the mirror direction)
    // We should scale this by 1/pi to give values in the range [0, 1],
    // but we don't need to now since there will be another pass to compute reflectivity later.
    // Additionally removeDiffuse() depends on luminance values being in the same scale
    // as in the original photographs.
    vec4 color = getLinearColor() / PI;
    
    float nDotV = dot(geometricNormal, view);
    
    if (color.a * nDotV > 0)
    {
        vec3 lightPreNormalized = getLightVector();
        vec3 attenuatedLightIntensity = infiniteLightSource ? lightIntensity : lightIntensity / (dot(lightPreNormalized, lightPreNormalized));
        vec3 light = normalize(lightPreNormalized);
        
        vec4 residual = removeDiffuse(color, diffuseColor, light, attenuatedLightIntensity, diffuseNormal);
        info.residualLuminance = getLuminance(residual.rgb);
        info.alpha = residual.a;
        
        vec3 tangent = normalize(fTangent - dot(geometricNormal, fTangent));
        vec3 bitangent = normalize(fBitangent
            - dot(geometricNormal, fBitangent) * geometricNormal 
            - dot(tangent, fBitangent) * tangent);
            
        mat3 tangentToObject = mat3(tangent, bitangent, geometricNormal);
        mat3 objectToTangent = transpose(tangentToObject);
        
        info.halfAngleVector = objectToTangent * normalize(view + light);
    }
    else
    {
        info.alpha = 0.0;
    }
    
    return info;
}

#endif // SPECULARFIT_GLSL
