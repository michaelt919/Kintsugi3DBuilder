#ifndef COLOR_APPEARANCE_GLSL
#define COLOR_APPEARANCE_GLSL

#include "linearize.glsl"

#line 7 1003

#define PI 3.1415926535897932384626433832795 // For convenience

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_POSE_COUNT_DIV_4 256
#define MAX_LIGHT_COUNT 1024

uniform int viewCount;
uniform bool infiniteLightSources;

layout(std140) uniform ViewIndices
{
    ivec4 viewIndices[MAX_CAMERA_POSE_COUNT_DIV_4];
};

layout(std140) uniform CameraWeights
{
    vec4 cameraWeights[MAX_CAMERA_POSE_COUNT_DIV_4];
};

layout(std140) uniform CameraPoses
{
    mat4 cameraPoses[MAX_CAMERA_POSE_COUNT];
};

layout(std140) uniform LightPositions
{
    vec4 lightPositions[MAX_LIGHT_COUNT];
};

layout(std140) uniform LightIntensities
{
    vec4 lightIntensities[MAX_LIGHT_COUNT];
};

layout(std140) uniform LightIndices
{
    ivec4 lightIndices[MAX_CAMERA_POSE_COUNT_DIV_4];
};

int getViewIndex(int virtualIndex)
{
    return viewIndices[virtualIndex/4][virtualIndex%4];
}

int getLightIndex(int virtualIndex)
{
    int viewIndex = getViewIndex(virtualIndex);
    return lightIndices[viewIndex/4][viewIndex%4];
}

float getCameraWeight(int virtualIndex)
{
    int viewIndex = getViewIndex(virtualIndex);
    return cameraWeights[viewIndex/4][viewIndex%4];
}

vec3 getViewVector(int virtualIndex)
{
    int viewIndex = getViewIndex(virtualIndex);
    return transpose(mat3(cameraPoses[viewIndex])) * -cameraPoses[viewIndex][3].xyz - fPosition;
}

vec3 getLightVector(int virtualIndex)
{
    int viewIndex = getViewIndex(virtualIndex);
    return transpose(mat3(cameraPoses[viewIndex])) * 
        (lightPositions[getLightIndex(viewIndex)].xyz - cameraPoses[viewIndex][3].xyz) - fPosition;
}

vec3 getLightIntensity(int virtualIndex)
{
    int viewIndex = getViewIndex(virtualIndex);
    return lightIntensities[getLightIndex(viewIndex)].rgb;
}

vec4 getColor(int virtualIndex); // Defined by imgspace_subset.glsl or texspace_subset.glsl

vec4 getLinearColor(int virtualIndex)
{
    return linearizeColor(getColor(virtualIndex));
}

#endif // COLOR_APPEARANCE_GLSL
