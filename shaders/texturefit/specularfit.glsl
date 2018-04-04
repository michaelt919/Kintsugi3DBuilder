#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#line 5 2004

#define DARPA_MODE 0

#define MIN_ROUGHNESS 0.00390625    // 1/256
#define MAX_ROUGHNESS 1.0 // 0.70710678 // sqrt(1/2)

#define MIN_SPECULAR_REFLECTIVITY 0.04 // corresponds to dielectric with index of refraction = 1.5
#define MAX_ROUGHNESS_WHEN_CLAMPING MAX_ROUGHNESS

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;

uniform float fittingGamma;
uniform bool standaloneMode;

//uniform vec4 normalCandidateWeights;
uniform vec2 normalCandidate;

#define fitNearSpecularOnly (DARPA_MODE != 0) // should be true for DARPA stuff (at least when comparing with Joey), false for cultural heritage
#define chromaticRoughness (false && DARPA_MODE == 0)
#define chromaticSpecular true
#define aggressiveNormal false
#define relaxedSpecularPeaks (true && DARPA_MODE == 0)
#define USE_LIGHT_INTENSITIES 1

#define LINEAR_WEIGHT_MODE DARPA_MODE
#define PERCEPTUAL_WEIGHT_MODE (!DARPA_MODE)

vec4 getDiffuseColor()
{
    if (standaloneMode)
    {
        return vec4(0, 0, 0, 1);
    }
    else
    {
        vec4 textureResult = texture(diffuseEstimate, fTexCoord);
        return vec4(pow(textureResult.rgb, vec3(gamma)), textureResult.a);
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
    vec4 diffuseColor;
    vec4 normal;
    vec4 specularColor;
    vec4 roughness;
};

vec4 removeDiffuse(vec4 originalColor, vec3 diffuseColor, vec3 light,
                    vec3 attenuatedLightIntensity, vec3 normal, float maxLuminance)
{
    vec3 diffuseContrib = diffuseColor * max(0, dot(light, normal)) * attenuatedLightIntensity;

    if (chromaticSpecular)
    {
        float cap = maxLuminance - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
        vec3 remainder = clamp(originalColor.rgb - diffuseContrib, 0, cap);
        return vec4(remainder, cap);
    }
    else
    {
        vec3 remainder = max(originalColor.rgb - diffuseContrib, vec3(0));
        float remainderMin = min(min(remainder.r, remainder.g), remainder.b);
        return vec4(vec3(remainderMin), 1.0);
    }
}

ParameterizedFit fitSpecular()
{
    vec3 normal = normalize(fNormal);

    vec3 tangent = normalize(fTangent - dot(normal, fTangent));
    vec3 bitangent = normalize(fBitangent
        - dot(normal, fBitangent) * normal
        - dot(tangent, fBitangent) * tangent);

    mat3 tangentToObject = mat3(tangent, bitangent, normal);
    vec3 diffuseNormalTS = getDiffuseNormalVector();
    vec3 oldDiffuseNormal = tangentToObject * diffuseNormalTS;

    vec4 diffuseColor = getDiffuseColor();

    float maxLuminance = getMaxLuminance();
    vec3 maxResidual = vec3(0);
    vec2 maxResidualLuminance = vec2(0);
    vec4 maxResidualDirection = vec4(0);

    vec3 directionSum = vec3(0);
    vec3 intensityWeightedDirectionSum = vec3(0);

    float[255] normalXBins;
    float[255] normalYBins;
    float[255] normalXWeightBins;
    float[255] normalYWeightBins;

    for (int i = 0; i < 255; i++)
    {
        normalXBins[i] = 0.0;
        normalYBins[i] = 0.0;
        normalXWeightBins[i] = 0.0;
        normalYWeightBins[i] = 0.0;
    }

    float binSum = 0.0;
    float binWeightSum = 0;

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        vec4 color = getLinearColor(i);
        vec3 view = normalize(getViewVector(i));
        float nDotV = dot(view, normal);

        if (color.a * dot(view, normal) > 0)
        {
            LightInfo lightInfo = getLightInfo(i);
            vec3 light = lightInfo.normalizedDirection;

            vec3 colorRemainder;

#if USE_LIGHT_INTENSITIES
            colorRemainder = removeDiffuse(color, diffuseColor.rgb, light, lightInfo.attenuatedIntensity, oldDiffuseNormal, maxLuminance).rgb
                / lightInfo.attenuatedIntensity;
#else
            colorRemainder = removeDiffuse(color, diffuseColor.rgb, light, vec3(1.0), oldDiffuseNormal, maxLuminance).rgb;
#endif

            float luminance = getLuminance(colorRemainder);

            vec3 halfway = normalize(view + light);

            float weight = clamp(sqrt(2) * nDotV, 0, 1);

            directionSum += weight * halfway;
            intensityWeightedDirectionSum += weight * halfway * luminance;

            float normalWeight = weight * clamp(luminance * 10 - 9, 0, 1);

            if (normalWeight > maxResidualLuminance[1] * clamp(maxResidualLuminance[0] * 10 - 9, 0, 1))
            {
                maxResidualDirection = normalWeight * vec4(halfway, 1);
                maxResidualLuminance = vec2(luminance, weight);
                maxResidual = colorRemainder;
            }
            else if (maxResidualLuminance[0] * 10 <= 9 && luminance * weight > maxResidualLuminance[0] * maxResidualLuminance[1])
            {
                maxResidualLuminance = vec2(luminance, weight);
                maxResidual = colorRemainder;
            }

            float cameraWeight = getCameraWeight(i);
            vec3 halfwayTS = transpose(tangentToObject) * halfway;

            normalXBins[int(round(halfwayTS.x * 127 + 127))] += cameraWeight * luminance;
            normalYBins[int(round(halfwayTS.y * 127 + 127))] += cameraWeight * luminance;

            binSum += cameraWeight * luminance;

            normalXWeightBins[int(round(halfwayTS.x * 127 + 127))] += cameraWeight;
            normalYWeightBins[int(round(halfwayTS.y * 127 + 127))] += cameraWeight;

            binWeightSum += cameraWeight;
        }
    }

    if (dot(intensityWeightedDirectionSum, intensityWeightedDirectionSum) < 1.0)
    {
        intensityWeightedDirectionSum += (1 - length(intensityWeightedDirectionSum)) * oldDiffuseNormal;
    }

    vec3 maxResidualXYZ = rgbToXYZ(maxResidual);
    float maxResidualComponent = max(max(maxResidualXYZ.x, maxResidualXYZ.y), maxResidualXYZ.z);
    float luminanceUncertaintyRange = max(0, maxResidualComponent - 0.9);

    vec3 specularNormal;
    vec3 biasedHeuristicNormal;
    vec3 bias;
    float resolvability;

    biasedHeuristicNormal = normalize(intensityWeightedDirectionSum);
    float directionScale = length(directionSum);
    resolvability = min(1, directionScale);
    bias = directionSum / max(1, directionScale);



//    vec2 heuristicNormalTS;
//    vec2 biasTS;
//
//    float runningXSum = normalXBins[0];
//    float runningYSum = normalYBins[0];
//
//    float runningXWeightSum = normalXWeightBins[0];
//    float runningYWeightSum = normalYWeightBins[0];
//
//    for (int i = 1; i < 255; i++)
//    {
//        float nextXSum = runningXSum + normalXBins[i];
//        float nextYSum = runningYSum + normalYBins[i];
//
//        float nextXWeightSum = runningXWeightSum + normalXWeightBins[i];
//        float nextYWeightSum = runningYWeightSum + normalYWeightBins[i];
//
//        if (nextXSum != runningXSum && runningXSum <= binSum / 2 && binSum / 2 < nextXSum)
//        {
//            heuristicNormalTS.x = (mix(i - 1, i, (binSum / 2 - runningXSum) / (nextXSum - runningXSum)) - 127) / 127.0;
//        }
//
//        if (nextYSum != runningYSum && runningYSum <= binSum / 2 && binSum / 2 <= nextYSum)
//        {
//            heuristicNormalTS.y = (mix(i - 1, i, (binSum / 2 - runningYSum) / (nextYSum - runningYSum)) - 127) / 127.0;
//        }
//
//        if (nextXWeightSum != runningXWeightSum && runningXWeightSum <= binWeightSum / 2 && binWeightSum / 2 < nextXWeightSum)
//        {
//            biasTS.x = (mix(i - 1, i, float(binWeightSum * 0.5 - runningXWeightSum) / float(nextXWeightSum - runningXWeightSum)) - 127) / 127.0;
//        }
//
//        if (nextYWeightSum != runningYWeightSum && runningYWeightSum <= binWeightSum / 2 && binWeightSum / 2 < nextYWeightSum)
//        {
//            biasTS.y = (mix(i - 1, i, float(binWeightSum * 0.5 - runningYWeightSum) / float(nextYWeightSum - runningYWeightSum)) - 127) / 127.0;
//        }
//
//        runningXSum = nextXSum;
//        runningYSum = nextYSum;
//        runningXWeightSum = nextXWeightSum;
//        runningYWeightSum = nextYWeightSum;
//    }
//
//    biasedHeuristicNormal = tangentToObject * vec3(heuristicNormalTS, sqrt(1 - dot(heuristicNormalTS, heuristicNormalTS)));
//    bias = tangentToObject * vec3(biasTS, sqrt(1 - dot(biasTS, biasTS)));
//    resolvability = 1.0;





    vec3 heuristicNormal;

    if (aggressiveNormal)
    {
        heuristicNormal = biasedHeuristicNormal;
    }
    else
    {
        float specularNormalFidelity = dot(bias, normal);                                               // correlation between biased average (either normalized or between 0-1) and geometric normal (normalized)
        vec3 certaintyDirectionUnnormalized = cross(bias - specularNormalFidelity * normal, normal);    // component of biased average orthogonal to geometric normal
        vec3 certaintyDirection = certaintyDirectionUnnormalized                                        // points in direction of component of biased average orthogonal to geometric normal
            / max(1, length(certaintyDirectionUnnormalized));                                           // length is between 0 (no bias) and 1 (biased average completely orthogonal to geometric normal)

        float specularNormalCertainty =                                                                 // correlation between biased normal and the scaled "certainty" direction
            resolvability * dot(biasedHeuristicNormal, certaintyDirection);                             // range is between 0 (no bias or singular conditions) and 1 (orthogonally biased)
        vec3 scaledCertaintyDirection = specularNormalCertainty * certaintyDirection;                   // projection of biased normal onto scaled "certainty" direction
                                                                                                        // length is between 0 (no bias) and 1 (biased average completely orthogonal to geometric normal)

        heuristicNormal = normalize(                                                                    // normalize the following:
            scaledCertaintyDirection                                                                    // projection of biased normal onto scaled "certainty" direction
                + sqrt(1 - specularNormalCertainty * specularNormalCertainty                            // length of vector which, if orthogonal to the preceding projection,
                            * dot(certaintyDirection, certaintyDirection))                              // is such that their sum has length 1
                    * normalize(mix(normal, normalize(biasedHeuristicNormal - scaledCertaintyDirection),// mix between the geometric normal and the component of the potentially biased normal orthogonal to the scaled "certainty" direction
                       resolvability * specularNormalFidelity)));                                       // using the non-singularity and the correlation between the biased average and the geometric normal
    }                                                                                                   // as the basis for whether to select the potentially biased normal.

//    specularNormal = normalize(mat4x3(normal, oldDiffuseNormal, maxResidualDirection, heuristicNormal)
//                        * normalCandidateWeights);
//
//    specularNormal = tangentToObject * vec3(normalCandidate, sqrt(1 - dot(normalCandidate, normalCandidate)));

    specularNormal = maxResidualDirection.xyz + (1 - maxResidualDirection.w) * heuristicNormal;




    // Estimate the roughness and specular reflectivity (in an XYZ color space).
    vec3 roughnessSquared;
    vec3 specularColorXYZEstimate;




//    vec3 errors1[256];
////    vec3 errors2[256];
//    vec3 sqrtMaxResidualXYZ = sqrt(maxResidualXYZ);
//    float gammaInv = 1.0 / fittingGamma;
//    float minusTwoGammaInv =- 2.0 / fittingGamma;
//    float pow255Gamma = pow(255, fittingGamma);
//    int sq255 = 255 * 255;
//
//    for (int i = 0; i < VIEW_COUNT; i++)
//    {
//        vec3 view = normalize(getViewVector(i));
//
//        // Values of 1.0 for this color would correspond to the expected reflectance
//        // for an ideal diffuse reflector (diffuse albedo of 1), which is a reflectance of 1 / pi.
//        // Hence, this color corresponds to the reflectance times pi.
//        // Both the Phong model and the Cook Torrance with a Beckmann distribution also have a 1/pi factor.
//        // By adopting the convention that all reflectance values are scaled by pi in this shader,
//        // We can avoid division by pi here as well as the 1/pi factors in the parameterized models.
//        vec4 color = getLinearColor(i);
//
//        if (color.a * dot(view, normal) > 0)
//        {
//            LightInfo lightInfo = getLightInfo(i);
//            vec3 light = lightInfo.normalizedDirection;
//
//            vec3 colorRemainderRGB;
//
//#if USE_LIGHT_INTENSITIES
//            colorRemainderRGB = removeDiffuse(color, diffuseColor.rgb, light, lightInfo.attenuatedIntensity, specularNormal, maxLuminance).rgb
//                / lightInfo.attenuatedIntensity;
//#else
//            colorRemainderRGB = removeDiffuse(color, diffuseColor.rgb, light, vec3(1.0), specularNormal, maxLuminance).rgb;
//#endif
//
//            vec3 colorRemainderXYZ = rgbToXYZ(colorRemainderRGB);
//            vec3 colorRemainderXYZGamma = pow(colorRemainderXYZ, vec3(gammaInv));
//
//            float nDotV = max(0, dot(specularNormal, view));
//
//            vec3 halfway = normalize(view + light);
//            float nDotH = dot(halfway, specularNormal);
//            float nDotHSquared = nDotH * nDotH;
//
//            if (nDotV > 0 && nDotL > 0 && (fitNearSpecularOnly ? nDotHSquared > 0.5 : colorRemainderXYZ.y <= 1.0)
//                /* && nDotV * (1 + nDotHSquared) * (1 + nDotHSquared) > 1.0*/)
//            {
//                float hDotV = max(0, dot(halfway, view));
//                float geomRatio = min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV) / (4 * nDotV);
//                float inversesqrtGeom = inversesqrt(geomRatio);
//
//                vec3 a = nDotHSquared * 0.5 * inversesqrt(maxResidualXYZ * geomRatio);
//                vec3 b = (1 - nDotHSquared) * 2 * pow255Gamma * sqrtMaxResidualXYZ * inversesqrtGeom;
//
////                float c = nDotHSquared * inversesqrtGeom / sq255;
////                float d = (1 - nDotHSquared) * inversesqrtGeom * sq255;
//
//                for (int j = 1; j < 256; j++) // skip j=0 since it's indeterminate
//                {
//                    vec3 diff1 = (colorRemainderXYZGamma - pow(a + b / pow(j, fittingGamma), vec3(minusTwoGammaInv)));
//                    errors1[j] += diff1 * diff1;
//
////                    float jSq = j * j;
////                    vec3 diff2 = (colorRemainderXYZGamma - vec3(pow(c * jSq + d / jSq, minusTwoGammaInv)));
////                    errors2[j] += diff2 * diff2;
//                }
//            }
//        }
//    }
//
//    vec3 minError = vec3(999999.0);
//
//    ivec3 error1Cap = ivec3(floor(pow(min(vec3(1), 4 * maxResidualXYZ * MAX_ROUGHNESS), vec3(gammaInv)) * 255));
//    ivec3 error2Cap = ivec3(ceil(sqrt(sqrt(min(vec3(1), 0.25 / maxResidualXYZ))) * 255));
//
//    for (int i = 1; i < 256; i++) // skip i=0 since it's indeterminate
//    {
//        if (i <= error1Cap.x && errors1[i].x <= minError.x)
//        {
//            minError.x = errors1[i].x;
//            specularColorXYZEstimate.x = pow(i / 255.0, fittingGamma);
//            roughnessSquared.x = 0.25 * specularColorXYZEstimate.x / maxResidualXYZ.x;
//        }
//
//        if (i <= error1Cap.y && errors1[i].y <= minError.y)
//        {
//            minError.y = errors1[i].y;
//            specularColorXYZEstimate.y = pow(i / 255.0, fittingGamma);
//            roughnessSquared.y = 0.25 * specularColorXYZEstimate.y / maxResidualXYZ.y;
//        }
//
//        if (i <= error1Cap.z && errors1[i].z <= minError.z)
//        {
//            minError.z = errors1[i].z;
//            specularColorXYZEstimate.z = pow(i / 255.0, fittingGamma);
//            roughnessSquared.z = 0.25 * specularColorXYZEstimate.z / maxResidualXYZ.z;
//        }
//
////        if (i >= error2Cap.x && errors2[i].x < minError.x)
////        {
////            minError.x = errors2[i].x;
////            specularColorXYZEstimate.x = 1.0;
////
////            float sqrtRoughness = i / 255.0;
////            float roughnessEstimate = sqrtRoughness * sqrtRoughness;
////            roughnessSquared.x = roughnessEstimate * roughnessEstimate;
////        }
////
////        if (i >= error2Cap.y && errors2[i].y < minError.y)
////        {
////            minError.y = errors2[i].y;
////            specularColorXYZEstimate.y = 1.0;
////
////            float sqrtRoughness = i / 255.0;
////            float roughnessEstimate = sqrtRoughness * sqrtRoughness;
////            roughnessSquared.y = roughnessEstimate * roughnessEstimate;
////        }
////
////        if (i >= error2Cap.z && errors2[i].z < minError.z)
////        {
////            minError.z = errors2[i].z;
////            specularColorXYZEstimate.z = 1.0;
////
////            float sqrtRoughness = i / 255.0;
////            float roughnessEstimate = sqrtRoughness * sqrtRoughness;
////            roughnessSquared.z = roughnessEstimate * roughnessEstimate;
////        }
//    }





//    vec3 roughnessSums[3];
//    roughnessSums[0] = vec3(0);
//    roughnessSums[1] = vec3(0);
//    roughnessSums[2] = vec3(0);
//
//    vec2 altRoughnessSums = vec2(0);
//
//    vec4 sumResidualXYZGamma = vec4(0.0);
//
//    for (int i = 0; i < VIEW_COUNT; i++)
//    {
//        vec3 view = normalize(getViewVector(i));
//
//        // Values of 1.0 for this color would correspond to the expected reflectance
//        // for an ideal diffuse reflector (diffuse albedo of 1), which is a reflectance of 1 / pi.
//        // Hence, this color corresponds to the reflectance times pi.
//        // Both the Phong model and the Cook Torrance with a Beckmann distribution also have a 1/pi factor.
//        // By adopting the convention that all reflectance values are scaled by pi in this shader,
//        // We can avoid division by pi here as well as the 1/pi factors in the parameterized models.
//        vec4 color = getLinearColor(i);
//
//        if (color.a * dot(view, normal) > 0)
//        {
//            LightInfo lightInfo = getLightInfo(i);
//              vec3 light = lightInfo.normalizedDirection;
//
//              vec3 colorRemainderRGB;
//
//  #if USE_LIGHT_INTENSITIES
//              colorRemainderRGB = removeDiffuse(color, diffuseColor.rgb, light, lightInfo.attenuatedIntensity, specularNormal, maxLuminance).rgb
//                  / lightInfo.attenuatedIntensity;
//  #else
//              colorRemainderRGB = removeDiffuse(color, diffuseColor.rgb, light, vec3(1.0), specularNormal, maxLuminance).rgb;
//  #endif
//
//            vec3 colorRemainderXYZ = rgbToXYZ(colorRemainderRGB);
//
//            float nDotL = max(0, dot(light, specularNormal));
//            float nDotV = max(0, dot(specularNormal, view));
//
//            vec3 halfway = normalize(view + light);
//            float nDotH = dot(halfway, specularNormal);
//            float nDotHSquared = nDotH * nDotH;
//
//            if (nDotV > 0 && nDotL > 0 && (fitNearSpecularOnly ? nDotHSquared > 0.5 : colorRemainderXYZ.y <= 1.0) /* && nDotV * (1 + nDotHSquared) * (1 + nDotHSquared) > 1.0*/)
//            {
//                float hDotV = max(0, dot(halfway, view));
//
//                vec3 globalWeight = vec3(nDotV * (fitNearSpecularOnly ? 1.0 : clamp(9 - 10 * colorRemainderXYZ.y, 0, 1)));
////                    * sqrt(1 - nDotHSquared);
//
//#if LINEAR_WEIGHT_MODE
//                globalWeight *= colorRemainderXYZ;
//#elif PERCEPTUAL_WEIGHT_MODE
//                globalWeight *= pow(colorRemainderXYZ, vec3(1.0 / fittingGamma));
//#endif
//
//                vec3 perspectiveWeightedIntensity = colorRemainderXYZ * nDotV / min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV);
//                vec3 sqrtPerspectiveWeightedIntensity = sqrt(perspectiveWeightedIntensity);
//
//                roughnessSums[0] += globalWeight * sqrtPerspectiveWeightedIntensity * (1 - nDotHSquared);
//                roughnessSums[1] += globalWeight * sqrtPerspectiveWeightedIntensity * nDotHSquared;
//                roughnessSums[2] += globalWeight;
//
//                float scaledIntensity = 4 * perspectiveWeightedIntensity.y;
//                float scaledIntensityTimesHalfwayFactors = scaledIntensity * nDotHSquared * (nDotHSquared - 1);
//
//                float altRoughnessEstimate =
//                    (1 + 2 * scaledIntensityTimesHalfwayFactors - sqrt(1 + 4 * scaledIntensityTimesHalfwayFactors))
//                         / (2 * scaledIntensity * nDotHSquared * nDotHSquared);
//
//                altRoughnessSums[1] += roughnessSums[2].y * altRoughnessEstimate;
//                altRoughnessSums[0] += roughnessSums[1].y * altRoughnessEstimate;
//
//                sumResidualXYZGamma += nDotV * vec4(pow(colorRemainderXYZ, vec3(1.0 / fittingGamma)), 1.0);
//            }
//        }
//    }
//
//    if (roughnessSums[2] == vec3(0.0) || sumResidualXYZGamma.w == 0.0)
//    {
//        return ParameterizedFit(diffuseColor, vec4(diffuseNormalTS, 1), vec4(0), vec4(0));
//    }
//
//    //    Derivation:
//    //    maxResidual.rgb = xyzToRGB(rgbToXYZ(specularColor) * 1.0 / roughnessSquared) / 4;
//    //                    = xyzToRGB * (1.0 / roughnessSquared) * rgbToXYZ * specularColor / 4;
//    //    rgbToXYZ * maxResidual.rgb = (1.0 / roughnessSquared) * rgbToXYZ * specularColor / 4;
//    //    roughnessSquared * rgbToXYZ * maxResidual.rgb = rgbToXYZ * specularColor / 4;
//    //    4 * xyzToRGB * roughnessSquared * rgbToXYZ * maxResidual.rgb = specularColor;
//    if (chromaticRoughness)
//    {
//        vec3 sumWeights = max(vec3(0.0), sqrt(maxResidualXYZ) * roughnessSums[2] - roughnessSums[1]);
//
//        roughnessSquared = clamp(roughnessSums[0] / sumWeights,
//            MIN_ROUGHNESS * MIN_ROUGHNESS, /*min(0.25 / maxResidualComponent, */MAX_ROUGHNESS * MAX_ROUGHNESS/*)*/);
//        specularColorXYZEstimate = 4 * roughnessSquared * maxResidualXYZ;
//
//        if (relaxedSpecularPeaks && specularColorXYZEstimate.y > 1.0)
//        {
//            vec3 adjustedMaxResidual = maxResidualXYZ / (4 * maxResidualXYZ.y *
//                clamp((sqrt(maxResidualXYZ).y * altRoughnessSums[1] - altRoughnessSums[0]) / sumWeights.y,
//                                MIN_ROUGHNESS * MIN_ROUGHNESS, MAX_ROUGHNESS * MAX_ROUGHNESS));
//
//            roughnessSquared = clamp(roughnessSums[0] / max(vec3(0.0), sqrt(adjustedMaxResidual) * roughnessSums[2] - roughnessSums[1]),
//                MIN_ROUGHNESS * MIN_ROUGHNESS, min(0.25 / maxResidualComponent, MAX_ROUGHNESS * MAX_ROUGHNESS));
//
//            specularColorXYZEstimate = 4 * roughnessSquared * adjustedMaxResidual;
//        }
//    }
//    else
//    {
//        float sumWeights = max(0.0, sqrt(maxResidualLuminance[0]) * roughnessSums[2].y - roughnessSums[1].y);
//        float initRoughnessSquared = clamp(roughnessSums[0].y / sumWeights, MIN_ROUGHNESS * MIN_ROUGHNESS, MAX_ROUGHNESS * MAX_ROUGHNESS);
//
//        float reflectivityEstimate = 4 * initRoughnessSquared * maxResidualLuminance[0];
//        vec3 avgResidualXYZ = pow(sumResidualXYZGamma.xyz / sumResidualXYZGamma.w, vec3(fittingGamma));
//
//        if (relaxedSpecularPeaks && reflectivityEstimate > 1.0)
//        {
//            roughnessSquared = vec3(clamp((sqrt(maxResidualLuminance[0]) * altRoughnessSums[1] - altRoughnessSums[0]) / sumWeights,
//                MIN_ROUGHNESS * MIN_ROUGHNESS, MAX_ROUGHNESS * MAX_ROUGHNESS));
//
//            specularColorXYZEstimate = avgResidualXYZ / max(0.001, avgResidualXYZ.y);
//        }
//        else
//        {
//            roughnessSquared = vec3(initRoughnessSquared);
//            specularColorXYZEstimate = reflectivityEstimate * avgResidualXYZ / max(0.001, avgResidualXYZ.y);
//        }
//
//
////        // Force monochrome roughness and reflectivity (for debugging)
////        vec3 specularColor = 4 * roughnessSquared * maxResidualLuminance[0];
//    }






    vec3 specularSumA = vec3(0.0);
    vec3 specularSumB = vec3(0.0);

    vec4 sumResidualXYZGamma = vec4(0.0);

    for (int i = 0; i < VIEW_COUNT; i++)
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
            LightInfo lightInfo = getLightInfo(i);
            vec3 light = lightInfo.normalizedDirection;

            vec3 colorRemainderRGB;

#if USE_LIGHT_INTENSITIES
            colorRemainderRGB = removeDiffuse(color, diffuseColor.rgb, light, lightInfo.attenuatedIntensity, specularNormal, maxLuminance).rgb
                / lightInfo.attenuatedIntensity;
#else
            colorRemainderRGB = removeDiffuse(color, diffuseColor.rgb, light, vec3(1.0), specularNormal, maxLuminance).rgb;
#endif

            float nDotL = max(0, dot(light, specularNormal));
            float nDotV = max(0, dot(specularNormal, view));

            vec3 halfway = normalize(view + light);
            float nDotH = dot(halfway, specularNormal);
            float nDotHSquared = nDotH * nDotH;

            vec3 colorRemainderXYZ = rgbToXYZ(colorRemainderRGB);

            if (nDotV > 0 && nDotL > 0 && (fitNearSpecularOnly ? nDotHSquared > 0.5 : colorRemainderXYZ.y <= 1.0))
            {
                vec3 globalWeight = vec3(nDotV);

                vec3 commonFactor = colorRemainderXYZ * sqrt(colorRemainderXYZ * nDotV);


                vec3 numerator, denominator;
#if LINEAR_WEIGHT_MODE
                numerator = nDotV * (1 - nDotHSquared) * commonFactor;
                denominator = nDotV * (colorRemainderXYZ * sqrt(maxResidualXYZ) - nDotHSquared * commonFactor);
#else
                numerator = nDotV * pow((1 - nDotHSquared) * commonFactor, vec3(1.0 / fittingGamma));
                denominator = nDotV * pow(colorRemainderXYZ * sqrt(maxResidualXYZ) - nDotHSquared * commonFactor, vec3(1.0 / fittingGamma));
#endif

                specularSumA += numerator;
                specularSumB += denominator;

                sumResidualXYZGamma += nDotV * vec4(pow(colorRemainderXYZ, vec3(1.0 / fittingGamma)), 1.0);
            }
        }
    }

    if (sumResidualXYZGamma.w == 0.0)
    {
        return ParameterizedFit(diffuseColor, vec4(diffuseNormalTS, 1), vec4(0), vec4(0));
    }

    if (chromaticRoughness)
    {
        roughnessSquared = clamp(pow(specularSumA / specularSumB, vec3(fittingGamma)),
            MIN_ROUGHNESS * MIN_ROUGHNESS, MAX_ROUGHNESS * MAX_ROUGHNESS);
        specularColorXYZEstimate = clamp(4 * maxResidualXYZ * roughnessSquared, MIN_SPECULAR_REFLECTIVITY, 1.0);
    }
    else
    {
        roughnessSquared = vec3(clamp(pow(specularSumA.y / specularSumB.y, fittingGamma),
            MIN_ROUGHNESS * MIN_ROUGHNESS, MAX_ROUGHNESS * MAX_ROUGHNESS));
        specularColorXYZEstimate = clamp(4 * maxResidualXYZ.y * roughnessSquared, MIN_SPECULAR_REFLECTIVITY, 1.0)
            * pow(sumResidualXYZGamma.xyz / max(0.01 * sumResidualXYZGamma.w, sumResidualXYZGamma.y), vec3(fittingGamma));
    }





    // Convert the XYZ specular color to RGB, enforce monochrome constraints, and ensure a minimum specular reflectivity in cases where there may be
    // ambiguity between the specular and diffuse terms.
    // The roughness estimate may be updated as necessary for consistency.
    vec3 specularColor;

    if (diffuseColor.rgb != vec3(0.0) && MIN_SPECULAR_REFLECTIVITY > 0.0 && specularColorXYZEstimate.y < MIN_SPECULAR_REFLECTIVITY)
    {
        vec3 diffuseXYZ = rgbToXYZ(diffuseColor.rgb);

        if (chromaticRoughness)
        {
            vec3 specularColorBounded = max(specularColorXYZEstimate,
                min(min(MAX_ROUGHNESS_WHEN_CLAMPING * MAX_ROUGHNESS_WHEN_CLAMPING * (4 * maxResidualXYZ),
                        16 * diffuseXYZ * maxResidualXYZ + specularColorXYZEstimate),
                        vec3(MIN_SPECULAR_REFLECTIVITY)));

            roughnessSquared = clamp(specularColorBounded / (4 * maxResidualXYZ),
                vec3(MIN_ROUGHNESS * MIN_ROUGHNESS), vec3(MAX_ROUGHNESS_WHEN_CLAMPING * MAX_ROUGHNESS_WHEN_CLAMPING));

            specularColor = xyzToRGB(specularColorBounded);
        }
        else
        {
            if (chromaticSpecular)
            {
                vec3 specularColorBounded = max(specularColorXYZEstimate,
                    min(min(MAX_ROUGHNESS_WHEN_CLAMPING * MAX_ROUGHNESS_WHEN_CLAMPING * (4 * maxResidualXYZ),
                            16 * diffuseXYZ * maxResidualXYZ + specularColorXYZEstimate),
                            vec3(MIN_SPECULAR_REFLECTIVITY)));

                roughnessSquared = vec3(clamp(specularColorBounded.y / (4 * maxResidualLuminance[0]),
                    MIN_ROUGHNESS * MIN_ROUGHNESS, MAX_ROUGHNESS_WHEN_CLAMPING * MAX_ROUGHNESS_WHEN_CLAMPING));

                specularColor = xyzToRGB(specularColorBounded);
            }
            else
            {
                specularColor = vec3(max(specularColorXYZEstimate.y,
                    min(min(MAX_ROUGHNESS_WHEN_CLAMPING * MAX_ROUGHNESS_WHEN_CLAMPING * (4 * maxResidualLuminance[0]),
                            16 * diffuseXYZ.y * maxResidualLuminance[0] + specularColorXYZEstimate.y),
                            MIN_SPECULAR_REFLECTIVITY)));

                roughnessSquared = vec3(clamp(specularColor.g / (4 * maxResidualLuminance[0]),
                    MIN_ROUGHNESS * MIN_ROUGHNESS, MAX_ROUGHNESS_WHEN_CLAMPING * MAX_ROUGHNESS_WHEN_CLAMPING));
            }
        }
    }
    else
    {
        if (chromaticSpecular)
        {
            specularColor = xyzToRGB(specularColorXYZEstimate);
        }
        else
        {
            specularColor = vec3(specularColorXYZEstimate.y);
        }
    }

    // Refit the diffuse color using a simple linear regression after subtracting the final specular estimate.
    vec4 adjustedDiffuseColor;

    if (diffuseColor.rgb != vec3(0.0))
    {
        vec4 sumDiffuse = vec4(0.0);

        for (int i = 0; i < VIEW_COUNT; i++)
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
                LightInfo lightInfo = getLightInfo(i);
                vec3 light = lightInfo.normalizedDirection;

                float nDotL = max(0, dot(light, specularNormal));
                float nDotV = max(0, dot(specularNormal, view));

                vec3 halfway = normalize(view + light);
                float nDotH = dot(halfway, specularNormal);
                float nDotHSquared = nDotH * nDotH;

                if (nDotV > 0 && nDotL > 0 /*&& nDotHSquared > 0.5*/)
                {
                    vec3 q1 = roughnessSquared + (1.0 - nDotHSquared) / nDotHSquared;
                    vec3 mfdEval = roughnessSquared / (nDotHSquared * nDotHSquared * q1 * q1);

                    float hDotV = max(0, dot(halfway, view));
                    float geomRatio = min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV) / (4 * nDotV);

                    vec3 specularTerm = min(vec3(1), rgbToXYZ(specularColor)) * mfdEval * geomRatio;

#if USE_LIGHT_INTENSITIES
                    sumDiffuse += vec4(max(vec3(0.0), nDotL * (color.rgb / lightInfo.attenuatedIntensity - xyzToRGB(specularTerm))), nDotL * nDotL);
#else
                    sumDiffuse += vec4(max(vec3(0.0), nDotL * (color.rgb - xyzToRGB(specularTerm))), nDotL * nDotL);
#endif
                }
            }
        }

        if (sumDiffuse.a > 0.0)
        {
            adjustedDiffuseColor = vec4(sumDiffuse.rgb / sumDiffuse.a, 1);
        }
        else
        {
            // Discard and hole fill
            adjustedDiffuseColor = vec4(0.0);
        }
    }
    else
    {
        adjustedDiffuseColor = vec4(0, 0, 0, 1);
    }

    // Dividing by the sum of weights to get the weighted average.
    // We'll put a lower cap of 1/m^2 on the alpha we divide by so that noise doesn't get amplified
    // for texels where there isn't enough information at the specular peak.
    return ParameterizedFit(adjustedDiffuseColor, vec4(normalize(transpose(tangentToObject) * specularNormal), 1),
        vec4(specularColor, 1), vec4(sqrt(roughnessSquared), 1));
}

#endif // SPECULARFIT_GLSL
