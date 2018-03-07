#ifndef COLOR_APPEARANCE_GLSL
#define COLOR_APPEARANCE_GLSL

#include "linearize.glsl"
#include "../common/extractcomponent.glsl"

#line 7 1000

#define PI 3.1415926535897932384626433832795 // For convenience

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_POSE_COUNT_DIV_4 256
#define MAX_LIGHT_COUNT 1024

uniform int viewCount;
uniform bool infiniteLightSources;

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

int getLightIndex(int poseIndex)
{
    return extractComponentByIndex(lightIndices[poseIndex/4], poseIndex%4);
}

float getCameraWeight(int index)
{
    return extractComponentByIndex(cameraWeights[index/4], index%4);
}

vec3 getViewVector(int index)
{
    return transpose(mat3(cameraPoses[index])) * -cameraPoses[index][3].xyz - fPosition;
}

vec3 getLightVector(int index)
{
    return transpose(mat3(cameraPoses[index])) * 
        (lightPositions[getLightIndex(index)].xyz - cameraPoses[index][3].xyz) - fPosition;
}

vec3 getLightIntensity(int index)
{
    return lightIntensities[getLightIndex(index)].rgb;
}

vec4 getColor(int index); // Defined by imgspace.glsl or texspace.glsl

vec4 getLinearColor(int index)
{
    return linearizeColor(getColor(index));
}

#endif // COLOR_APPEARANCE_GLSL
