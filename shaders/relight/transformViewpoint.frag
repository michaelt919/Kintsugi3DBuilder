#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;

uniform mat4 model_view;
uniform mat4 fullProjection;
uniform int viewIndex;

#include "../colorappearance/colorappearance.glsl"
#include "../colorappearance/imgspace.glsl"
#include "reflectanceequations.glsl"
#include "environment.glsl"

#line 21 0

#ifndef DIFFUSE_TEXTURE_ENABLED
#define DIFFUSE_TEXTURE_ENABLED 0
#endif

#ifndef NORMAL_TEXTURE_ENABLED
#define NORMAL_TEXTURE_ENABLED 0
#endif

#ifndef SPECULAR_TEXTURE_ENABLED
#define SPECULAR_TEXTURE_ENABLED 0
#endif

#ifndef ROUGHNESS_TEXTURE_ENABLED
#define ROUGHNESS_TEXTURE_ENABLED 0
#endif

#ifndef DEFAULT_DIFFUSE_COLOR
#define DEFAULT_DIFFUSE_COLOR (vec3(0.0))
#endif // DEFAULT_DIFFUSE_COLOR

#ifndef DEFAULT_SPECULAR_COLOR
#if DIFFUSE_TEXTURE_ENABLED
#define DEFAULT_SPECULAR_COLOR (vec3(0.03125))
#else
#define DEFAULT_SPECULAR_COLOR (vec3(0.5))
#endif // DIFFUSE_TEXTURE_ENABLED
#endif // DEFAULT_SPECULAR_COLOR

#ifndef DEFAULT_SPECULAR_ROUGHNESS
#define DEFAULT_SPECULAR_ROUGHNESS (vec3(0.25)); // TODO pass in a default?
#endif

#ifndef FRESNEL_EFFECT_ENABLED
#define FRESNEL_EFFECT_ENABLED 1
#endif

#ifndef SHADOWS_ENABLED
#define SHADOWS_ENABLED 1
#endif

#if DIFFUSE_TEXTURE_ENABLED
uniform sampler2D diffuseMap;
#endif

#if NORMAL_TEXTURE_ENABLED
uniform sampler2D normalMap;
#endif

#if SPECULAR_TEXTURE_ENABLED
uniform sampler2D specularMap;
#endif

#if ROUGHNESS_TEXTURE_ENABLED
uniform sampler2D roughnessMap;
#endif

void main()
{
    vec4 color = getLinearColor(viewIndex);
    mat4 cameraPose = getCameraPose(viewIndex);
    vec3 lightPosition = lightPositions[getLightIndex(viewIndex)].xyz;
    vec3 lightIntensity = lightIntensities[getLightIndex(viewIndex)].xyz;

    if (color.a == 0.0)
    {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    vec3 triangleNormal = normalize(fNormal);

    vec3 normalDir;
#if NORMAL_TEXTURE_ENABLED
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(triangleNormal, fBitangent) * triangleNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);
    vec2 normalDirXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    normalDir = tangentToObject * normalDirTS;
#else
    normalDir = triangleNormal;
#endif // NORMAL_TEXTURE_ENABLED

    // All in camera space
    vec3 fragmentPos = (cameraPose * vec4(fPosition, 1.0)).xyz;
    vec3 normalDirCameraSpace = normalize((cameraPose * vec4(normalDir, 0.0)).xyz);
    vec3 sampleViewDir = normalize(-fragmentPos);
    vec3 sampleLightDirUnnorm = lightPosition.xyz - fragmentPos;
    float lightDistSquared = dot(sampleLightDirUnnorm, sampleLightDirUnnorm);
    vec3 sampleLightDir = sampleLightDirUnnorm * inversesqrt(lightDistSquared);
    vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);

    float nDotH = max(0, dot(normalDirCameraSpace, sampleHalfDir));
    float nDotV_sample = max(0, dot(normalDirCameraSpace, sampleViewDir));
    float nDotL_sample = max(0, dot(normalDirCameraSpace, sampleLightDir));
    float hDotV_sample = max(0, dot(sampleHalfDir, sampleViewDir));

    if (nDotV_sample == 0.0 || nDotL_sample == 0.0)
    {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    vec3 viewPos = transpose(mat3(model_view)) * -model_view[3].xyz;

    vec3 virtualViewDir =
        normalize((cameraPose * vec4(viewPos, 1.0)).xyz - fragmentPos);
    vec3 virtualLightDir = -reflect(virtualViewDir, sampleHalfDir);

#if SHADOWS_ENABLED
    float shadow = shadowTest(fPosition, transpose(mat3(cameraPose)) * virtualLightDir);

    if (shadow == 1.0)
    {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
#else
    float shadow = 0.0;
#endif

    vec3 diffuseColor;
#if DIFFUSE_TEXTURE_ENABLED
    diffuseColor = pow(texture(diffuseMap, fTexCoord).rgb, vec3(gamma));
#else
    diffuseColor = DEFAULT_DIFFUSE_COLOR;
#endif

    vec3 specularColor;
#if SPECULAR_TEXTURE_ENABLED
    specularColor = pow(texture(specularMap, fTexCoord).rgb, vec3(gamma));
#else
    specularColor = DEFAULT_SPECULAR_COLOR;
#endif

    vec3 roughness;
#if ROUGHNESS_TEXTURE_ENABLED
    vec3 roughnessLookup = texture(roughnessMap, fTexCoord).rgb;
    vec3 sqrtRoughness = vec3(
        roughnessLookup.g + roughnessLookup.r - 16.0 / 31.0,
        roughnessLookup.g,
        roughnessLookup.g + roughnessLookup.b - 16.0 / 31.0);
    roughness = sqrtRoughness * sqrtRoughness;
#else
    roughness = DEFAULT_SPECULAR_ROUGHNESS;
#endif

    float geomAttenSample = geom(roughness.y, nDotH, nDotV_sample, nDotL_sample, hDotV_sample);

    if (geomAttenSample == 0.0)
    {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    float maxLuminance = getMaxLuminance();

    vec3 attenuatedLightIntensity = lightIntensity;

#if !INFINITE_LIGHT_SOURCES
    attenuatedLightIntensity /= lightDistSquared;
#endif

    vec3 diffuseContrib = diffuseColor * nDotL_sample * attenuatedLightIntensity;

    float nDotL_virtual = max(0, dot(normalDirCameraSpace, virtualLightDir));
    float nDotV_virtual = max(0.125, dot(normalDirCameraSpace, virtualViewDir));
    float hDotV_virtual = max(0, dot(sampleHalfDir, virtualViewDir));

    float geomAttenVirtual = geom(roughness.y, nDotH, nDotV_virtual, nDotL_virtual, hDotV_virtual);

    vec3 specularResid = clamp(color.rgb - diffuseContrib,
        0, maxLuminance - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b)));

    // Light intensities in view set files are assumed to be pre-divided by pi.
    // Or alternatively, the result of getLinearColor gives a result
    // where a diffuse reflectivity of 1 is represented by a value of pi.
    // See diffusefit.glsl
    vec3 cosineBRDF = specularResid.rgb / (attenuatedLightIntensity * PI);

    vec3 fresnelTimesDist = cosineBRDF * 4 * nDotV_sample / geomAttenSample;
    float dist = getLuminance(fresnelTimesDist / specularColor);

    vec3 newFresnelTimesDist;
#if FRESNEL_EFFECT_ENABLED
    newFresnelTimesDist = fresnel(fresnelTimesDist, vec3(dist), hDotV_virtual);
#else
    newFresnelTimesDist = fresnelTimesDist;
#endif

    fragColor = vec4((1.0 - shadow) * newFresnelTimesDist * geomAttenVirtual / (4 * nDotV_virtual), 1.0);
}
