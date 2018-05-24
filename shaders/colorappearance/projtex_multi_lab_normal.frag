#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 normalOut;

#include "colorappearance_multi_as_single.glsl"
#include "imgspace.glsl"

#line 16 1011

uniform bool lightIntensityCompensation;

void main()
{
    normalOut = vec4(normalize(fNormal) * 0.5 + 0.5, 1.0);

    if (lightIntensityCompensation)
    {
        LightInfo lightInfo = getLightInfo();
        fragColor = vec4(xyzToLab(rgbToXYZ(getLinearColor().rgb / lightInfo.attenuatedIntensity)), 1.0);
    }
    else
    {
        fragColor = vec4(xyzToLab(rgbToXYZ(getLinearColor().rgb)), 1.0);
    }
}
