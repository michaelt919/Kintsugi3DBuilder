#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 shadingInfo;

#include "colorappearance_multi_as_single.glsl"
#include "imgspace.glsl"

#line 16 1011

uniform bool lightIntensityCompensation;

void main()
{
    vec3 view = normalize(getViewVector());
    LightInfo lightInfo = getLightInfo();
    vec3 light = lightInfo.normalizedDirection;
    vec3 halfway = normalize(light + view);
    vec3 normal = normalize(fNormal);
    shadingInfo = vec4(dot(normal, light), dot(normal, view), dot(normal, halfway), dot(halfway, view));

    if (lightIntensityCompensation)
    {
        fragColor = vec4(xyzToLab(rgbToXYZ(getLinearColor().rgb / lightInfo.attenuatedIntensity)), 1.0);
    }
    else
    {
        fragColor = vec4(xyzToLab(rgbToXYZ(getLinearColor().rgb)), 1.0);
    }
}
