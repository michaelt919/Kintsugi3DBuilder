#ifndef SPECULARPEAKFIT_GLSL
#define SPECULARPEAKFIT_GLSL

#line 5 2008

#ifndef CHROMATIC_SPECULAR
#define CHROMATIC_SPECULAR 1
#endif

#define MIN_SPECULAR_REFLECTIVITY 0.04

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D peakEstimate;

vec4 getDiffuseColor()
{
    vec4 textureResult = texture(diffuseEstimate, fTexCoord);
    return vec4(pow(textureResult.rgb, vec3(gamma)), textureResult.a);
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

vec4 getSpecularPeak()
{
    return texture(peakEstimate, fTexCoord); // already in linear color space
}

vec4 removeDiffuse(vec4 originalColor, vec3 diffuseColor, vec3 light,
                    vec3 attenuatedLightIntensity, vec3 normal, float maxLuminance)
{
    vec3 diffuseContrib = diffuseColor * max(0, dot(light, normal)) * attenuatedLightIntensity;

#if CHROMATIC_SPECULAR
    float cap = maxLuminance - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
    vec3 remainder = clamp(originalColor.rgb - diffuseContrib, 0, cap);
    return vec4(remainder, cap);
#else
    vec3 remainder = max(originalColor.rgb - diffuseContrib, vec3(0));
    float remainderMin = min(min(remainder.r, remainder.g), remainder.b);
    return vec4(vec3(remainderMin), 1.0);
#endif
}

struct ParameterizedFit
{
    vec4 diffuseColor;
    vec4 normal;
    vec4 specularColor;
    vec4 roughness;
    vec4 roughnessStdDev;
};

struct Residual
{
    vec3 color;
    float luminance;
    float weight;
    float rating;
    vec3 direction;
    float nDotV;
    int index;
};

vec3 brdf(vec3 normal, vec3 specularColor, float roughnessSq, Residual residual)
{
    float nDotH = max(0.0, dot(normal, residual.direction));
    float sqrtDenominator = (roughnessSq - 1) * nDotH * nDotH + 1;

    // Assume scaling by pi
    return specularColor * roughnessSq / (4 * residual.nDotV * sqrtDenominator * sqrtDenominator);
}

Residual getResidual(int index, vec3 diffuseColor, vec3 normal, float maxLuminance)
{
    Residual residual;

    vec4 color = getLinearColor(index);

    vec3 view = normalize(getViewVector(index));
    residual.nDotV = max(0.0, dot(normal, view));

    LightInfo lightInfo = getLightInfo(index);
    vec3 light = lightInfo.normalizedDirection;
    residual.direction = normalize(view + light);

    residual.color = removeDiffuse(color, diffuseColor, light, lightInfo.attenuatedIntensity, normal, maxLuminance).rgb
        / lightInfo.attenuatedIntensity;

//    // debug
//    float roughnessSq = 0.125 / getLuminance(getSpecularPeak().rgb);
//    float sqrtDenominator = (roughnessSq - 1) * residual.nDotV * residual.nDotV + 1;
//    residual.color = vec3(0.125 * roughnessSq / (residual.nDotV * sqrtDenominator * sqrtDenominator));

    residual.luminance = getLuminance(residual.color);
    residual.weight = color.a * clamp(sqrt(2) * residual.nDotV, 0, 1);
    residual.rating = residual.luminance * residual.weight;
    residual.index = index;

    return residual;
}

struct RoughnessEstimation
{
    float weightNumTimesDen;
    float weightDenSq;
};

RoughnessEstimation getRoughnessEstimation(Residual residual, float peakLuminance, vec3 normal)
{
    float nDotH = max(0.0, dot(normal, residual.direction));
    float nDotHSquared = nDotH * nDotH;

    float numerator = sqrt(max(0.0, (1 - nDotHSquared) * sqrt(residual.luminance * residual.nDotV)));
    float denominatorSq = max(0.0, sqrt(peakLuminance) - nDotHSquared * sqrt(residual.luminance * residual.nDotV));
    float denominator = sqrt(denominatorSq);

    return RoughnessEstimation(residual.weight * numerator * denominator, residual.weight * denominatorSq);
}

float estimateRoughness(Residual maxResiduals[7], float peakLuminance, vec3 normal)
{
    RoughnessEstimation estimation[6];
    estimation[0] = getRoughnessEstimation(maxResiduals[0], peakLuminance, normal);
    estimation[1] = getRoughnessEstimation(maxResiduals[1], peakLuminance, normal);
    estimation[2] = getRoughnessEstimation(maxResiduals[2], peakLuminance, normal);
    estimation[3] = getRoughnessEstimation(maxResiduals[3], peakLuminance, normal);
    estimation[4] = getRoughnessEstimation(maxResiduals[4], peakLuminance, normal);
    estimation[5] = getRoughnessEstimation(maxResiduals[5], peakLuminance, normal);

    return (estimation[0].weightNumTimesDen + estimation[1].weightNumTimesDen + estimation[2].weightNumTimesDen
            + estimation[3].weightNumTimesDen + estimation[4].weightNumTimesDen + estimation[5].weightNumTimesDen)
        / (estimation[0].weightDenSq + estimation[1].weightDenSq + estimation[2].weightDenSq
            + estimation[3].weightDenSq + estimation[4].weightDenSq + estimation[5].weightDenSq);
}

float computeEnergy(Residual maxResiduals[7], vec3 specularPeak, float peakLuminance, vec3 normal)
{
    float nDotH = max(0.0, dot(normal, maxResiduals[0].direction));
    float nDotHSquared = nDotH * nDotH;

    float roughness = estimateRoughness(maxResiduals, peakLuminance, normal);
    float roughnessSq = roughness * roughness;
    vec3 specularColor = 4 * specularPeak * roughnessSq;

    vec3 diffs[5];
    diffs[0] = brdf(normal, specularColor, roughnessSq, maxResiduals[1]) - maxResiduals[1].color;
    diffs[1] = brdf(normal, specularColor, roughnessSq, maxResiduals[2]) - maxResiduals[2].color;
    diffs[2] = brdf(normal, specularColor, roughnessSq, maxResiduals[3]) - maxResiduals[3].color;
    diffs[3] = brdf(normal, specularColor, roughnessSq, maxResiduals[4]) - maxResiduals[4].color;
    diffs[4] = brdf(normal, specularColor, roughnessSq, maxResiduals[5]) - maxResiduals[5].color;

    return dot(diffs[0], diffs[0]) * maxResiduals[1].weight +
        dot(diffs[1], diffs[1]) * maxResiduals[2].weight +
        dot(diffs[2], diffs[2]) * maxResiduals[3].weight +
        dot(diffs[3], diffs[3]) * maxResiduals[4].weight +
        dot(diffs[4], diffs[4]) * maxResiduals[5].weight;
}

vec3 estimateNormal(vec4 diffuseColor, vec3 diffuseNormal, vec3 geometricNormal)
{
    float maxLuminance = getMaxLuminance();
    vec2 maxResidualLuminance = vec2(0);
    vec4 maxResidualDirection = vec4(0);

    vec3 directionSum = vec3(0);
    vec3 intensityWeightedDirectionSum = vec3(0);

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        vec4 color = getLinearColor(i);
        vec3 view = normalize(getViewVector(i));
        float nDotV = dot(view, geometricNormal);

        if (color.a * nDotV > 0)
        {
            LightInfo lightInfo = getLightInfo(i);
            vec3 light = lightInfo.normalizedDirection;

            vec3 colorRemainder;

#if USE_LIGHT_INTENSITIES
            colorRemainder = removeDiffuse(color, diffuseColor.rgb, light, lightInfo.attenuatedIntensity, diffuseNormal, maxLuminance).rgb
                / lightInfo.attenuatedIntensity;
#else
            colorRemainder = removeDiffuse(color, diffuseColor.rgb, light, vec3(1.0), diffuseNormal, maxLuminance).rgb;
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
            }
            else if (maxResidualLuminance[0] * 10 <= 9 && luminance * weight > maxResidualLuminance[0] * maxResidualLuminance[1])
            {
                maxResidualLuminance = vec2(luminance, weight);
            }
        }
    }

    if (dot(intensityWeightedDirectionSum, intensityWeightedDirectionSum) < 1.0)
    {
        intensityWeightedDirectionSum += (1 - length(intensityWeightedDirectionSum)) * diffuseNormal;
    }

    vec3 biasedHeuristicNormal;
    vec3 bias;
    float resolvability;

    biasedHeuristicNormal = normalize(intensityWeightedDirectionSum);
    float directionScale = length(directionSum);
    resolvability = min(1, directionScale);
    bias = directionSum / max(1, directionScale);


    vec3 heuristicNormal;

    float specularNormalFidelity = dot(bias, geometricNormal);                                                      // correlation between biased average (either normalized or between 0-1) and geometric normal (normalized)
    vec3 certaintyDirectionUnnormalized = cross(bias - specularNormalFidelity * geometricNormal, geometricNormal);  // component of biased average orthogonal to geometric normal
    vec3 certaintyDirection = certaintyDirectionUnnormalized                                                        // points in direction of component of biased average orthogonal to geometric normal
        / max(1, length(certaintyDirectionUnnormalized));                                                           // length is between 0 (no bias) and 1 (biased average completely orthogonal to geometric normal)

    float specularNormalCertainty =                                                                                 // correlation between biased normal and the scaled "certainty" direction
        resolvability * dot(biasedHeuristicNormal, certaintyDirection);                                             // range is between 0 (no bias or singular conditions) and 1 (orthogonally biased)
    vec3 scaledCertaintyDirection = specularNormalCertainty * certaintyDirection;                                   // projection of biased normal onto scaled "certainty" direction
                                                                                                                    // length is between 0 (no bias) and 1 (biased average completely orthogonal to geometric normal)

    heuristicNormal = normalize(                                                                                    // normalize the following:
        scaledCertaintyDirection                                                                                    // projection of biased normal onto scaled "certainty" direction
            + sqrt(1 - specularNormalCertainty * specularNormalCertainty                                            // length of vector which, if orthogonal to the preceding projection,
                        * dot(certaintyDirection, certaintyDirection))                                              // is such that their sum has length 1
                * normalize(mix(geometricNormal, normalize(biasedHeuristicNormal - scaledCertaintyDirection),       // mix between the geometric normal and the component of the potentially biased normal orthogonal to the scaled "certainty" direction
                   resolvability * specularNormalFidelity)));                                                       // using the non-singularity and the correlation between the biased average and the geometric normal
                                                                                                                    // as the basis for whether to select the potentially biased normal.

    return maxResidualDirection.xyz + (1 - maxResidualDirection.w) * heuristicNormal;
}

ParameterizedFit fitSpecular()
{
    vec3 normal = normalize(fNormal);

    vec3 tangent = normalize(fTangent - dot(normal, fTangent) * normal);
    vec3 bitangent = normalize(fBitangent
        - dot(normal, fBitangent) * normal
        - dot(tangent, fBitangent) * tangent);

    mat3 tangentToObject = mat3(tangent, bitangent, normal);
    vec3 diffuseNormalTS = getDiffuseNormalVector();
    vec3 oldDiffuseNormal = tangentToObject * diffuseNormalTS;

    vec4 diffuseColor = getDiffuseColor();
    vec4 specularPeak = getSpecularPeak();
    float peakLuminance = getLuminance(specularPeak.rgb);
    float maxLuminance = getMaxLuminance();

    ParameterizedFit result;

    if (specularPeak.a == 0.0)
    {
        return result;
    }

    Residual maxResiduals[7];

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        Residual residual = getResidual(i, diffuseColor.rgb, oldDiffuseNormal, maxLuminance);

        if (residual.rating > maxResiduals[0].rating)
        {
            maxResiduals[6] = maxResiduals[5];
            maxResiduals[5] = maxResiduals[4];
            maxResiduals[4] = maxResiduals[3];
            maxResiduals[3] = maxResiduals[2];
            maxResiduals[2] = maxResiduals[1];
            maxResiduals[1] = maxResiduals[0];
            maxResiduals[0] = residual;
        }
        else if (residual.rating > maxResiduals[1].rating)
        {
            maxResiduals[6] = maxResiduals[5];
            maxResiduals[5] = maxResiduals[4];
            maxResiduals[4] = maxResiduals[3];
            maxResiduals[3] = maxResiduals[2];
            maxResiduals[2] = maxResiduals[1];
            maxResiduals[1] = residual;
        }
        else if (residual.rating > maxResiduals[2].rating)
        {
            maxResiduals[6] = maxResiduals[5];
            maxResiduals[5] = maxResiduals[4];
            maxResiduals[4] = maxResiduals[3];
            maxResiduals[3] = maxResiduals[2];
            maxResiduals[2] = residual;
        }
        else if (residual.rating > maxResiduals[3].rating)
        {
            maxResiduals[6] = maxResiduals[5];
            maxResiduals[5] = maxResiduals[4];
            maxResiduals[4] = maxResiduals[3];
            maxResiduals[3] = residual;
        }
        else if (residual.rating > maxResiduals[4].rating)
        {
            maxResiduals[6] = maxResiduals[5];
            maxResiduals[5] = maxResiduals[4];
            maxResiduals[4] = residual;
        }
        else if (residual.rating > maxResiduals[5].rating)
        {
            maxResiduals[6] = maxResiduals[5];
            maxResiduals[5] = residual;
        }
        else if (residual.rating > maxResiduals[6].rating)
        {
            maxResiduals[6] = residual;
        }
    }

// TODO experiment with weights

////    maxResiduals[0].weight = 256.0;
//    maxResiduals[0].weight *= max(1.0 / 256.0, maxResiduals[0].rating - maxResiduals[6].rating);
//
//    float rating1MinusRating6 = max(1.0 / 256.0, maxResiduals[1].rating - maxResiduals[6].rating);
//    maxResiduals[1].weight *= rating1MinusRating6 ;// / max(rating1MinusRating6 / 256.0, maxResiduals[0].rating - maxResiduals[1].rating);
//
//    float rating2MinusRating6 = max(1.0 / 256.0, maxResiduals[2].rating - maxResiduals[6].rating);
//    float rating0MinusRating2 = max(rating2MinusRating6 / 256.0, maxResiduals[0].rating - maxResiduals[2].rating);
//    maxResiduals[2].weight *= rating2MinusRating6 ;// / rating0MinusRating2;
//
//    float rating0MinusRating3 = max(rating2MinusRating6 / 256.0, maxResiduals[0].rating - maxResiduals[3].rating);
//    maxResiduals[3].weight *= (maxResiduals[3].rating - maxResiduals[6].rating) ;// * rating0MinusRating2 / (rating0MinusRating3 * rating0MinusRating3);
//
//    float rating0MinusRating4 = max(rating2MinusRating6 / 256.0, maxResiduals[0].rating - maxResiduals[4].rating);
//    maxResiduals[4].weight *= (maxResiduals[4].rating - maxResiduals[6].rating) ;// * rating0MinusRating2 / (rating0MinusRating4 * rating0MinusRating4);
//
//    float rating0MinusRating5 = max(rating2MinusRating6 / 256.0, maxResiduals[0].rating - maxResiduals[5].rating);
//    maxResiduals[5].weight *= (maxResiduals[5].rating - maxResiduals[6].rating) ;// * rating0MinusRating2 / (rating0MinusRating5 * rating0MinusRating5);
//
//    maxResiduals[6].weight = 0;

    result.diffuseColor = diffuseColor;




//    ivec2 bestBlock;
//    vec3 bestNormal;
//    float minEnergy;
//
//    for (int i = 0; i < 16; i++)
//    {
//        for (int j = 0; j < 16; j++)
//        {
//            float nx = (i * 16.0 + 7.5) * 2 / 255.0 - 1;
//            float ny = (j * 16.0 + 7.5) * 2 / 255.0 - 1;
//            vec3 normal = tangentToObject * vec3(nx, ny, sqrt(1 - nx*nx - ny*ny));
//
//            float energy = computeEnergy(maxResiduals, specularPeak.rgb, peakLuminance, normal);
//            if (energy < minEnergy)
//            {
//                minEnergy = energy;
//                bestBlock = ivec2(i, j);
//                bestNormal = normal;
//            }
//        }
//    }
//
//    for (int i = 0; i < 16; i++)
//    {
//        for (int j = 0; j < 16; j++)
//        {
//            float nx = (bestBlock[0] * 16.0 + i) * 2 / 255.0 - 1;
//            float ny = (bestBlock[1] * 16.0 + j) * 2 / 255.0 - 1;
//            vec3 normal = tangentToObject * vec3(nx, ny, sqrt(1 - nx*nx - ny*ny));
//
//            float energy = computeEnergy(maxResiduals, specularPeak.rgb, peakLuminance, normal);
//            if (energy < minEnergy)
//            {
//                minEnergy = energy;
//                bestNormal = normal;
//            }
//        }
//    }

//    vec3 bestNormal = oldDiffuseNormal;
    vec3 bestNormal = estimateNormal(diffuseColor, oldDiffuseNormal, normal);

    vec3 normalObjSpace = mix(bestNormal, maxResiduals[0].direction,
        clamp((maxResiduals[0].weight * maxResiduals[0].luminance - maxResiduals[1].weight * maxResiduals[1].luminance)
            / ((peakLuminance - maxResiduals[1].weight * maxResiduals[1].luminance)), 0, 1));

    result.normal = vec4(transpose(tangentToObject) * normalObjSpace, 1.0);


    result.roughness = vec4(vec3(max(sqrt(0.25 * MIN_SPECULAR_REFLECTIVITY / getLuminance(specularPeak.rgb)),
        estimateRoughness(maxResiduals, peakLuminance, normalObjSpace))), 1.0);
    result.specularColor = vec4(4 * specularPeak.rgb * result.roughness[0] * result.roughness[0], 1.0);

//    result.roughness = vec4(vec3(sqrt(0.01 / specularPeak.rgb)), 1.0);
//    result.specularColor = vec4(0.04, 0.04, 0.04, 1.0);

    return result;
}

#endif // SPECULARPEAKFIT_GLSL
