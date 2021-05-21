#version 330

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

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

#include "../colorappearance/colorappearance.glsl"
#include "../colorappearance/imgspace.glsl"

#line 11 0

uniform float weightExponent;
uniform mat4 model_view;

uniform bool perPixelWeightsEnabled;

layout(std140) uniform ViewWeights
{
    vec4 viewWeights[MAX_CAMERA_POSE_COUNT_DIV_4];
};

uniform ViewIndices
{
    int viewIndices[MAX_CAMERA_POSE_COUNT];
};

uniform int targetViewIndex;

float getViewWeight(int viewIndex)
{
    return extractComponentByIndex(viewWeights[viewIndex/4], viewIndex%4);
}

layout(location = 0) out vec2 fidelity;

float computeSampleWeight(float correlation)
{
    return 1.0 / max(0.000001, 1.0 - pow(max(0.0, correlation), weightExponent)) - 1.0;
}

float getSampleWeight(int index)
{
    vec3 cameraPos = (cameraPoses[index] * 
        vec4(transpose(mat3(model_view)) * -model_view[3].xyz, 1.0)).xyz;
    vec3 fragmentPos = (cameraPoses[index] * vec4(fPosition, 1.0)).xyz;

    return computeSampleWeight(dot(normalize(-fragmentPos), normalize(cameraPos - fragmentPos)));
}

vec4 getSample(int index)
{
    vec4 color = getLinearColor(index);

    //if (!infiniteLightSources)
    {
        vec3 light = getLightVector(index);
        color.rgb *= dot(light, light) / getLightIntensity(index);
    }

    return color;
}

vec2 computeFidelity()
{
    vec4 sum = vec4(0.0);
    for (int i = 0; i < viewCount; i++)
    {
        int currentViewIndex = viewIndices[i];
        if (perPixelWeightsEnabled)
        {
            sum += getSampleWeight(currentViewIndex) * getSample(currentViewIndex);
        }
        else
        {
            sum += getViewWeight(currentViewIndex) * getSample(currentViewIndex);
        }
    }

    vec4 lfSample = getSample(targetViewIndex);

    // if (sum.a <= 0.0)
    // {
        // return vec2(-1.0, -1.0);
    // }
    // else
    {
        //vec3 diff;

        // if (sum.a <= 0.0)
        // {
            // diff = -lfSample.rgb;
        // }
        // else if (perPixelWeightsEnabled)
        // {
            // diff = sum.rgb / sum.a - lfSample.rgb;
        // }
        // else
        // {
            // diff = sum.rgb - lfSample.rgb;
        // }

        float diff;

        if (sum.a <= 0.0)
        {
            diff = -getLuminance(lfSample.rgb);
        }
        else if (perPixelWeightsEnabled)
        {
            diff = getLuminance(sum.rgb / sum.a) - getLuminance(lfSample.rgb);
        }
        else
        {
            diff = getLuminance(sum.rgb) - getLuminance(lfSample.rgb);
        }

        return clamp(normalize(mat3(model_view) * fNormal).z, 0.0, 1.0) // n dot v
            * lfSample.a
            * vec2(dot(diff, diff), 1);
            //* 2 * vec2(sum.g / sum.a, lfSample.g);
    }
}

void main()
{
    fidelity = computeFidelity();
}
