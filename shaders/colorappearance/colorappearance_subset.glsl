/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#ifndef COLOR_APPEARANCE_SUBSET_GLSL
#define COLOR_APPEARANCE_SUBSET_GLSL

#include "linearize.glsl"
#include "../common/extractcomponent.glsl"

#line 7 1003

#define PI 3.1415926535897932384626433832795 // For convenience

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_POSE_COUNT_DIV_4 256
#define MAX_LIGHT_COUNT 1024

uniform int viewCount;
uniform bool infiniteLightSources;

uniform bool useViewIndices;

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
    if (useViewIndices)
    {
        return extractComponentByIndex(viewIndices[virtualIndex/4], virtualIndex%4);
    }
    else
    {
        return virtualIndex;
    }
}

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

vec4 getColor(int virtualIndex); // Defined by imgspace_subset.glsl or texspace_subset.glsl

vec4 getLinearColor(int virtualIndex)
{
    return linearizeColor(getColor(virtualIndex));
}

#endif // COLOR_APPEARANCE_SUBSET_GLSL
