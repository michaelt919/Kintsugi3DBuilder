#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 fragColor;

#define SMITH_MASKING_SHADOWING 1

#include <shaders/colorappearance/colorappearance_multi_as_single.glsl>
//#include <shaders/colorappearance/imgspace.glsl>
#include <shaders/colorappearance/analytic.glsl>

#line 18 0

#if NORMAL_TEXTURE_ENABLED
uniform sampler2D normalMap;

vec3 getNormal(vec2 texCoord)
{
    vec2 normalXY = texture(normalMap, texCoord).xy * 2 - 1;
    return vec3(normalXY, 1.0 - dot(normalXY, normalXY));
}
#endif

void main()
{    
    float maxLuminance;
    //maxLuminance = getMaxLuminance();
    maxLuminance = max(ANALYTIC_SPECULAR_COLOR.r, max(ANALYTIC_SPECULAR_COLOR.g, ANALYTIC_SPECULAR_COLOR.b))
            / (4 * ANALYTIC_ROUGHNESS * ANALYTIC_ROUGHNESS)
        + max(ANALYTIC_DIFFUSE_COLOR.r, max(ANALYTIC_DIFFUSE_COLOR.g, ANALYTIC_DIFFUSE_COLOR.b));

    vec4 sampleColor = getLinearColor();
    float luminance = getLuminance(sampleColor.rgb / getLightInfo().attenuatedIntensity);
    float secondLuminance = 0.0;
    
    for (int i = 0; i < VIEW_COUNT; i++)
    {
        if (i != viewIndex)
        {
            secondLuminance = max(secondLuminance,
                getLuminance(getLinearColor(i).rgb / getLightInfo(i).attenuatedIntensity));
                
            if (secondLuminance >= luminance)
            {
                fragColor = vec4(0);
                return;
            }
        }
    }
    
    vec3 normal = normalize(mat3(cameraPose) * fNormal);
    vec3 view = -normalize((cameraPose * vec4(fPosition, 1.0)).xyz);
    vec3 adjX = normalize(vec3(1,0,0) - view.x * view);
    vec3 adjY = normalize(vec3(0,1,0) - view.y * view - adjX.y * adjX);
    
    float luminanceGammaCorrected = pow(luminance / maxLuminance, 1.0 / 2.2);
    float secondLuminanceGammaCorrected = pow(secondLuminance / maxLuminance, 1.0 / 2.2);
    
    fragColor = vec4(dot(adjX, normal) + 0.5, dot(adjY, normal) + 0.5, 
        luminanceGammaCorrected - secondLuminanceGammaCorrected, max(0.005, luminanceGammaCorrected));
}
