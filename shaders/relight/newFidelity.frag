#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;

#ifndef MATERIAL_EXPLORATION_MODE
#define MATERIAL_EXPLORATION_MODE 0
#endif

#define WRITE_INTENSITY_AS_ALPHA 1
#define SINGLE_VIEW_MODE_ENABLED 1
#define INTEGRATING_MODE_ENABLED 1
#define SMITH_MASKING_SHADOWING 1

#if SINGLE_VIEW_MODE_ENABLED
#include <shaders/colorappearance/colorappearance_multi_as_single.glsl>
#else
#include <shaders/colorappearance/colorappearance.glsl>
#endif

#if MATERIAL_EXPLORATION_MODE
#include <shaders/colorappearance/analytic.glsl>
#else
#include <shaders/colorappearance/imgspace.glsl>
#endif

#line 32 0

#if !SINGLE_VIEW_MODE_ENABLED
uniform mat4 model_view;
#endif

#if MATERIAL_EXPLORATION_MODE && NORMAL_TEXTURE_ENABLED
uniform sampler2D normalMap;

vec3 getNormal(vec2 texCoord)
{
    vec2 normalXY = texture(normalMap, texCoord).xy * 2 - 1;
    return vec3(normalXY, 1.0 - dot(normalXY, normalXY));
}
#endif

float min3(vec3 v)
{
    return min(v.x, min(v.y, v.z));
}

vec2 computeIntegratingWeight(int index, vec3 normal, vec3 viewPos)
{
    mat4 cameraPoseOther = getCameraPose(index);
    vec3 fragmentPos = (cameraPoseOther * vec4(fPosition, 1.0)).xyz;
    vec3 sampleLightDir = normalize(lightPositions[getLightIndex(index)].xyz - fragmentPos);
    float nDotL_sample = max(0.001, dot(normal, sampleLightDir));

    vec3 sampleViewDir = normalize(-fragmentPos);
    vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
    vec3 virtualViewDir = normalize((cameraPoseOther * vec4(viewPos, 1.0)).xyz - fragmentPos);
    vec3 virtualLightDir = -reflect(virtualViewDir, sampleHalfDir);
    float nDotL_virtual = max(0, dot(normal, virtualLightDir));

    return getCameraWeight(index) * nDotL_virtual / vec2(nDotL_sample, 1);
}

void main()
{
#if SINGLE_VIEW_MODE_ENABLED
    mat4 model_view = cameraPose;
#endif

    vec3 normal = normalize(mat3(model_view) * fNormal);
    vec3 view = -normalize((model_view * vec4(fPosition, 1.0)).xyz);
    vec3 adjX = normalize(vec3(1,0,0) - view.x * view);
    vec3 adjY = normalize(vec3(0,1,0) - view.y * view - adjX.y * adjX);

    vec4 sampleColor = getLinearColor();
    float luminance;
#if SINGLE_VIEW_MODE_ENABLED
        luminance = min3(sampleColor.rgb / getLightInfo().attenuatedIntensity);
#else
        luminance = 0.0;
#endif

    float secondLuminance = 0.0;

    float maxLuminance;
#if MATERIAL_EXPLORATION_MODE
    maxLuminance = max(ANALYTIC_SPECULAR_COLOR.r, max(ANALYTIC_SPECULAR_COLOR.g, ANALYTIC_SPECULAR_COLOR.b))
            / min(1.0, 4 * ANALYTIC_ROUGHNESS * ANALYTIC_ROUGHNESS)
        + max(ANALYTIC_DIFFUSE_COLOR.r, max(ANALYTIC_DIFFUSE_COLOR.g, ANALYTIC_DIFFUSE_COLOR.b));

#else
    maxLuminance = getMaxLuminance();
#endif

#if INTEGRATING_MODE_ENABLED
    vec3 viewPos = transpose(mat3(model_view)) * -model_view[3].xyz;
    float integratingWeight = computeIntegratingWeight(viewIndex, normal, viewPos)[0];
    float sum = 0.0;
    float weightSum = 0.0;
//#else
#endif

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        float sampleLuminance = min3(getLinearColor(i).rgb / getLightInfo(i).attenuatedIntensity);

#if INTEGRATING_MODE_ENABLED
        vec2 integratingWeights = computeIntegratingWeight(i, normal, viewPos);
        float weightedLuminance = sampleLuminance * integratingWeights[0];
        sum += weightedLuminance;
        weightSum += integratingWeights[1];
#endif

#if SINGLE_VIEW_MODE_ENABLED
        if (i != viewIndex)
#endif
        {
            secondLuminance = max(secondLuminance, sampleLuminance);
                
            if (secondLuminance >= luminance)
            {
#if SINGLE_VIEW_MODE_ENABLED
                fragColor = vec4(0);
                return;
#else
                float tmp = secondLuminance;
                secondLuminance = luminance;
                luminance = tmp;

#if INTEGRATING_MODE_ENABLED
                integratingWeight = integratingWeights[0];
#endif // INTEGRATING_MODE_ENABLED
#endif // SINGLE_VIEW_MODE_ENABLED
            }
        }
    }

    float intensity;
    float diff;
#if INTEGRATING_MODE_ENABLED
    intensity = pow(sum / (weightSum * maxLuminance), 1.0 / 2.2);
    diff = intensity - pow((sum - integratingWeight * (luminance - secondLuminance)) / (weightSum * maxLuminance), 1.0 / 2.2);
#else
    float luminanceGammaCorrected = pow(luminance / maxLuminance, 1.0 / 2.2);
    intensity = luminanceGammaCorrected;
    diff = luminanceGammaCorrected - pow(secondLuminance / maxLuminance, 1.0 / 2.2);
#endif

#if !WRITE_INTENSITY_AS_ALPHA
    intensity = 1.0;
#endif
    
    fragColor = vec4(dot(adjX, normal) + 0.5, dot(adjY, normal) + 0.5, diff, max(0.005, intensity));
}
