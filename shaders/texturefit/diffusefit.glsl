#ifndef DIFFUSEFIT_GLSL
#define DIFFUSEFIT_GLSL

#include "../colorappearance/colorappearance.glsl"

#line 7 2000

uniform float delta;
uniform int iterations;
uniform float fit1Weight;
uniform float fit3Weight;

#define SQRT2 1.4142135623730950488016887242097

struct DiffuseFit
{
    vec3 color;
    vec3 normal;
    vec3 normalTS;
    vec3 ambient;
};

bool validateFit(DiffuseFit fit)
{
    return ! isnan(fit.color.r) && ! isnan(fit.color.g) && ! isnan(fit.color.b) &&
            ! isinf(fit.color.r) && ! isinf(fit.color.g) && ! isinf(fit.color.b) &&
            ! isnan(fit.normal.x) && ! isnan(fit.normal.y) && ! isnan(fit.normal.z) &&
            ! isinf(fit.normal.x) && ! isinf(fit.normal.y) && ! isinf(fit.normal.z) &&
            ! isnan(fit.ambient.r) && ! isnan(fit.ambient.g) && ! isnan(fit.ambient.b) &&
            ! isinf(fit.ambient.r) && ! isinf(fit.ambient.g) && ! isinf(fit.ambient.b);
}

DiffuseFit fitDiffuse()
{
    vec3 geometricNormal = normalize(fNormal);
    
    DiffuseFit fit = DiffuseFit(vec3(0), vec3(0), vec3(0), vec3(0));
    
    for (int k = 0; k < iterations; k++)
    {
        //mat4 a = mat4(0);
        //mat4 b = mat4(0);
        mat3 a = mat3(0);
        mat3 b = mat3(0);
        vec4 weightedRadianceSum = vec4(0.0);
        vec3 weightedIrradianceSum = vec3(0.0);
        vec3 directionSum = vec3(0);

        for (int i = 0; i < VIEW_COUNT; i++)
        {
            vec3 view = normalize(getViewVector(i));

            // Physically plausible values for the color components range from 0 to pi
            // We don't need to scale by 1 / pi because we would just need to multiply by pi again
            // at the end to get a diffuse albedo value.
            vec4 color = getLinearColor(i);

            float nDotV = dot(geometricNormal, view);
            if (color.a * nDotV > 0)
            {
                LightInfo lightInfo = getLightInfo(i);
                vec3 light = lightInfo.normalizedDirection;

                float weight = color.a * nDotV;
                if (k != 0)
                {
                    vec3 error = color.rgb - fit.color * dot(fit.normal, light) * lightInfo.attenuatedIntensity;
                    weight *= exp(-dot(error,error)/(2*delta*delta));
                }

                float attenuatedLuminance = getLuminance(lightInfo.attenuatedIntensity);

                a += weight * outerProduct(lightNormalized, lightNormalized);
                //b += weight * outerProduct(lightNormalized, vec4(color.rgb / lightInfo.attenuatedIntensity, 0.0));
                b += weight * outerProduct(lightNormalized, color.rgb / lightInfo.attenuatedIntensity);

                float nDotL = max(0, dot(geometricNormal, lightNormalized));
                weightedRadianceSum += weight * vec4(color.rgb, 1.0) * nDotL;
                weightedIrradianceSum += weight * lightInfo.attenuatedIntensity * nDotL * nDotL;

                directionSum += light;
            }
        }

        if (fit3Weight > 0.0)
        {
            mat3 m = inverse(a) * b;
            vec3 rgbFit = vec3(length(m[0]), length(m[1]), length(m[2]));
            vec3 rgbScale = weightedRadianceSum.rgb / rgbFit;

            if (rgbFit.r == 0.0)
            {
                rgbScale.r = 0.0;
            }
            if (rgbFit.g == 0.0)
            {
                rgbScale.g = 0.0;
            }
            if (rgbFit.b == 0.0)
            {
                rgbScale.b = 0.0;
            }

            //vec4 solution = m * vec4(rgbWeights, 0.0);
            vec3 solution = m * rgbScale;

            //float ambientIntensity = solution.w;

            float fit3Quality = clamp(fit3Weight * determinant(a) / weightedRadianceSum.a *
                                    clamp(dot(normalize(solution.xyz), geometricNormal), 0, 1), 0.0, 1.0);

            vec3 geometricNormal = normalize(fNormal);

            fit.color = clamp(weightedRadianceSum.rgb / max(max(rgbScale.r, rgbScale.g), rgbScale.b), 0, 1)
                                * fit3Quality
                            + clamp(weightedRadianceSum.rgb / weightedIrradianceSum, 0, 1)
                                * clamp(fit1Weight * weightedIrradianceSum, 0, 1 - fit3Quality);
            vec3 diffuseNormalEstimate = normalize(normalize(solution.xyz) * fit3Quality + geometricNormal * (1 - fit3Quality));

            float directionScale = length(directionSum);
            vec3 averageDirection = directionSum / max(1, directionScale);

            // sqrt2 / 2 corresponds to 45 degrees between the geometric surface normal and the average view direction.
            // This is the point at which there is essentially no information that can be obtained about the true surface normal in one dimension
            // since all of the views are presumably on the same side of the surface normal.
            float diffuseNormalFidelity = max(0, (dot(averageDirection, geometricNormal) - SQRT2 / 2) / (1 - SQRT2 / 2));

            vec3 certaintyDirectionUnnormalized = cross(averageDirection - diffuseNormalFidelity * geometricNormal, geometricNormal);
            vec3 certaintyDirection = certaintyDirectionUnnormalized
                / max(1, length(certaintyDirectionUnnormalized));

            float diffuseNormalCertainty =
                min(1, directionScale) * dot(diffuseNormalEstimate, certaintyDirection);
            vec3 scaledCertaintyDirection = diffuseNormalCertainty * certaintyDirection;
            fit.normal = normalize(
                scaledCertaintyDirection
                    + sqrt(1 - diffuseNormalCertainty * diffuseNormalCertainty
                                * dot(certaintyDirection, certaintyDirection))
                        * normalize(mix(geometricNormal, normalize(diffuseNormalEstimate - scaledCertaintyDirection),
                            min(1, directionScale) * diffuseNormalFidelity)));

            //debug = vec4(fit3Quality,
            //    clamp(fit1Weight * weightedIrradianceSum, 0, 1 - fit3Quality), 0.0, 1.0);
        }
        else
        {
            fit.color = clamp(weightedRadianceSum.rgb / weightedIrradianceSum, 0, 1)
                                * clamp(fit1Weight * weightedIrradianceSum, 0, 1);
            fit.normal = fNormal;
        }
    }

    if (!validateFit(fit))
    {
        fit.color = vec3(0.0);
        fit.normal = vec3(0.0);
        fit.ambient = vec3(0.0);
    }
    else
    {
        vec3 tangent = normalize(fTangent - dot(geometricNormal, fTangent));
        vec3 bitangent = normalize(fBitangent
            - dot(geometricNormal, fBitangent) * geometricNormal 
            - dot(tangent, fBitangent) * tangent);
            
        mat3 tangentToObject = mat3(tangent, bitangent, geometricNormal);
        mat3 objectToTangent = transpose(tangentToObject);
    
        fit.normalTS = objectToTangent * fit.normal;
    }
    
    return fit;
}

#endif // DIFFUSEFIT_GLSL
