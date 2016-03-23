#ifndef REFLECTANCE_SINGLE_GLSL
#define REFLECTANCE_SINGLE_GLSL

#line 5 1001

uniform bool infiniteLightSource;
uniform float gamma;

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

#endif // REFLECTANCE_SINGLE_GLSL
