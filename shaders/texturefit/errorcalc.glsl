#ifndef ERRORCALC_GLSL
#define ERRORCALC_GLSL

#include "../colorappearance/colorappearance.glsl"

#line 7 2006

#define MAX_ERROR 3.402822E38 // Max 32-bit floating-point is 3.4028235E38
#define MIN_DAMPING_FACTOR 0.0078125 // 1/256
#define MAX_DAMPING_FACTOR 1024 // 1048576 // 2^20
//#define MIN_SHIFT_FRACTION 0.01171875 // 3/256

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D specularEstimate;
uniform sampler2D roughnessEstimate;
uniform sampler2D errorTexture;

uniform float fittingGamma;

vec3 getDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

vec3 getSpecularColor()
{
    return pow(texture(specularEstimate, fTexCoord).rgb, vec3(gamma));
}

float getRoughness()
{
    return texture(roughnessEstimate, fTexCoord).r;
}

struct ErrorResult
{
    bool mask;
    float dampingFactor;
    float sumSqError;
};

ErrorResult calculateError()
{
    vec4 prevErrorResult = texture(errorTexture, fTexCoord);

    if (prevErrorResult.x < MIN_DAMPING_FACTOR || prevErrorResult.x > MAX_DAMPING_FACTOR)
    {
        return ErrorResult(false, 0.0, prevErrorResult.y);
    }
    else
    {
        vec3 normal = normalize(fNormal);

        vec3 tangent = normalize(fTangent - dot(normal, fTangent));
        vec3 bitangent = normalize(fBitangent
            - dot(normal, fBitangent) * normal 
            - dot(tangent, fBitangent) * tangent);
            
        mat3 tangentToObject = mat3(tangent, bitangent, normal);
        vec3 shadingNormal = tangentToObject * getDiffuseNormalVector();

        vec3 diffuseColor = rgbToXYZ(getDiffuseColor());
        vec3 specularColor = rgbToXYZ(getSpecularColor());
        float roughness = getRoughness();
        float roughnessSquared = roughness * roughness;
        float maxLuminance = getMaxLuminance();
        float fittingGammaInv = 1.0 / fittingGamma;

        float sumSqError = 0.0;

        for (int i = 0; i < viewCount; i++)
        {
            vec3 view = normalize(getViewVector(i));
            float nDotV = max(0, dot(shadingNormal, view));
            vec4 color = getLinearColor(i);

            if (color.a > 0 && nDotV > 0 && dot(normal, view) > 0)
            {
                vec3 lightPreNormalized = getLightVector(i);
                vec3 attenuatedLightIntensity = infiniteLightSources ?
                    getLightIntensity(i) :
                    getLightIntensity(i) / (dot(lightPreNormalized, lightPreNormalized));
                vec3 light = normalize(lightPreNormalized);
                float nDotL = max(0, dot(light, shadingNormal));

                vec3 halfway = normalize(view + light);
                float nDotH = dot(halfway, shadingNormal);

                if (nDotL > 0.0 && nDotH > sqrt(0.5))
                {
                    float nDotHSquared = nDotH * nDotH;

                    float q1 = roughnessSquared + (1.0 - nDotHSquared) / nDotHSquared;
                    float mfdEval = roughnessSquared / (nDotHSquared * nDotHSquared * q1 * q1);

                    float hDotV = max(0, dot(halfway, view));
                    float geomRatio = min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV) / (4 * nDotV);

                    vec3 colorScaled = pow(rgbToXYZ(color.rgb / attenuatedLightIntensity),
                        vec3(fittingGammaInv));
                    vec3 currentFit =
                        diffuseColor
                        * nDotL + specularColor * mfdEval * geomRatio;
                    vec3 colorResidual = colorScaled - pow(currentFit, vec3(fittingGammaInv));

                    float weight = 1.0;//clamp(1 / (1 - nDotHSquared), 0, 1000000);
                    sumSqError += weight * dot(colorResidual, colorResidual);
                }
            }
        }

        sumSqError = min(sumSqError, MAX_ERROR);

        if (sumSqError >= prevErrorResult.y)
        {
            //return ErrorResult(false, prevErrorResult.x / 2, prevErrorResult.y);
            return ErrorResult(false, prevErrorResult.x * 2, prevErrorResult.y);
        }
        else
        {
            //return ErrorResult(true, prevErrorResult.x, sumSqError);
            return ErrorResult(true, prevErrorResult.x / 2, sumSqError);
        }
    }
}

#endif // ERRORCALC_GLSL
