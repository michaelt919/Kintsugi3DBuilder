#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#line 7 2004

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;

uniform float fittingGamma;
uniform bool standaloneMode;

vec3 getDiffuseColor()
{
    if (standaloneMode)
    {
        return vec3(0);
    }
    else
    {
        return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));
    }
}

vec3 getDiffuseNormalVector()
{
    if (standaloneMode)
    {
        return vec3(0,0,1);
    }
    else
    {
        return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
    }
}

struct ParameterizedFit
{
    vec3 diffuseColor;
    vec3 normal;
    vec3 specularColor;
    vec3 roughness;
};

vec4 removeDiffuse(vec4 originalColor, vec3 diffuseColor, vec3 light, 
                    vec3 attenuatedLightIntensity, vec3 normal, float maxLuminance)
{
    vec3 diffuseContrib = diffuseColor * max(0, dot(light, normal)) * attenuatedLightIntensity;
    float cap = maxLuminance - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
    vec3 remainder = clamp(originalColor.rgb - diffuseContrib, 0, cap);
    return vec4(remainder, 
        originalColor.a * pow(remainder.r * remainder.g * remainder.b, 1.0 / (3 * fittingGamma)));
}

ParameterizedFit fitSpecular()
{
    vec3 normal = normalize(fNormal);

    vec3 tangent = normalize(fTangent - dot(normal, fTangent));
    vec3 bitangent = normalize(fBitangent
        - dot(normal, fBitangent) * normal
        - dot(tangent, fBitangent) * tangent);

    mat3 tangentToObject = mat3(tangent, bitangent, normal);
    vec3 shadingNormalTS = getDiffuseNormalVector();
    vec3 shadingNormal = tangentToObject * shadingNormalTS;

    vec3 diffuseColor = getDiffuseColor();

    float maxLuminance = getMaxLuminance();
    vec4 maxResidual = vec4(0);
    float maxResidualLuminance = 0;
    //vec3 maxResidualDirection = vec3(0);

    vec3 directionSum = vec3(0);
    vec3 intensityWeightedDirectionSum = vec3(0);

    for (int i = 0; i < viewCount; i++)
    {
        vec4 color = getLinearColor(i);
        vec3 view = normalize(getViewVector(i));

        if (color.a * dot(view, normal) > 0)
        {
            vec3 lightPreNormalized = getLightVector(i);
            vec3 attenuatedLightIntensity = infiniteLightSources ?
                getLightIntensity(i) :
                getLightIntensity(i) / (dot(lightPreNormalized, lightPreNormalized));
            vec3 light = normalize(lightPreNormalized);

            vec3 colorRemainder =
                removeDiffuse(color, diffuseColor, light, attenuatedLightIntensity, shadingNormal, maxLuminance).rgb / attenuatedLightIntensity;
            float luminance = getLuminance(colorRemainder);

            vec3 halfway = normalize(view + light);
            float nDotH = dot(normal, halfway);

            if (nDotH * nDotH > 0.5)
            {
                directionSum += halfway;
                intensityWeightedDirectionSum += halfway * luminance;

                if (luminance * nDotH > maxResidualLuminance * maxResidual.a)
                {
                    maxResidual = vec4(colorRemainder, nDotH);
                    maxResidualLuminance = luminance;
                    //maxResidualDirection = halfway;
                }
            }
        }
    }

    vec3 specularNormal;

    if(dot(intensityWeightedDirectionSum, intensityWeightedDirectionSum) < 1.0)
    {
        intensityWeightedDirectionSum += (1 - length(intensityWeightedDirectionSum)) * shadingNormal;
    }

    float directionScale = length(directionSum);
    vec3 averageDirection = directionSum / max(1, directionScale);
    float specularNormalFidelity = dot(averageDirection, normal);
    vec3 certaintyDirectionUnnormalized = cross(averageDirection - specularNormalFidelity * normal, normal);
    vec3 certaintyDirection = certaintyDirectionUnnormalized
        / max(1, length(certaintyDirectionUnnormalized));

    vec3 specularNormalEstimate = normalize(intensityWeightedDirectionSum);
    float specularNormalCertainty =
        min(1, directionScale) * dot(specularNormalEstimate, certaintyDirection);
    vec3 scaledCertaintyDirection = specularNormalCertainty * certaintyDirection;
    specularNormal = normalize(
        scaledCertaintyDirection
            + sqrt(1 - specularNormalCertainty * specularNormalCertainty
                        * dot(certaintyDirection, certaintyDirection))
                * normalize(mix(normal, normalize(specularNormalEstimate - scaledCertaintyDirection),
                    min(1, directionScale) * specularNormalFidelity)));

    vec3 chromaticitySum = vec3(0);
    vec3 roughnessSums[3];
    roughnessSums[0] = vec3(0);
    roughnessSums[1] = vec3(0);
    roughnessSums[2] = vec3(0);
    
    for (int i = 0; i < viewCount; i++)
    {
        vec3 view = normalize(getViewVector(i));
        
        // Values of 1.0 for this color would correspond to the expected reflectance
        // for an ideal diffuse reflector (diffuse albedo of 1), which is a reflectance of 1 / pi.
        // Hence, this color corresponds to the reflectance times pi.
        // Both the Phong model and the Cook Torrance with a Beckmann distribution also have a 1/pi factor.
        // By adopting the convention that all reflectance values are scaled by pi in this shader,
        // We can avoid division by pi here as well as the 1/pi factors in the parameterized models.
        vec4 color = getLinearColor(i);

        if (color.a * dot(view, normal) > 0)
        {
            vec3 lightPreNormalized = getLightVector(i);
            vec3 attenuatedLightIntensity = infiniteLightSources ? 
                getLightIntensity(i) : 
                getLightIntensity(i) / (dot(lightPreNormalized, lightPreNormalized));
            vec3 light = normalize(lightPreNormalized);
            float nDotL = max(0, dot(light, specularNormal));
            float nDotV = max(0, dot(specularNormal, view));
            
            vec3 halfway = normalize(view + light);
            float nDotH = dot(halfway, specularNormal);
            float nDotHSquared = nDotH * nDotH;
            
            if (nDotV > 0 && nDotHSquared > 0.5)
            {
                float hDotV = max(0, dot(halfway, view));

                vec3 colorRemainder =
                    removeDiffuse(color, diffuseColor, light, attenuatedLightIntensity, normal, maxLuminance).rgb / attenuatedLightIntensity;
                vec3 colorXYZGammaCorrected = pow(rgbToXYZ(colorRemainder), vec3(1.0 / fittingGamma));

                chromaticitySum += vec3(
                    colorXYZGammaCorrected.y,
                    5 * (colorXYZGammaCorrected.x - colorXYZGammaCorrected.y),
                    2 * (colorXYZGammaCorrected.y - colorXYZGammaCorrected.z));

                if (nDotV * (1 + nDotHSquared) * (1 + nDotHSquared) > 1.0)
                {
                    roughnessSums[0] += 1//pow(colorXYZ * nDotV, vec3(1.0 / fittingGamma))
                        * sqrt(colorRemainder * nDotV) * (1 - nDotHSquared);

                    roughnessSums[1] += 1//pow(colorXYZ * nDotV, vec3(1.0 / fittingGamma))
                        * sqrt(colorRemainder * nDotV) * nDotHSquared;

                    roughnessSums[2] += 1;//pow(colorXYZ * nDotV, vec3(1.0 / fittingGamma));
                }
            }
        }
    }

     // Chromatic roughness
//     vec3 roughnessSquared = min(vec3(1.0), roughnessSums[0] /
//             (sqrt(maxResidual.rgb) * roughnessSums[2] - roughnessSums[1]));

//    // Monochrome roughness
    vec3 roughnessSquared = vec3(min(1.0, getLuminance(roughnessSums[0]) /
        (sqrt(maxResidualLuminance) * getLuminance(roughnessSums[2]) - getLuminance(roughnessSums[1]))));

    vec3 specularColor = 4 * roughnessSquared * maxResidual.rgb;

    vec3 adjustedDiffuseColor = diffuseColor - specularColor * roughnessSquared / 2;

    // Dividing by the sum of weights to get the weighted average.
    // We'll put a lower cap of 1/m^2 on the alpha we divide by so that noise doesn't get amplified
    // for texels where there isn't enough information at the specular peak.
    return ParameterizedFit(adjustedDiffuseColor,
        normalize(transpose(tangentToObject) * specularNormal), specularColor, sqrt(roughnessSquared));
}

#endif // SPECULARFIT_GLSL
