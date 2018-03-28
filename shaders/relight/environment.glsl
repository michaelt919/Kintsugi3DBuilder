#ifndef ENVIRONMENT_GLSL
#define ENVIRONMENT_GLSL

#line 5 3003

uniform vec3 ambientColor;
uniform samplerCube environmentMap;
uniform bool useEnvironmentMap;
uniform int environmentMipMapLevel;
uniform int diffuseEnvironmentMipMapLevel;

vec3 getEnvironmentFresnel(vec3 lightDirection, float fresnelFactor)
{
    if (useEnvironmentMap)
    {
        return ambientColor * textureLod(environmentMap, lightDirection,
            mix(environmentMipMapLevel, 0, fresnelFactor)).rgb;
    }
    else
    {
        return ambientColor;
    }
}

vec3 getEnvironment(vec3 lightDirection)
{
    if (useEnvironmentMap)
    {
        return ambientColor * textureLod(environmentMap, lightDirection, environmentMipMapLevel).rgb;
    }
    else
    {
        return ambientColor;
    }
}

vec3 getEnvironmentDiffuse(vec3 normalDirection)
{
    if (useEnvironmentMap)
    {
        return ambientColor * textureLod(environmentMap, normalDirection, diffuseEnvironmentMipMapLevel).rgb / 2;
    }
    else
    {
        return ambientColor;
    }
}

#endif // ENVIRONMENT_GLSL