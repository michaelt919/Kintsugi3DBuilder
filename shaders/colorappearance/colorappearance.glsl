#ifndef COLOR_APPEARANCE_GLSL
#define COLOR_APPEARANCE_GLSL

#include "linearize.glsl"
#include "../common/extractcomponent.glsl"

#line 8 1000

#ifndef PI
#define PI 3.1415926535897932384626433832795 // For convenience
#endif

#ifndef CAMERA_POSE_COUNT
#define CAMERA_POSE_COUNT 1024
#endif

#define CAMERA_POSE_COUNT_DIV_4 ((CAMERA_POSE_COUNT + 3) / 4)

#ifndef LIGHT_COUNT
#define LIGHT_COUNT 1024
#endif

#ifndef VIEW_COUNT
#define VIEW_COUNT CAMERA_POSE_COUNT
#endif

#define VIEW_COUNT_DIV_4 ((VIEW_COUNT + 3) / 4)

#ifndef USE_VIEW_INDICES
#define USE_VIEW_INDICES 0
#endif

#if USE_VIEW_INDICES
layout(std140) uniform ViewIndices
{
    ivec4 viewIndices[VIEW_COUNT_DIV_4];
};
#endif

int getViewIndex(int virtualIndex)
{
#if USE_VIEW_INDICES
    return extractComponentByIndex(viewIndices[virtualIndex/4], virtualIndex%4);
#else
    return virtualIndex;
#endif
}

#ifndef INFINITE_LIGHT_SOURCES
#define INFINITE_LIGHT_SOURCES 0
#endif

layout(std140) uniform CameraWeights
{
    vec4 cameraWeights[CAMERA_POSE_COUNT_DIV_4];
};

layout(std140) uniform CameraPoses
{
    mat4 cameraPoses[CAMERA_POSE_COUNT];
};

layout(std140) uniform LightPositions
{
    vec4 lightPositions[LIGHT_COUNT];
};

layout(std140) uniform LightIntensities
{
    vec4 lightIntensities[LIGHT_COUNT];
};

layout(std140) uniform LightIndices
{
    ivec4 lightIndices[CAMERA_POSE_COUNT_DIV_4];
};

struct LightInfo
{
    vec3 attenuatedIntensity;
    vec3 normalizedDirection;
};

mat4 getCameraPose(int virtualIndex)
{
    return cameraPoses[getViewIndex(virtualIndex)];
}

int getLightIndex(int virtualIndex)
{
    int viewIndex = getViewIndex(virtualIndex);
    return extractComponentByIndex(lightIndices[viewIndex/4], viewIndex%4);
}

float getCameraWeight(int virtualIndex)
{
    int viewIndex = getViewIndex(virtualIndex);
    return extractComponentByIndex(cameraWeights[viewIndex/4], viewIndex%4);
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

LightInfo getLightInfo(int virtualIndex)
{
    LightInfo result;
    result.normalizedDirection = getLightVector(virtualIndex);
    result.attenuatedIntensity = getLightIntensity(virtualIndex);

    float lightDistSquared = dot(result.normalizedDirection, result.normalizedDirection);
    result.normalizedDirection *= inversesqrt(lightDistSquared);

#if !INFINITE_LIGHT_SOURCES
    result.attenuatedIntensity /= lightDistSquared;
#endif

    return result;
}

vec4 getColor(int virtualIndex); // Defined by imgspace.glsl or texspace.glsl

vec4 getLinearColor(int virtualIndex)
{
    return linearizeColor(getColor(virtualIndex));
}

#endif // COLOR_APPEARANCE_GLSL
