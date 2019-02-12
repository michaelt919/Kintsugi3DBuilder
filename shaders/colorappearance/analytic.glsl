#ifndef COLOR_APPEARANCE_ANALYTIC_GLSL
#define COLOR_APPEARANCE_ANALYTIC_GLSL

#include "colorappearance.glsl"
#include "../relight/reflectanceequations.glsl"

#line 7 1109

#ifndef ANALYTIC_METAL
#define ANALYTIC_METAL 1
#endif


#ifndef ANALYTIC_COLOR

#if ANALYTIC_METAL
#define ANALYTIC_COLOR vec3(1.0, 0.6, 0.133)
//#define ANALYTIC_COLOR vec3(1.0, 0.5, 0.1)
//#define ANALYTIC_COLOR vec3(1.0, 0.25, 0.0)
#else
#define ANALYTIC_COLOR vec3(.45,.27,.065)
#endif

#endif // ANALYTIC_COLOR


#ifndef ANALYTIC_DIFFUSE_COLOR

#if ANALYTIC_METAL
#define ANALYTIC_DIFFUSE_COLOR vec3(0)
#else
#define ANALYTIC_DIFFUSE_COLOR ANALYTIC_COLOR
#endif

#endif // ANALYTIC_DIFFUSE_COLOR


#ifndef ANALYTIC_SPECULAR_COLOR

#if ANALYTIC_METAL
#define ANALYTIC_SPECULAR_COLOR ANALYTIC_COLOR
#else
#define ANALYTIC_SPECULAR_COLOR vec3(0.04)
#endif

#endif // ANALYTIC_SPECULAR_COLOR


#ifndef ANALYTIC_BUMP_HEIGHT
#define ANALYTIC_BUMP_HEIGHT 4.0
#endif

#ifndef ANALYTIC_ROUGHNESS
#define ANALYTIC_ROUGHNESS 0.2
#endif

#ifndef ANALYTIC_UV_SCALE
#define ANALYTIC_UV_SCALE 5.0
#endif

vec3 getNormal(vec2 texCoord);

vec4 getColor(int index)
{
    vec3 normal = normalize(fNormal);
    vec3 view = normalize(getViewVector(index));
    float nDotV = max(0, dot(normal, view));

    vec3 tangent = normalize(fTangent - dot(normal, fTangent) * normal);
    vec3 bitangent = normalize(fBitangent
        - dot(normal, fBitangent) * normal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, normal);

    vec2 scaledTexCoord = ANALYTIC_UV_SCALE * fTexCoord;

    vec3 shadingNormal =
        normalize(tangentToObject * (getNormal(scaledTexCoord - floor(scaledTexCoord)) * vec3(ANALYTIC_BUMP_HEIGHT, ANALYTIC_BUMP_HEIGHT, 1.0)));

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
            float hDotV = max(0, dot(halfway, view));

            return vec4(pow((ANALYTIC_DIFFUSE_COLOR * nDotL
                        + fresnel(ANALYTIC_SPECULAR_COLOR, vec3(1.0), hDotV) * distTimesPi(nDotH, vec3(ANALYTIC_ROUGHNESS))
                            * geom(ANALYTIC_ROUGHNESS, nDotH, nDotV, nDotL, hDotV) / (4 * nDotV))
                    * lightInfo.attenuatedIntensity,
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

#endif // COLOR_APPEARANCE_ANALYTIC_GLSL
