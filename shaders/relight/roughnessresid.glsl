#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#line 5 2003

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

vec4 computeRoughnessResidual()
{
    vec3 normal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(normal, fTangent));
    vec3 bitangent = normalize(fBitangent
        - dot(normal, fBitangent) * normal
        - dot(tangent, fBitangent) * tangent);

    mat3 tangentToObject = mat3(tangent, bitangent, normal);
    vec3 shadingNormal = tangentToObject * getDiffuseNormalVector();


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
        LightInfo lightInfo = getLightInfo();
        vec3 light = lightInfo.normalizedDirection;
        float nDotL = max(0, dot(normal, light));

        if (nDotL > 0.0)
        {
            vec3 halfway = normalize(view + light);
            float nDotH = max(0, dot(halfway, shadingNormal));
            float nDotHSquared = nDotH * nDotH;
            float hDotV = max(0, dot(halfway, view));

            vec3 colorScaled = rgbToXYZ(color.rgb / lightInfo.attenuatedIntensity);
            vec3 diffuseContrib = diffuseColor * nDotL;
            float geomRatio = min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV) / (4 * nDotV);
            vec3 mfdFresnel = max(vec3(0.0), colorScaled - diffuseContrib) / geomRatio;

            vec3 denominator = max(vec3(0), sqrt(specularColor) / roughness
                                   - sqrt(mfdFresnel) * vec3(nDotHSquared));
            
            return vec4(clamp(sqrt(sqrt(sqrt(mfdFresnel) * vec3(max(0.0, 1 - nDotHSquared)) / denominator)),
                vec3(0), vec3(MAX_SQRT_ROUGHNESS)) - sqrtRoughness,
                    nDotV
                    * pow(max(0.0, colorScaled.y - diffuseContrib.y) * roughnessSquared.y / specularColor.y,
                        1.0 / gamma)
                    * (1 - sqrt(mfdFresnel.y * roughnessSquared.y / specularColor.y) * nDotHSquared));
                    // 1.0);
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

#endif // SPECULARFIT_GLSL
