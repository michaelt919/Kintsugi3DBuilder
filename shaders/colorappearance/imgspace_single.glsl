#ifndef IMGSPACE_SINGLE_GLSL
#define IMGSPACE_SINGLE_GLSL

#include "colorappearance_single.glsl"

#line 7 1111

#ifndef VISIBILITY_TEST_ENABLED
#define VISIBILITY_TEST_ENABLED 0
#endif

#ifndef SHADOW_TEST_ENABLED
#define SHADOW_TEST_ENABLED 0
#endif

uniform mat4 cameraProjection;
uniform sampler2D viewImage;

#if VISIBILITY_TEST_ENABLED || SHADOW_TEST_ENABLED
uniform float occlusionBias;
#endif

#if VISIBILITY_TEST_ENABLED
uniform sampler2D depthImage;
#endif

#if SHADOW_TEST_ENABLED
uniform sampler2D shadowImage;
uniform mat4 shadowMatrix;
#endif

vec4 getColor()
{
    vec4 projTexCoord = cameraProjection * cameraPose * vec4(fPosition, 1.0);
    projTexCoord /= projTexCoord.w;
    projTexCoord = (projTexCoord + vec4(1)) / 2;

    if (projTexCoord.x < 0 || projTexCoord.x > 1 || projTexCoord.y < 0 || projTexCoord.y > 1)
    {
        return vec4(0);
    }
    else
    {
#if VISIBILITY_TEST_ENABLED || SHADOW_TEST_ENABLED
        if (projTexCoord.z >= 0 && projTexCoord.z <= 1)
        {
#if VISIBILITY_TEST_ENABLED
            float imageDepth = texture(depthImage, projTexCoord.xy).r;
            if (abs(projTexCoord.z - imageDepth) > occlusionBias)
            {
                // Occluded
                return vec4(0);
            }
#endif

#if SHADOW_TEST_ENABLED
            vec4 shadowTexCoord = shadowMatrix * vec4(fPosition, 1.0);
            shadowTexCoord /= shadowTexCoord.w;
            shadowTexCoord = (shadowTexCoord + vec4(1)) / 2;

            if (shadowTexCoord.x >= 0 && shadowTexCoord.x <= 1 &&
                 shadowTexCoord.y >= 0 && shadowTexCoord.y <= 1 &&
                 shadowTexCoord.z >= 0 && shadowTexCoord.z <= 1)
            {
                float shadowImageDepth = texture(shadowImage, shadowTexCoord.xy).r;
                if (abs(shadowTexCoord.z - shadowImageDepth) > occlusionBias)
                {
                    // Occluded
                    return vec4(0);
                }
            }
#endif
        }
#endif

        return texture(viewImage, projTexCoord.xy);
    }
}

#endif // IMGSPACE_SINGLE_GLSL
