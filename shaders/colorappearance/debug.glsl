#ifndef COLOR_APPEARANCE_DEBUG_GLSL
#define COLOR_APPEARANCE_DEBUG_GLSL

#include "colorappearance.glsl"

#line 7 1109

#define DIFFUSE_COLOR vec3(0.5)
#define SPECULAR_COLOR vec3(fTexCoord.y / 4)
#define ROUGHNESS_SQUARED (fTexCoord.y / 2)

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
        //fNormal;
        tangentToObject * normalize(vec3(0.5 * cos(8 * 3.14 * fTexCoord), 1.0));

    if (nDotV > 0)
    {
        vec3 lightPreNormalized = getLightVector(index);
        vec3 attenuatedLightIntensity = infiniteLightSources ?
            getLightIntensity(index) :
            getLightIntensity(index) / (dot(lightPreNormalized, lightPreNormalized));
        vec3 light = normalize(lightPreNormalized);
        float nDotL = max(0, dot(light, shadingNormal));
        nDotV = max(0, dot(view, shadingNormal));

        vec3 halfway = normalize(view + light);
        float nDotH = dot(halfway, shadingNormal);

        if (nDotV > 0.0 && nDotH > 0.0)
        {
            float nDotHSquared = nDotH * nDotH;

            // float mfdEval = exp((nDotHSquared - 1.0) / (nDotHSquared * ROUGHNESS_SQUARED))
                // / (ROUGHNESS_SQUARED * nDotHSquared * nDotHSquared);

            float q = ROUGHNESS_SQUARED + (1 - nDotHSquared) / nDotHSquared;
            float mfdEval =  ROUGHNESS_SQUARED / (nDotHSquared * nDotHSquared * q * q);

            float hDotV = max(0, dot(halfway, view));

            return vec4(pow((DIFFUSE_COLOR * nDotL + SPECULAR_COLOR * mfdEval / (4 * nDotV)
                    * min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV)) * attenuatedLightIntensity
                    / getMaxLuminance(), vec3(1.0 / gamma)), 1.0);
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
