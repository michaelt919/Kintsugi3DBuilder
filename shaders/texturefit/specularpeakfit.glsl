#ifndef SPECULARPEAKFIT_GLSL
#define SPECULARPEAKFIT_GLSL

#line 5 2008

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
    vec4 textureResult = texture(peakEstimate, fTexCoord);
    return vec4(pow(textureResult.rgb, vec3(gamma)), textureResult.a);
}

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
    vec3 direction;
    float nDotV;
    int index;
};

Residual getResidual(int index, vec3 diffuseColor, vec3 normal, vec3 maxLuminance)
{
    Residual residual;

    vec4 color = getLinearColor(i);
    vec3 view = normalize(getViewVector(i));

    residual.nDotV = dot(view, normal);

    LightInfo lightInfo = getLightInfo(index);
    vec3 light = lightInfo.normalizedDirection;

    residual.color = removeDiffuse(color, diffuseColor, light, lightInfo.attenuatedIntensity, normal, maxLuminance).rgb
        / lightInfo.attenuatedIntensity;

    residual.luminance = getLuminance(residual.color);
    residual.weight = color.a * clamp(sqrt(2) * residual.nDotV, 0, 1);
    residual.direction = normalize(view + light);
    residual.index = index;

    return residual;
}

vec3 brdf(vec3 normal, vec3 specularColor, float roughnessSq, Residual residual)
{
    float nDotH = max(0.0, dot(normal, residual.direction));
    float sqrtDenominator = (roughnessSq - 1) * nDotH * nDotH + 1;

    // Assume scaling by pi
    return specularColor * roughnessSq / (4 * residual.nDotV * sqrtDenominator * sqrtDenominator);
}

float computeEnergy(Residual maxResiduals[4], vec3 normal)
{
    float nDotH = max(0.0, dot(normal, maxResiduals[0].direction));
    float nDotHSquared = nDotH * nDotH;

    float numerator = sqrt(max(0.0, (1 - nDotHSquared) * sqrt(maxResiduals[0].luminance * maxResiduals[0].nDotV)));
    float denominatorSq = max(0.0, sqrt(peakEstimate) - nDotHSquared * sqrt(maxResiduals[0].luminance * maxResiduals[0].nDotV));
    float denominator = sqrt(denominatorSq);
    float roughnessSq = numerator / denominator;

    float roughness = sqrt(roughnessSq);
    vec3 specularColor = 4 * specularPeak * roughnessSq;

    vec3 diffs[3];
    diffs[0] = brdf(normal, specularColor, roughnessSq, maxResiduals[1]) - maxResiduals[1].color;
    diffs[1] = brdf(normal, specularColor, roughnessSq, maxResiduals[1]) - maxResiduals[2].color;
    diffs[2] = brdf(normal, specularColor, roughnessSq, maxResiduals[1]) - maxResiduals[3].color;

    return dot(diffs[0], diffs[0]) * maxResiduals[1].weight +
        dot(diffs[1], diffs[1]) * maxResiduals[2].weight +
        dot(diffs[2], diffs[2]) * maxResiduals[3].weight;
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

    float maxLuminance = getMaxLuminance();

    Residual maxResiduals[4];

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        Residual residual = getResidual(i, diffuseColor.rgb, oldDiffuseNormal, maxLuminance);

        if (residual.luminance * residual.weight > maxResiduals[0].luminance * maxResiduals[0].weight)
        {
            maxResiduals[0] = residual;
        }
    }

    float distanceThreshold = 2.0;
    for (int i = 0; i < VIEW_COUNT; i++)
    {
        if (i != maxResiduals[0].index)
        {
            distanceThreshold = min(distanceThreshold,
                length(normalize(getLightInfo(i).normalizedDirection + normalize(getViewVector(i))) - maxResiduals[0].direction));
        }
    }

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        Residual residual = getResidual(i, diffuseColor.rgb, oldDiffuseNormal, maxLuminance);
        residual.weight *= clamp(length(residual.direction - maxResiduals[0].direction) / distanceThreshold + 1.0, 0, 1);

        if (residual.luminance * residual.weight > maxResiduals[1].luminance * maxResiduals[1].weight)
        {
            maxResiduals[1] = residual;
        }
    }

    vec3 v = normalize(maxResiduals[1].direction - maxResiduals[0].direction);

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        Residual residual = getResidual(i, diffuseColor.rgb, oldDiffuseNormal, maxLuminance);
        residual.weight *= clamp(length(residual.direction - maxResiduals[0].direction) / distanceThreshold + 1.0, 0, 1);

        float alignment = dot(normalize(residual.direction - maxResiduals[0].direction), v);
        residual.weight *= 1.0 - alignment * alignment;

        if (residual.luminance * residual.weight > maxResiduals[2].luminance * maxResiduals[2].weight)
        {
            maxResiduals[2] = residual;
        }
        if (residual.luminance * residual.weight > maxResiduals[2].luminance * maxResiduals[2].weight)
        {
            maxResiduals[2] = residual;
        }
    }

    ParameterizedFit result;

    result.diffuseColor = diffuseColor;
//    result.normal = vec4(mix(oldDiffuseNormal, maxResiduals[0].direction,
//        clamp((maxResiduals[0].luminance - maxResiduals[1].luminance) / (specularPeak - maxResiduals[1].luminance), 0, 1)), 1.0);

    ivec2 bestBlock;

    for (int i = 0; i < 16; i++)
    {
        for (int j = 0; j < 16; j++)
        {
            float nx = (i * 16.0 + 3.5) / 255.0;
            float ny = (j * 16.0 + 3.5) / 255.0;
            vec3 normal = tangentToObject * vec3(nx, ny, sqrt(1 - nx*nx - ny*ny));


        }
    }

    float nDotH = max(0.0, dot(result.normal, maxResiduals[0].direction));
    float nDotHSquared = nDotH * nDotH;

    float numerator = sqrt(max(0.0, (1 - nDotHSquared) * sqrt(maxResiduals[0].luminance * maxResiduals[0].nDotV)));
    float denominatorSq = max(0.0, sqrt(peakEstimate) - nDotHSquared * sqrt(maxResiduals[0].luminance * maxResiduals[0].nDotV));
    float denominator = sqrt(denominatorSq);
    float roughnessSq = numerator / denominator;

    result.roughness = sqrt(roughnessSq);
    result.specularColor = 4 * specularPeak * roughnessSq;

    return result;
}

#endif // SPECULARPEAKFIT_GLSL
