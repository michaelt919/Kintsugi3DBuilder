#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#line 5 2003

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform bool useDiffuseEstimate;

struct SpecularResidualInfo
{
    vec3 residualXYZ;
    float nDotL;
    vec3 halfAngleVector;
    float geomRatio;
};

vec3 getDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

vec3 removeDiffuse(vec4 originalColor, vec3 diffuseColor, float maxLuminance,
    float nDotL, vec3 attenuatedLightIntensity, vec3 normal)
{
    if (nDotL <= 0.0)
    {
        return vec3(0);
    }
    else
    {
        vec3 diffuseContrib = diffuseColor * nDotL * attenuatedLightIntensity;
        float cap = maxLuminance - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
        return rgbToXYZ((originalColor.rgb - diffuseContrib) / attenuatedLightIntensity);
    }
}

SpecularResidualInfo computeSpecularResidualInfo()
{
    SpecularResidualInfo info;

    vec3 normal = normalize(fNormal);
    float maxLuminance = getMaxLuminance();
    
    vec3 view = normalize(getViewVector());
    
    // Values of 1.0 for this color would correspond to the expected reflectance
    // for an ideal diffuse reflector (diffuse albedo of 1)
    // Hence, the maximum possible physically plausible reflectance is pi 
    // (for a perfect specular surface reflecting all the incident light in the mirror direction)
    // We should scale this by 1/pi to give values in the range [0, 1],
    // but we don't need to now since there will be another pass to compute reflectivity later.
    vec4 color = getLinearColor();
    
    float nDotV = dot(normal, view);
    
    if (color.a * nDotV > 0)
    {
        LightInfo lightInfo = getLightInfo();
        vec3 light = lightInfo.normalizedDirection;
        info.nDotL = max(0, dot(normal, light));

        info.residualXYZ = useDiffuseEstimate ? 
            removeDiffuse(color, getDiffuseColor(), maxLuminance, info.nDotL, lightInfo.attenuatedIntensity,
                getDiffuseNormalVector())
            : rgbToXYZ(color.rgb / lightInfo.attenuatedIntensity);

        vec3 halfAngle = normalize(light + view);
        vec3 tangent = normalize(fTangent - dot(normal, fTangent));
        vec3 bitangent = normalize(fBitangent
            - dot(normal, fBitangent) * normal 
            - dot(tangent, fBitangent) * tangent);
            
        mat3 tangentToObject = mat3(tangent, bitangent, normal);
        mat3 objectToTangent = transpose(tangentToObject);
        

        info.halfAngleVector = objectToTangent * normalize(view + light);
            // Uncomment the following line for debugging purposes ONLY :
            // acos(objectToTangent * normalize(view + light)) / PI;
        info.geomRatio =
            max(0.0, min(1.0, 2.0 * max(0, info.halfAngleVector.z) * min(nDotV, info.nDotL)) / max(0, dot(view, halfAngle)))
            / (4 * nDotV * info.nDotL);
    }
    else
    {
        info.residualXYZ = vec3(0);
        info.nDotL = 0.0;
        info.halfAngleVector = vec3(0.0);
        info.geomRatio = 0.0;
    }
    
    return info;
}

#endif // SPECULARFIT_GLSL
