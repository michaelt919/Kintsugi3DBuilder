#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 shadingInfo;
layout(location = 2) out vec4 projTexCoord;


#include "colorappearance_single.glsl"
#include "imgspace_single.glsl"

#line 18 1010

uniform bool lightIntensityCompensation;

void main()
{
    projTexCoord = cameraProjection * cameraPose * vec4(fPosition, 1.0);
    projTexCoord /= projTexCoord.w;
    projTexCoord = (projTexCoord + vec4(1)) / 2;

    if (projTexCoord.x < 0 || projTexCoord.x > 1 || projTexCoord.y < 0 || projTexCoord.y > 1
        || projTexCoord.z < 0 || projTexCoord.z > 1)
    {
        discard;
    }
    else
    {
        if (occlusionEnabled)
        {
            float imageDepth = texture(depthImage, projTexCoord.xy).r;
            if (abs(projTexCoord.z - imageDepth) > occlusionBias)
            {
                // Occluded
                discard;
            }
        }

        vec3 view = normalize(getViewVector());
        vec3 lightPreNormalized = getLightVector();
        vec3 attenuatedLightIntensity = //infiniteLightSources ? lightIntensity :
            lightIntensity / (dot(lightPreNormalized, lightPreNormalized));
        vec3 light = normalize(lightPreNormalized);
        vec3 halfway = normalize(light + view);
        vec3 normal = normalize(fNormal);
        shadingInfo = vec4(dot(normal, light), dot(normal, view), dot(normal, halfway),
            dot(halfway, view));

        if (lightIntensityCompensation)
        {
            fragColor = vec4(pow(getLinearColor().rgb / attenuatedLightIntensity, vec3(1.0 / gamma)), 1.0);
        }
        else
        {
            fragColor = vec4(texture(viewImage, projTexCoord.xy).rgb, 1.0);
        }
    }
}
