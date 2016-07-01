#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#include "../reflectance/reflectance_single.glsl"

#line 7 2003

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;

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

// struct DiffuseRemovalResult
// {
	// float luminance;
	// float alpha;
	// float nDotL;
// }

// DiffuseRemovalResult removeDiffuse(vec4 originalColor, vec3 diffuseColor, float maxLuminance,
    // vec3 light, vec3 attenuatedLightIntensity, vec3 normal)
// {
    // float nDotL = max(0, dot(light, normal));
    // if (nDotL == 0.0)
    // {
        // return vec2(0);
    // }
    // else
    // {
        // vec3 diffuseContrib = diffuseColor * nDotL * attenuatedLightIntensity;
        // float cap = maxLuminance - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
        // float remainder = getLuminance((originalColor.rgb - diffuseContrib) / attenuatedLightIntensity);
        // return DiffuseRemovalResult(remainder, originalColor.a, nDotL);
    // }
// }

SpecularResidualInfo computeSpecularResidualInfo()
{
    SpecularResidualInfo info;

    vec3 geometricNormal = normalize(fNormal);
    vec3 diffuseNormal = getDiffuseNormalVector();
    vec3 diffuseColor = getDiffuseColor();
    float maxLuminance = getMaxLuminance();
    
    vec3 view = normalize(getViewVector());
    
    // Values of 1.0 for this color would correspond to the expected reflectance
    // for an ideal diffuse reflector (diffuse albedo of 1)
    // Hence, the maximum possible physically plausible reflectance is pi 
    // (for a perfect specular surface reflecting all the incident light in the mirror direction)
    // We should scale this by 1/pi to give values in the range [0, 1],
    // but we don't need to now since there will be another pass to compute reflectivity later.
    // Additionally removeDiffuse() depends on luminance values being in the same scale
    // as in the original photographs.
    vec4 color = getLinearColor();
    
    float nDotV = dot(geometricNormal, view);
    
    if (color.a * nDotV > 0)
    {
        vec3 lightPreNormalized = getLightVector();
        vec3 attenuatedLightIntensity = infiniteLightSource ? 
			lightIntensity : lightIntensity / (dot(lightPreNormalized, lightPreNormalized));
        vec3 light = normalize(lightPreNormalized);
		info.nDotL = max(0, dot(geometricNormal, light));
		
        info.residualXYZ = rgbToXYZ(color.rgb / attenuatedLightIntensity);
				
		vec3 halfAngle = normalize(light + view);
        vec3 tangent = normalize(fTangent - dot(geometricNormal, fTangent));
        vec3 bitangent = normalize(fBitangent
            - dot(geometricNormal, fBitangent) * geometricNormal 
            - dot(tangent, fBitangent) * tangent);
            
        mat3 tangentToObject = mat3(tangent, bitangent, geometricNormal);
        mat3 objectToTangent = transpose(tangentToObject);
        
		
        info.halfAngleVector = objectToTangent * normalize(view + light);
		info.geomRatio = 
			max(0.0, min(1.0, 2.0 * max(0, info.halfAngleVector.z) * min(nDotV, info.nDotL)) / max(0, dot(view, halfAngle)))
			/ (4 * nDotV * info.nDotL);
        
        // // TODO debug code, remove this
        // float roughnessSquared = 0.03846153846153846153846153846154; //0.25 * 0.25;
        // float nDotHSquared = info.halfAngleVector.z * info.halfAngleVector.z;
        // info.residualLuminance = exp((nDotHSquared - 1.0) / (nDotHSquared * roughnessSquared))
            // / (nDotHSquared * nDotHSquared);
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
