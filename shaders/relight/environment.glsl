#ifndef ENVIRONMENT_GLSL
#define ENVIRONMENT_GLSL

#line 5 3003

#ifndef ENVIRONMENT_TEXTURE_ENABLED
#define ENVIRONMENT_TEXTURE_ENABLED 0
#endif

#ifndef SCREEN_SPACE_RAY_TRACING_ENABLED
#define SCREEN_SPACE_RAY_TRACING_ENABLED SHADOWS_ENABLED
#endif

uniform vec3 ambientColor;

#if ENVIRONMENT_TEXTURE_ENABLED
uniform samplerCube environmentMap;
uniform mat4 envMapMatrix;
uniform int environmentMipMapLevel;
uniform int diffuseEnvironmentMipMapLevel;
#endif

#if SCREEN_SPACE_RAY_TRACING_ENABLED

#ifndef MAX_RAYTRACING_SAMPLE_COUNT
#define MAX_RAYTRACING_SAMPLE_COUNT 8
#endif

#ifndef RAY_DEPTH_GRADIENT
#define RAY_DEPTH_GRADIENT 0.1
#endif

#ifndef RAY_DEPTH_BIAS
#define RAY_DEPTH_BIAS 0.01
#endif

uniform sampler2D screenSpaceDepthBuffer;

float shadowTest(vec3 position, vec3 direction)
{
    mat4 proj_model_view = fullProjection * model_view;

    vec4 projPos = proj_model_view * vec4(position, 1);
    vec4 screenSpacePos = projPos / projPos.w;

    float dirScale = 1.0 / 256.0;
    float iterationFactor = pow(256, 1.0 / MAX_RAYTRACING_SAMPLE_COUNT);

    vec4 projDir = proj_model_view * vec4(direction, 0);
    vec4 projDirScaled = projDir * min(1.0, dirScale / length(projDir.xy));

    vec4 currentProjPos = projPos + projPos.w * projDirScaled;
    float currentGradientScale = dirScale * RAY_DEPTH_GRADIENT;

    float shadowed = 0;

    for (int i = 0; i < MAX_RAYTRACING_SAMPLE_COUNT; i++)
    {
        vec3 currentScreenSpacePos = currentProjPos.xyz / currentProjPos.w;

        if (abs(currentScreenSpacePos.x) < 1 && abs(currentScreenSpacePos.y) < 1 && currentScreenSpacePos.z < 1 && projDir.z > -1.0)
        {
            float surfaceDepth = textureOffset(screenSpaceDepthBuffer, currentScreenSpacePos.xy * 0.5 + 0.5, ivec2(0))[0];
            float surfaceDepthLinear = fullProjection[3][2] / (2 * surfaceDepth - 1 + fullProjection[2][2]);

            float scaledDiff = clamp((surfaceDepthLinear - currentProjPos.w + RAY_DEPTH_BIAS) / currentGradientScale, 0, 1);
            shadowed = max(shadowed, 1.0 - scaledDiff);
        }

        currentProjPos += currentProjPos.w * projDirScaled;
        currentGradientScale += dirScale * RAY_DEPTH_GRADIENT;

        projDirScaled *= iterationFactor;
        dirScale *= iterationFactor;
    }

    return shadowed;
}

#endif

vec3 getEnvironmentFresnel(vec3 lightDirection, float fresnelFactor)
{
    vec3 result;
#if ENVIRONMENT_TEXTURE_ENABLED
    result = ambientColor * textureLod(environmentMap, mat3(envMapMatrix) * lightDirection,
            mix(environmentMipMapLevel, 0, fresnelFactor)).rgb;
#else
    result = ambientColor;
#endif
    return result;
}

vec3 getEnvironment(vec3 lightPosition, vec3 lightDirection)
{
    vec3 result;
#if ENVIRONMENT_TEXTURE_ENABLED
    result = ambientColor * textureLod(environmentMap, mat3(envMapMatrix) * lightDirection, environmentMipMapLevel).rgb;
#else
    result = ambientColor;
#endif

#if SCREEN_SPACE_RAY_TRACING_ENABLED
    result *= (1 - shadowTest(lightPosition, lightDirection));
#endif

    return result;
}

vec3 getEnvironmentSpecular(vec3 lightDirection)
{
    vec3 result;
#if ENVIRONMENT_TEXTURE_ENABLED
    result = ambientColor * texture(environmentMap, mat3(envMapMatrix) * lightDirection).rgb;
#else
    result = ambientColor;
#endif
    return result;
}

vec3 getEnvironmentDiffuse(vec3 normalDirection)
{    vec3 result;

#if ENVIRONMENT_TEXTURE_ENABLED
    result = ambientColor * textureLod(environmentMap, mat3(envMapMatrix) * normalDirection, diffuseEnvironmentMipMapLevel).rgb / 2;
#else
    result = ambientColor;
#endif
    return result;
}

#endif // ENVIRONMENT_GLSL