#ifndef COLOR_APPEARANCE_SINGLE_GLSL
#define COLOR_APPEARANCE_SINGLE_GLSL

#include "linearize.glsl"

#line 7 1001

#ifndef PI
#define PI 3.1415926535897932384626433832795 // For convenience
#endif

uniform bool infiniteLightSource;

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

#endif // COLOR_APPEARANCE_SINGLE_GLSL
