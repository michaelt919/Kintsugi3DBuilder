#ifndef COLOR_APPEARANCE_DEBUG_GLSL
#define COLOR_APPEARANCE_DEBUG_GLSL

#include "colorappearance.glsl"

#line 7 1109

//vec3 getSpecularColor();
//vec3 getRoughness();

#define DIFFUSE_COLOR vec3(0)
#define ROUGHNESS_SQUARED (sqrt(rgbToXYZ(fTexCoord.yxy)) / 2)
#define SPECULAR_COLOR (ROUGHNESS_SQUARED * 0.5)
//#define SPECULAR_COLOR (ROUGHNESS_SQUARED * rgbToXYZ(getSpecularColor()) / pow(getRoughness(), vec3(2)))

vec4 getColor(int index)
{
    vec3 normal = normalize(fNormal);
    vec3 view = normalize(getViewVector(index));
    float nDotV = max(0, dot(normal, view));

    vec3 tangent = normalize(fTangent - dot(normal, fTangent));
    vec3 bitangent = normalize(fBitangent
        - dot(normal, fBitangent) * normal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, normal);
    vec3 shadingNormal =
        fNormal;
        //tangentToObject * normalize(vec3(0.5 * cos(8 * 3.14 * fTexCoord), 1.0));

    if (nDotV > 0)
    {
        LightInfo lightInfo = getLightInfo(index);
        vec3 light = lightInfo.normalizedDirection;
        float nDotL = max(0, dot(light, shadingNormal));
        nDotV = max(0, dot(view, shadingNormal));

        vec3 halfway = normalize(view + light);
        float nDotH = dot(halfway, shadingNormal);

        if (nDotV > 0.0 && nDotH > 0.0)
        {
            float nDotHSquared = nDotH * nDotH;

            // float mfdEval = exp((nDotHSquared - 1.0) / (nDotHSquared * ROUGHNESS_SQUARED))
                // / (ROUGHNESS_SQUARED * nDotHSquared * nDotHSquared);

            vec3 q = ROUGHNESS_SQUARED + (1 - nDotHSquared) / nDotHSquared;
            vec3 mfdEval = ROUGHNESS_SQUARED / (nDotHSquared * nDotHSquared * q * q);

            float hDotV = max(0, dot(halfway, view));

            return vec4(pow(DIFFUSE_COLOR * nDotL + xyzToRGB(SPECULAR_COLOR * mfdEval) / (4 * nDotV)
                    * min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV)
                    * lightInfo.attenuatedIntensity / getMaxLuminance(),
                vec3(1.0 / gamma)), 1.0);
        }
        else
        {
            return vec4(0.0);
        }
    }
    else
    {
        return vec4(0.0);
    }
}

#endif // COLOR_APPEARANCE_DEBUG_GLSL
