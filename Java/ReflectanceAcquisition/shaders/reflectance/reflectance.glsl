#ifndef REFLECTANCE_GLSL
#define REFLECTANCE_GLSL

#include "linearize.glsl"

#line 7 1000

#define PI 3.1415926535897932384626433832795 // For convenience

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_LIGHT_COUNT 1024

uniform int viewCount;
uniform bool infiniteLightSources;

uniform CameraPoses
{
	mat4 cameraPoses[MAX_CAMERA_POSE_COUNT];
};

uniform LightPositions
{
	vec4 lightPositions[MAX_LIGHT_COUNT];
};

uniform LightIntensities
{
    vec3 lightIntensities[MAX_LIGHT_COUNT];
};

uniform LightIndices
{
	int lightIndices[MAX_CAMERA_POSE_COUNT];
};

vec3 getViewVector(int index)
{
    return transpose(mat3(cameraPoses[index])) * -cameraPoses[index][3].xyz - fPosition;
}

vec3 getLightVector(int index)
{
    return transpose(mat3(cameraPoses[index])) * 
        (lightPositions[lightIndices[index]].xyz - cameraPoses[index][3].xyz) - fPosition;
}

vec3 getLightIntensity(int index)
{
    return lightIntensities[lightIndices[index]];
}

vec4 getColor(int index); // Defined by imgspace.glsl or texspace.glsl

vec4 getLinearColor(int index)
{
    return linearizeColor(getColor(index));
}

#endif // REFLECTANCE_GLSL
