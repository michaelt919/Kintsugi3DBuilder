#ifndef RESID_GLSL
#define RESID_GLSL

#include "reflectanceequations.glsl"

#line 7 2101

uniform sampler2D diffuseMap;
uniform sampler2D specularMap;
uniform sampler2D roughnessMap;

uniform mat4 model_view;
uniform bool diffuseMode;

#define MAX_SQRT_ROUGHNESS 1.0

vec3 getDiffuseColor(vec2 texCoord)
{
    return pow(texture(diffuseMap, texCoord).rgb, vec3(gamma));
}

vec3 getSpecularColor(vec2 texCoord)
{
    vec3 specularColor = texture(specularMap, texCoord).rgb;
    return sign(specularColor) * pow(abs(specularColor), vec3(gamma));
}

vec3 getSqrtRoughness(vec2 texCoord)
{
    vec3 roughnessLookup = texture(roughnessMap, texCoord).rgb;
    return vec3(
            roughnessLookup.y + roughnessLookup.x - 16.0 / 31.0,
            roughnessLookup.y,
            roughnessLookup.y + roughnessLookup.z - 16.0 / 31.0);
}

vec4 computeResidual(vec2 texCoord, vec3 shadingNormal)
{
    vec3 diffuseColorRGB = getDiffuseColor(texCoord);
    vec3 diffuseColor = rgbToXYZ(diffuseColorRGB);
    vec3 specularColor = rgbToXYZ(getSpecularColor(texCoord));
    vec3 sqrtRoughness = getSqrtRoughness(texCoord);
    vec3 roughness = sqrtRoughness * sqrtRoughness;
    vec3 roughnessSquared = roughness * roughness;
    float maxLuminance = getMaxLuminance();
    
    vec3 view = normalize(getViewVector());

    float nDotV = max(0, dot(shadingNormal, view));
    vec4 color = getLinearColor();

    if (nDotV > 0)
    {
        LightInfo lightInfo = getLightInfo();
        vec3 light = lightInfo.normalizedDirection;
        float nDotL = max(0, dot(shadingNormal, light));

        if (nDotL > 0.0)
        {
            vec3 halfway = normalize(view + light);
            float nDotH = max(0, dot(halfway, shadingNormal));
            float nDotHSquared = nDotH * nDotH;
            float hDotV = max(0, dot(halfway, view));

            vec3 colorScaled = rgbToXYZ(color.rgb / lightInfo.attenuatedIntensity);
            vec3 diffuseContrib = diffuseColor * nDotL;


            float invGeomRatio = 4 * nDotV / (geomPartial(roughness.y, nDotL) * geomPartial(roughness.y, nDotV));
            vec3 mfdFresnel = max(vec3(0.0), (colorScaled - diffuseContrib)) * invGeomRatio;

            vec3 sqrtDenominator = (roughnessSquared - 1) * nDotH * nDotH + 1;

            if (color.a > 0)
            {
    //            return vec4(
    //                        pow(
    //                            clamp(mfdFresnel * roughnessSquared / specularColor, 0, 1)
    //                        , vec3(1.0 / 2.2))
    //                        - pow(
    //                            diffuseMode ?
    //                                clamp(nDotL * roughnessSquared * invGeomRatio, 0, 1)
    //                                : clamp(roughnessSquared * roughnessSquared / (sqrtDenominator * sqrtDenominator), 0, 1),
    //                        vec3(1.0 / 2.2))
    //                    , nDotV);

                return vec4(clamp(roughnessSquared * (mfdFresnel / specularColor - 1.0), 0, 1), 1.0);
            }
            else
            {
                return vec4(clamp(roughnessSquared * (roughnessSquared / (sqrtDenominator * sqrtDenominator) - 1.0), 0, 1), 1.0);
            }
        }
        else
        {
            return vec4(vec3(roughnessSquared * (roughnessSquared - 1.0)), 1.0);
        }
    }
    else
    {
        return vec4(vec3(roughnessSquared * (roughnessSquared - 1.0)), 1.0);
    }
}

#endif // RESID_GLSL
