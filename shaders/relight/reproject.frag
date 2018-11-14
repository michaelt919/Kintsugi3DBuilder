#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out int fragObjectID;

#include "reflectanceequations.glsl"
#include "tonemap.glsl"
#include "environment.glsl"

#line 17 0

uniform int objectID;

uniform mat4 model_view;
uniform vec3 viewPos;
uniform mat4 envMapMatrix;

#define MAX_VIRTUAL_LIGHT_COUNT 4
uniform vec3 lightIntensityVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform vec3 lightPosVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform mat4 lightMatrixVirtual[MAX_VIRTUAL_LIGHT_COUNT];
uniform int virtualLightCount;

uniform sampler2D diffuseMap;
uniform sampler2D normalMap;
uniform sampler2D specularMap;
uniform sampler2D roughnessMap;

uniform bool useDiffuseTexture;
uniform bool useNormalTexture;
uniform bool useSpecularTexture;
uniform bool useRoughnessTexture;

uniform sampler2DArray shadowMaps;
uniform bool shadowsEnabled;

uniform bool relightingEnabled;
uniform bool pbrGeometricAttenuationEnabled;
uniform bool fresnelEnabled;

uniform bool imageBasedRenderingEnabled;
uniform bool depthTestingEnabled;
uniform float depthTestBias;

uniform mat4 prerenderedModelViewPrimary;
uniform mat4 prerenderedModelViewSecondary;
uniform mat4 prerenderedProjectionPrimary;
uniform mat4 prerenderedProjectionSecondary;
uniform float prerenderedSecondaryWeight;
uniform sampler2D prerenderedImagePrimary;
uniform sampler2D prerenderedImageSecondary;
uniform sampler2D prerenderedDepthImagePrimary;
uniform sampler2D prerenderedDepthImageSecondary;

//#define MAX_PRERENDERED_IMAGE_COUNT 5
//uniform int prerenderedImageCount;
//uniform mat4 prerenderedModelView[MAX_PRERENDERED_IMAGE_COUNT];
//uniform mat4 prerenderedProjection[MAX_PRERENDERED_IMAGE_COUNT];
//uniform float firstPrerenderedImageWeight;
//uniform sampler2DArray prerenderedImages;
//uniform sampler2DArray prerenderedDepthImages;

vec4 computeProjTexCoord(mat4 projection, mat4 modelView)
{
    vec4 projTexCoord = projection * modelView * vec4(fPosition, 1.0);
    projTexCoord /= projTexCoord.w;
    projTexCoord = (projTexCoord + vec4(1)) / 2;
    return projTexCoord;
}

vec4 reproject(sampler2D colorImage, sampler2D depthImage, mat4 projection, mat4 modelView, vec3 view, vec3 normal)
{
    float nDotVCurrent = dot(normal, view);

    vec4 projTexCoord = projection * modelView * vec4(fPosition, 1.0);
    projTexCoord /= projTexCoord.w;
    projTexCoord = (projTexCoord + vec4(1)) / 2;

    float nDotVPrerendered = (modelView * vec4(normal, 0.0)).z;

    if (projTexCoord.x < 0 || projTexCoord.x > 1 || projTexCoord.y < 0 || projTexCoord.y > 1 ||
        projTexCoord.z < 0 || projTexCoord.z > 1)
    {
        return vec4(0);
    }
    else
    {
        if (depthTestingEnabled)
        {
            float imageDepth = texture(depthImage, projTexCoord.xy).r;
            if (abs(projTexCoord.z - imageDepth) > depthTestBias)
            {
                // Occluded
                return vec4(0);
            }
        }

        return clamp(2 * nDotVPrerendered / nDotVCurrent, 0.0, 1.0) * texture(colorImage, projTexCoord.xy);
    }
}

//float computeBuehlerWeight(vec3 targetDirection, vec3 sampleDirection)
//{
//    return 1.0 / (1.0 - clamp(dot(sampleDirection, targetDirection), 0.0, 0.99999));
//}
//
//float getWeightByImage(int index, vec3 targetDirection)
//{
//    float weight = computeBuehlerWeight(mat3(prerenderedModelView[index]) * targetDirection,
//                        -normalize((prerenderedModelView[index] * vec4(fPosition, 1)).xyz));
//    if (index == 0)
//    {
//        weight *= firstPrerenderedImageWeight;
//    }
//
//    return weight;
//}
//
//vec4 computeBuehler(vec3 view, vec3 normal)
//{
//    // Evaluate the light field
//    // weights[0] should be the smallest weight
//    vec4 sum = vec4(0.0);
//    for (int i = 1; i < MAX_PRERENDERED_IMAGE_COUNT && i < prerenderedImageCount; i++)
//    {
//        vec4 projTexCoord = prerenderedProjection[i] * prerenderedModelView[i] * vec4(fPosition, 1.0);
//        projTexCoord /= projTexCoord.w;
//        projTexCoord = (projTexCoord + vec4(1)) / 2;
//
//        float nDotVPrerendered = (prerenderedModelView[i] * vec4(normal, 0.0)).z;
//        float nDotVCurrent = dot(normal, view);
//
//        if (projTexCoord.x < 0 || projTexCoord.x > 1 || projTexCoord.y < 0 || projTexCoord.y > 1 ||
//            projTexCoord.z < 0 || projTexCoord.z > 1)
//        {
//            return vec4(0);
//        }
//        else
//        {
//#if VISIBILITY_TEST_ENABLED
//            float imageDepth = texture(prerenderedDepthImages, vec3(projTexCoord.xy, i)).r;
//            if (abs(projTexCoord.z - imageDepth) > occlusionBias)
//            {
//                // Occluded
//                return vec4(0);
//            }
//#endif
//
//            sum += getWeightByImage(i, view) * min(1, 2 * nDotVPrerendered / nDotVCurrent)
//                    * texture(prerenderedImages, vec3(projTexCoord.xy, i));
//        }
//    }
//
//    return sum / max(sum.a, 1);
//}

void main()
{
    vec3 viewDir = normalize(viewPos - fPosition);

    vec2 normalDirXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));

    vec3 gNormal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(gNormal, fTangent) * gNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(gNormal, fBitangent) * gNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, gNormal);

    vec3 normalDir;
    if (useNormalTexture)
    {
        normalDir = tangentToObject * normalDirTS;
        //normalDir = gNormal;
        //normalDir = normalDirTS;
    }
    else
    {
        normalDir = normalize(fNormal);
    }

    vec3 diffuseColor;
    if (useDiffuseTexture)
    {
        diffuseColor = pow(texture(diffuseMap, fTexCoord).rgb, vec3(gamma));
    }
    else if (!imageBasedRenderingEnabled && !useSpecularTexture)
    {
        diffuseColor = vec3(0.125);
    }
    else
    {
        diffuseColor = vec3(0.0);
    }

    vec3 specularColor;
    if (useSpecularTexture)
    {
        specularColor = pow(texture(specularMap, fTexCoord).rgb, vec3(gamma));
    }
    else if (!imageBasedRenderingEnabled && useDiffuseTexture)
    {
        specularColor = vec3(0.0);
    }
    else
    {
        specularColor = vec3(0.03125); // TODO pass in a default?
    }

    vec3 specularColorXYZ = rgbToXYZ(specularColor);

    vec3 roughness;
    if (useRoughnessTexture)
    {
        vec3 roughnessLookup = texture(roughnessMap, fTexCoord).rgb;
        vec3 sqrtRoughness = vec3(
            roughnessLookup.y + roughnessLookup.x - 16.0 / 31.0,
            roughnessLookup.y,
            roughnessLookup.y + roughnessLookup.z - 16.0 / 31.0);
        roughness = sqrtRoughness * sqrtRoughness;
    }
    else
    {
        roughness = vec3(0.25); // TODO pass in a default?
    }

    vec3 roughnessSq = roughness * roughness;

    vec4 reprojectedColor = vec4(0);

    if (imageBasedRenderingEnabled)
    {
        //reprojectedColor = computeBuehler(viewDir, normalDir);
        reprojectedColor = //mix(
            reproject(prerenderedImagePrimary, prerenderedDepthImagePrimary,
                prerenderedProjectionPrimary, prerenderedModelViewPrimary, viewDir, normalDir)//,
        //    reproject(prerenderedImageSecondary, prerenderedDepthImageSecondary,
        //        prerenderedProjectionSecondary, prerenderedModelViewSecondary, viewDir, normalDir),
        ;//    prerenderedSecondaryWeight);
    }

    //if (reprojectedColor.a < 1.0)
    {
        float nDotV = dot(normalDir, viewDir);
        vec3 reflectance = vec3(0.0);

        if (relightingEnabled)
        {
            reflectance += diffuseColor * getEnvironmentDiffuse((envMapMatrix * vec4(normalDir, 0.0)).xyz);

            vec3 reflectivity;
            if (useSpecularTexture)
            {
                reflectivity = min(vec3(1.0), diffuseColor + specularColor);
            }
            else
            {
                reflectivity = diffuseColor;
            }

            if (fresnelEnabled)
            {
                reflectance += fresnel(ambientColor * reflectivity, ambientColor, nDotV);
            }
            else
            {
                reflectance += ambientColor * reflectivity;
            }
        }

        int effectiveLightCount = (relightingEnabled ? virtualLightCount : 1);

        for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT && i < effectiveLightCount; i++)
        {
            vec3 lightDirUnNorm;
            vec3 lightDir;
            float nDotL;
            if (relightingEnabled)
            {
                lightDirUnNorm = lightPosVirtual[i] - fPosition;
                lightDir = normalize(lightDirUnNorm);
                nDotL = max(0.0, dot(normalDir, lightDir));
            }
            else
            {
                lightDirUnNorm = viewPos - fPosition;
                lightDir = viewDir;
                nDotL = max(0.0, dot(normalDir, viewDir));
            }

            if (nDotL > 0.0)
            {
                bool shadow = false;
                if (relightingEnabled && shadowsEnabled)
                {
                    vec4 projTexCoord = lightMatrixVirtual[i] * vec4(fPosition, 1.0);
                    projTexCoord /= projTexCoord.w;
                    projTexCoord = (projTexCoord + vec4(1)) / 2;
                    shadow = !(projTexCoord.x >= 0 && projTexCoord.x <= 1
                        && projTexCoord.y >= 0 && projTexCoord.y <= 1
                        && projTexCoord.z >= 0 && projTexCoord.z <= 1
                        && texture(shadowMaps, vec3(projTexCoord.xy, i)).r - projTexCoord.z >= -0.01);
                }

                if (!shadow)
                {
                    vec3 halfDir = normalize(viewDir + lightDir);
                    float hDotV = dot(halfDir, viewDir);
                    float nDotH = dot(normalDir, halfDir);

                    float nDotHSq = max(0, nDotH) * max(0, nDotH);

                    vec3 mfdFresnel;

                    if (relightingEnabled && fresnelEnabled)
                    {
                        vec3 mfdFresnelBaseXYZ = specularColorXYZ * distTimesPi(nDotH, roughness);
                        mfdFresnel = fresnel(xyzToRGB(mfdFresnelBaseXYZ), vec3(mfdFresnelBaseXYZ.y), hDotV);
                    }
                    else
                    {
                        mfdFresnel = xyzToRGB(specularColorXYZ * distTimesPi(nDotH, roughness));
                    }

                    vec3 lightVectorTransformed = (model_view * vec4(lightDirUnNorm, 0.0)).xyz;

                    reflectance += (
                        nDotL * diffuseColor +
                        mfdFresnel
                         * (pbrGeometricAttenuationEnabled
                            ? geom(roughness.y, nDotH, nDotV, nDotL, hDotV) / (4 * nDotV) : (nDotL / 4)))
                         * lightIntensityVirtual[i] / dot(lightVectorTransformed, lightVectorTransformed);
                }
            }
        }

        fragColor = reprojectedColor + (1.0 - reprojectedColor.a) * tonemap(reflectance, 1.0);
    }
//    else
//    {
//        fragColor = reprojectedColor;
//    }

    fragObjectID = objectID;
}
