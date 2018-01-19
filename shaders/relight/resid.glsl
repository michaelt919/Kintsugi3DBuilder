#ifndef RESID_GLSL
#define RESID_GLSL

#line 5 2101

uniform sampler2D diffuseMap;
uniform sampler2D normalMap;
uniform sampler2D specularMap;
uniform sampler2D roughnessMap;

uniform mat4 model_view;

#define MAX_SQRT_ROUGHNESS 1.0

vec3 getDiffuseColor()
{
    return pow(texture(diffuseMap, fTexCoord).rgb, vec3(gamma));
}

vec3 getDiffuseNormalVector()
{
    vec2 normalDirXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
    return vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
}

vec3 getSpecularColor()
{
    vec3 specularColor = texture(specularMap, fTexCoord).rgb;
    return sign(specularColor) * pow(abs(specularColor), vec3(gamma));
}

vec3 getSqrtRoughness()
{
    vec3 roughnessLookup = texture(roughnessMap, fTexCoord).rgb;
    return vec3(
            roughnessLookup.y + roughnessLookup.x - 16.0 / 31.0,
            roughnessLookup.y,
            roughnessLookup.y + roughnessLookup.z - 16.0 / 31.0);
}

vec4 computeResidual()
{
    vec3 normal = normalize(fNormal);
//    vec3 tangent = normalize(fTangent - dot(normal, fTangent));
//    vec3 bitangent = normalize(fBitangent
//        - dot(normal, fBitangent) * normal
//        - dot(tangent, fBitangent) * tangent);
//
//    mat3 tangentToObject = mat3(tangent, bitangent, normal);
//    vec3 shadingNormal = tangentToObject * getDiffuseNormalVector();
    vec3 shadingNormal = normal;


    vec3 diffuseColorRGB = getDiffuseColor();
    vec3 diffuseColor = rgbToXYZ(diffuseColorRGB);
    vec3 specularColor = rgbToXYZ(getSpecularColor());
    vec3 sqrtRoughness = getSqrtRoughness();
    vec3 roughness = sqrtRoughness * sqrtRoughness;
    vec3 roughnessSquared = roughness * roughness;
    float maxLuminance = getMaxLuminance();
    
    vec3 view = normalize(getViewVector());

    float nDotV = max(0, dot(shadingNormal, view));
    vec4 color = getLinearColor();

    if (color.a > 0 && nDotV > 0 && dot(normal, view) > 0)
    {
        vec3 lightPreNormalized = getLightVector();
        vec3 attenuatedLightIntensity = infiniteLightSource ? 
            lightIntensity : lightIntensity / dot(lightPreNormalized, lightPreNormalized);
        vec3 light = normalize(lightPreNormalized);
        float nDotL = max(0, dot(normal, light));

        if (nDotL > 0.0)
        {
            vec3 halfway = normalize(view + light);
            float nDotH = max(0, dot(halfway, shadingNormal));
            float nDotHSquared = nDotH * nDotH;
            float hDotV = max(0, dot(halfway, view));

            vec3 colorScaled = rgbToXYZ(color.rgb / attenuatedLightIntensity);
            vec3 diffuseContrib = diffuseColor * nDotL;
            float geomRatio = min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV) / (4 * nDotV);
            vec3 mfdFresnel = /*max(vec3(0.0), */( colorScaled /*- diffuseContrib*/) / geomRatio;

            vec3 sqrtDenominator = (roughnessSquared - 1) * nDotH * nDotH + 1;
            return vec4(
                        pow(
                            clamp(mfdFresnel * roughnessSquared / specularColor, 0, 1)
                        , vec3(1.0 / 2.2))
                        - pow(
//                            clamp(roughnessSquared * roughnessSquared / (sqrtDenominator * sqrtDenominator), 0, 1),
                            clamp(diffuseContrib * roughnessSquared / (geomRatio * specularColor), 0, 1),
                        vec3(1.0 / 2.2))
                    , nDotV);
        }
        else
        {
            return vec4(0);
        }
    }
    else
    {
        return vec4(0);
    }
}

#endif // RESID_GLSL
