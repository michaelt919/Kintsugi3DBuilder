#ifndef DIFFUSEFIT_GLSL
#define DIFFUSEFIT_GLSL

#include "../colorappearance/colorappearance.glsl"

#line 7 2000

uniform float delta;
uniform int iterations;
uniform float fit1Weight;
uniform float fit3Weight;

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

        for (int i = 0; i < viewCount; i++)
        {
            vec3 view = normalize(getViewVector(i));

            // Physically plausible values for the color components range from 0 to pi
            // We don't need to scale by 1 / pi because we would just need to multiply by pi again
            // at the end to get a diffuse albedo value.
            vec4 color = getLinearColor(i);

            float nDotV = dot(geometricNormal, view);
            if (color.a * nDotV > 0)
            {
                //vec4 light = vec4(getLightVector(i), 1.0);
                vec3 light = getLightVector(i);
                vec3 attenuatedIncidentRadiance = infiniteLightSources ?
                    getLightIntensity(i) : getLightIntensity(i) / (dot(light, light));
                vec3 lightNormalized = normalize(light);

                float weight = color.a * nDotV;
                if (k != 0)
                {
                    vec3 error = color.rgb - fit.color * dot(fit.normal, lightNormalized) * attenuatedIncidentRadiance;
                    weight *= exp(-dot(error,error)/(2*delta*delta));
                }

                float attenuatedLuminance = getLuminance(attenuatedIncidentRadiance);

                a += weight * outerProduct(lightNormalized, lightNormalized);
                //b += weight * outerProduct(lightNormalized, vec4(color.rgb / attenuatedIncidentRadiance, 0.0));
                b += weight * outerProduct(lightNormalized, color.rgb / attenuatedIncidentRadiance);
                weightedRadianceSum += weight * vec4(color.rgb, 1.0);
                weightedIrradianceSum += weight * attenuatedIncidentRadiance * max(0, dot(geometricNormal, lightNormalized));
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

            fit.color = clamp(weightedRadianceSum.rgb / max(max(rgbScale.r, rgbScale.g), rgbScale.b), 0, 1)
                                * fit3Quality
                            + clamp(weightedRadianceSum.rgb / weightedIrradianceSum, 0, 1)
                                * clamp(fit1Weight * weightedIrradianceSum, 0, 1 - fit3Quality);
            fit.normal = normalize(normalize(solution.xyz) * fit3Quality + fNormal * (1 - fit3Quality));
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
