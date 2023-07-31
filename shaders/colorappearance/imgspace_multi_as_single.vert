#version 330

/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

#ifndef CAMERA_POSE_COUNT
#define CAMERA_POSE_COUNT 1024
#endif

#define CAMERA_POSE_COUNT_DIV_4 ((CAMERA_POSE_COUNT + 3) / 4)

#ifndef CAMERA_PROJECTION_COUNT
#define CAMERA_PROJECTION_COUNT 1024
#endif

#ifndef USE_VIEW_INDICES
#define USE_VIEW_INDICES 0
#endif

#if USE_VIEW_INDICES
layout(std140) uniform ViewIndices
{
    ivec4 viewIndices[VIEW_COUNT_DIV_4];
};
#endif

layout(std140) uniform CameraPoses
{
    mat4 cameraPoses[CAMERA_POSE_COUNT];
};

layout(std140) uniform CameraProjections
{
    mat4 cameraProjections[CAMERA_PROJECTION_COUNT];
};

layout(std140) uniform CameraProjectionIndices
{
    ivec4 cameraProjectionIndices[CAMERA_POSE_COUNT_DIV_4];
};

uniform int viewIndex;

#include "../common/extractcomponent.glsl"
#line 44 0

int getViewIndex(int virtualIndex)
{
#if USE_VIEW_INDICES
    return extractComponentByIndex(viewIndices[virtualIndex/4], virtualIndex%4);
#else
    return virtualIndex;
#endif
}

int getCameraProjectionIndex(int virtualIndex)
{
    int viewIndex = getViewIndex(virtualIndex);
    return extractComponentByIndex(cameraProjectionIndices[viewIndex/4], viewIndex%4);
}

#define MODEL_VIEW (cameraPoses[viewIndex])
#define PROJECTION (cameraProjections[getCameraProjectionIndex(viewIndex)])

in vec3 position;
in vec2 texCoord;
in vec3 normal;
in vec4 tangent;

out vec3 fPosition;
out vec2 fTexCoord;
out vec3 fNormal;
out vec3 fTangent;
out vec3 fBitangent;

void main(void)
{
    gl_Position = PROJECTION * MODEL_VIEW * vec4(position, 1.0);
    fPosition = position;
    fTexCoord = texCoord;
    fNormal = normal;
    fTangent = tangent.xyz;
    fBitangent = tangent.w * normalize(cross(normal, tangent.xyz));
}
