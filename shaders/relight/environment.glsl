/*
 *  Copyright (c) Michael Tetzlaff 2022
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#ifndef ENVIRONMENT_GLSL
#define ENVIRONMENT_GLSL

#line 17 3003

#ifndef ENVIRONMENT_TEXTURE_ENABLED
#define ENVIRONMENT_TEXTURE_ENABLED 0
#endif

#ifndef SCREEN_SPACE_RAY_TRACING_ENABLED
#define SCREEN_SPACE_RAY_TRACING_ENABLED SHADOWS_ENABLED
#endif

uniform vec3 ambientColor;

#if ENVIRONMENT_TEXTURE_ENABLED
uniform samplerCube environmentMap;
uniform mat4 envMapMatrix;
uniform float environmentMipMapLevel;
uniform int diffuseEnvironmentMipMapLevel;
#endif

#if SCREEN_SPACE_RAY_TRACING_ENABLED

#ifndef MAX_RAYTRACING_SAMPLE_COUNT
#define MAX_RAYTRACING_SAMPLE_COUNT 32
#endif

//#include "shadow_mcguire.glsl"

#line 32 3003

#ifndef RAY_DEPTH_GRADIENT
#define RAY_DEPTH_GRADIENT 0.1
#endif

#ifndef RAY_DEPTH_BIAS
#define RAY_DEPTH_BIAS 0.00001
#endif

#ifndef RAY_LINEAR_DEPTH_BIAS
#define RAY_LINEAR_DEPTH_BIAS (RAY_DEPTH_GRADIENT * 0.05)
#endif

#ifndef RAY_START_RESOLUTION
#define RAY_START_RESOLUTION 256.0
#endif

#ifndef RAY_DISTANCE_FRACTION
#define RAY_DISTANCE_FRACTION 1.0
#endif

uniform sampler2D screenSpaceDepthBuffer;

float rayTest(vec2 screenSpaceCoord, float rayDepth, float gradientScale)
{
    float surfaceDepth = texture(screenSpaceDepthBuffer, screenSpaceCoord)[0];
    float surfaceDepthLinear = fullProjection[3][2] / (2 * (surfaceDepth + RAY_DEPTH_BIAS) - 1 + fullProjection[2][2]);
    return clamp((surfaceDepthLinear + RAY_LINEAR_DEPTH_BIAS - rayDepth) / gradientScale, 0, 1);
}

float shadowTest(vec3 position, vec3 direction)
{
    mat4 proj_model_view = fullProjection * model_view;

    vec4 projPos = proj_model_view * vec4(position, 1);
    vec4 screenSpacePos = projPos / projPos.w;

//    // This might be necessary when working with SVD:
//    projPos = fullProjection * vec4((model_view * vec4(position, 1)).xy,
//        -fullProjection[3][2] / (texture(screenSpaceDepthBuffer, screenSpacePos.xy * 0.5 + 0.5)[0] * 2 - 1 + fullProjection[2][2]), 1.0);
//    screenSpacePos = projPos / projPos.w;

    float dirScale = 1.0 / RAY_START_RESOLUTION;
    float iterationFactor = pow(RAY_START_RESOLUTION * RAY_DISTANCE_FRACTION, 1.0 / MAX_RAYTRACING_SAMPLE_COUNT);

//    1. Initialize step size.
//    2. Initialize ray position to the surface position + step size * w-component * normalized ray direction.
//    For each sample:
//        A. Depth test at ray position.
//        B. Increment ray position by step size * w-component * normalized ray-direction.
//        C. Multiply step size by a factor greater than 1.

    vec4 projDir = proj_model_view * vec4(direction, 0);
    vec4 projDirScaled = projDir * min(1.0, dirScale / length(projDir.xy));

    vec4 currentProjPos = projPos + projPos.w * projDirScaled;
    float currentGradientScale = dirScale * RAY_DEPTH_GRADIENT;

    float shadowed = 0;

    for (int i = 0; i < MAX_RAYTRACING_SAMPLE_COUNT; i++)
    {
        vec3 currentScreenSpacePos = currentProjPos.xyz / currentProjPos.w;

        if (abs(currentScreenSpacePos.x) < 1 && abs(currentScreenSpacePos.y) < 1 && currentScreenSpacePos.z < 1 && projDir.z > -0.99)
        {
            float scaledDiff = rayTest((currentScreenSpacePos.xy * 0.5 + 0.5), currentProjPos.w, currentGradientScale);
            shadowed = max(shadowed, 1.0 - scaledDiff);
        }

        currentProjPos += currentProjPos.w * projDirScaled;
        currentGradientScale += dirScale * RAY_DEPTH_GRADIENT;

        projDirScaled *= iterationFactor;
        dirScale *= iterationFactor;
    }

    return shadowed;
}

#endif

vec3 getEnvironmentFresnel(vec3 lightDirection, float fresnelFactor)
{
    vec3 result;
#if ENVIRONMENT_TEXTURE_ENABLED
    result = ambientColor * textureLod(environmentMap, mat3(envMapMatrix) * lightDirection,
            mix(environmentMipMapLevel, 0, fresnelFactor)).rgb;
#else
    result = ambientColor;
#endif
    return result;
}

vec3 getEnvironmentLod(vec3 lightPosition, vec3 lightDirection, float lod)
{
    vec3 result;
#if ENVIRONMENT_TEXTURE_ENABLED
    result = ambientColor * textureLod(environmentMap, mat3(envMapMatrix) * lightDirection, lod).rgb;
#else
    result = ambientColor;
#endif

#if SCREEN_SPACE_RAY_TRACING_ENABLED
    result *= (1 - shadowTest(lightPosition, lightDirection));


//    vec2 hitPixel;
//    vec3 hitPoint;
//    if (traceScreenSpaceRay(
//            (model_view * vec4(lightPosition, 1.0)).xyz,
//            (model_view * vec4(lightDirection, 0.0)).xyz,
//            fullProjection,
//            screenSpaceDepthBuffer,
//            0.0, // near plane z
//            0.005, // stride
//            0.0, // jitter
//            32, // max steps
//            2.0, // max distance
//            hitPixel, hitPoint))
//    {
//        return vec3(0);
//    }
#endif

    return result;
}

vec3 getEnvironment(vec3 lightPosition, vec3 lightDirection)
{
#if ENVIRONMENT_TEXTURE_ENABLED
    return getEnvironmentLod(lightPosition, lightDirection, environmentMipMapLevel);
#else
    return getEnvironmentLod(lightPosition, lightDirection, 0);
#endif
}

vec3 getEnvironment(vec3 lightPosition, vec3 lightDirection, float dOmega)
{
#if ENVIRONMENT_TEXTURE_ENABLED
    ivec2 envDims = textureSize(environmentMap, 0);
    return getEnvironmentLod(lightPosition, lightDirection, 0.5 * log2(6 * envDims.x * envDims.y * dOmega));
#else
    return getEnvironmentLod(lightPosition, lightDirection, 0);
#endif
}

vec3 getEnvironmentSpecular(vec3 lightDirection)
{
    vec3 result;
#if ENVIRONMENT_TEXTURE_ENABLED
    result = ambientColor * texture(environmentMap, mat3(envMapMatrix) * lightDirection).rgb;
#else
    result = ambientColor;
#endif
    return result;
}

vec3 getEnvironmentDiffuse(vec3 normalDirection)
{    vec3 result;

#if ENVIRONMENT_TEXTURE_ENABLED
    result = ambientColor * textureLod(environmentMap, mat3(envMapMatrix) * normalDirection, 
    diffuseEnvironmentMipMapLevel).rgb / 2; // Why divide by 2?  This doesn't seem right.
    // https://www.wolframalpha.com/input/?i=2+*+pi+*+%28integrate+sine+theta+*+cosine+theta+%2F+pi%3B+theta+from+0+to+pi%2F2%29
#else
    result = ambientColor;
#endif
    return result;
}

#endif // ENVIRONMENT_GLSL