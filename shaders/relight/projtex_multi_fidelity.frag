#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 geomOut;

#include "colorappearance_multi_as_single.glsl"
#include "imgspace.glsl"

#line 16 1011

uniform bool lightIntensityCompensation;

void main()
{
    vec3 view = normalize(getViewVector());
    vec3 normal = normalize(fNormal);

    vec4 projPos = cameraProjections[getCameraProjectionIndex(viewIndex)] * cameraPose * fPosition;
    geomOut = 0.5 + 0.5 * projPos / projPos.w;
    //geomOut = vec4(normal * 0.5 + 0.5, 1.0);

    if (lightIntensityCompensation)
    {
        LightInfo lightInfo = getLightInfo();
        fragColor = vec4(getLuminance(getLinearColor().rgb / lightInfo.attenuatedIntensity), max(0, dot(normal, view)), 0.0, 1.0);
    }
    else
    {
        fragColor = vec4(getLuminance(getLinearColor().rgb), max(0, dot(normal, view)), 0.0, 1.0);
    }
}
