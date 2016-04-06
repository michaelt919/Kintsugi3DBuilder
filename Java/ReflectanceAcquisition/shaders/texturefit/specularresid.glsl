#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#include "../reflectance/reflectance_single.glsl"

#line 7 2003

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;

struct SpecularResidualInfo
{
    vec4 residual;
    vec3 halfAngleVector;
};

vec3 getDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

vec4 removeDiffuse(vec4 originalColor, vec3 diffuseColor, vec3 light, vec3 attenuatedLightIntensity, vec3 normal)
{
    float nDotL = max(0, dot(light, normal));
    vec3 diffuseContrib = diffuseColor * nDotL * attenuatedLightIntensity;
    float cap = 1.0 - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
    vec3 remainder = clamp(originalColor.rgb - diffuseContrib, 0, cap);
    return vec4(remainder / nDotL, originalColor.a * nDotL);
}

SpecularResidualInfo computeSpecularResidualInfo()
{
    SpecularResidualInfo info;

    vec3 geometricNormal = normalize(fNormal);
    vec3 diffuseNormal = getDiffuseNormalVector();
    vec3 diffuseColor = getDiffuseColor();
    
    vec3 view = normalize(getViewVector());
    vec4 color = getColor();
    float nDotV = dot(geometricNormal, view);
    
    if (color.a * nDotV > 0)
    {
        vec3 lightPreNormalized = getLightVector();
        vec3 attenuatedLightIntensity = infiniteLightSource ? lightIntensity : lightIntensity / (dot(lightPreNormalized, lightPreNormalized));
        vec3 light = normalize(lightPreNormalized);
        
        info.residual = removeDiffuse(color, diffuseColor, light, attenuatedLightIntensity, diffuseNormal);
        
        vec3 tangent = normalize(fTangent - dot(geometricNormal, fTangent));
        vec3 bitangent = normalize(fBitangent
            - dot(geometricNormal, fBitangent) * geometricNormal 
            - dot(tangent, fBitangent) * tangent);
            
        mat3 tangentToObject = mat3(tangent, bitangent, geometricNormal);
        mat3 objectToTangent = transpose(tangentToObject);
        
        info.halfAngleVector = objectToTangent * normalize(view + light);
        
        //info.residual.rgb = vec3(0.3 * pow(max(0, info.halfAngleVector.z), 10));
        float nDotH = max(0,dot(diffuseNormal, normalize(view + light)));
        float nDotHSquared = nDotH * nDotH;
        
        //info.residual = vec4(0.5, 0.5, 0.5, 1.0);
        //info.residual.rgb = vec3(0.5 * exp((nDotHSquared - 1) / (nDotHSquared * 0.0625)) / (nDotHSquared * nDotHSquared));
    }
    else
    {
        info.residual.a = 0.0;
    }
    
    return info;
}

#endif // SPECULARFIT_GLSL
