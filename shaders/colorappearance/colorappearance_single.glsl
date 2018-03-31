#ifndef COLOR_APPEARANCE_SINGLE_GLSL
#define COLOR_APPEARANCE_SINGLE_GLSL

#include "linearize.glsl"

#line 7 1001

#ifndef PI
#define PI 3.1415926535897932384626433832795 // For convenience
#endif

#ifndef INFINITE_LIGHT_SOURCE
#define INFINITE_LIGHT_SOURCE 0
#endif

uniform mat4 cameraPose;
uniform vec3 lightPosition;
uniform vec3 lightIntensity;

vec3 getViewVector()
{
    return transpose(mat3(cameraPose)) * -cameraPose[3].xyz - fPosition;
}

vec3 getLightVector()
{
    return transpose(mat3(cameraPose)) * (lightPosition.xyz - cameraPose[3].xyz) - fPosition;
}

vec4 getColor(); // Defined by imgspace_single.glsl or texspace_single.glsl

vec4 getLinearColor()
{
    return linearizeColor(getColor());
}

LightInfo getLightInfo()
{
    LightInfo result;
    result.normalizedLightDirection = getLightVector();
    result.attenuatedLightIntensity = getLightIntensity();

    float lightDistSquared = dot(result.normalizedLightDirection, result.normalizedLightDirection);
    result.normalizedLightDirection *= inversesqrt(lightDistSquared);

#if !INFINITE_LIGHT_SOURCES
    result.attenuatedLightIntensity /= lightDistSquared;
#endif

    return result;
}

#endif // COLOR_APPEARANCE_SINGLE_GLSL
