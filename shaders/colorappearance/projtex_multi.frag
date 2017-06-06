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

#line 17 1011

void main()
{
    fragColor = getColor();
	
	vec3 view = normalize(getViewVector());
	vec3 lightPreNormalized = getLightVector();
	// vec3 attenuatedLightIntensity = // infiniteLightSources ? lightIntensity : 
		// lightIntensity / (dot(lightPreNormalized, lightPreNormalized));
	vec3 light = normalize(lightPreNormalized);
	vec3 halfway = normalize(light + view);
	vec3 normal = normalize(fNormal);
	shadingInfo = vec4(dot(normal, light), dot(normal, view), dot(normal, halfway), 
		dot(halfway, view));
		
	// fragColor = vec4(pow(getLinearColor().rgb / attenuatedLightIntensity, vec3(1.0 / gamma)), 1.0);
}
