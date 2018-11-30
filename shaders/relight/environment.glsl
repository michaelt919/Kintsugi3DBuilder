#ifndef ENVIRONMENT_GLSL
#define ENVIRONMENT_GLSL

#line 5 3003

#ifndef ENVIRONMENT_TEXTURE_ENABLED
#define ENVIRONMENT_TEXTURE_ENABLED 0
#endif

uniform vec3 ambientColor;

#if ENVIRONMENT_TEXTURE_ENABLED
uniform samplerCube environmentMap;
uniform int environmentMipMapLevel;
uniform int diffuseEnvironmentMipMapLevel;
#endif

vec3 getEnvironmentFresnel(vec3 lightDirection, float fresnelFactor)
{
    vec3 result;
#if ENVIRONMENT_TEXTURE_ENABLED
    result = ambientColor * textureLod(environmentMap, lightDirection,
            mix(environmentMipMapLevel, 0, fresnelFactor)).rgb;
#else
    result = ambientColor;
#endif
    return result;
}

vec3 getEnvironment(vec3 lightDirection)
{
    vec3 result;
#if ENVIRONMENT_TEXTURE_ENABLED
    result = ambientColor * textureLod(environmentMap, lightDirection, environmentMipMapLevel).rgb;
#else
    result = ambientColor;
#endif
    return result;
}

vec3 getEnvironmentSpecular(vec3 lightDirection)
{
    vec3 result;
#if ENVIRONMENT_TEXTURE_ENABLED
    result = ambientColor * texture(environmentMap, lightDirection).rgb;
#else
    result = ambientColor;
#endif
    return result;
}

vec3 getEnvironmentDiffuse(vec3 normalDirection)
{    vec3 result;

#if ENVIRONMENT_TEXTURE_ENABLED
    result = ambientColor * textureLod(environmentMap, normalDirection, diffuseEnvironmentMipMapLevel).rgb / 2;
#else
    result = ambientColor;
#endif
    return result;
}

#endif // ENVIRONMENT_GLSL