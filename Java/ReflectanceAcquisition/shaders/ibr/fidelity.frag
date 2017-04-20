#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

#include "../reflectance/reflectance.glsl"
#include "../reflectance/imgspace.glsl"

#line 11 0

uniform float weightExponent;
uniform mat4 model_view;

uniform ViewIndices
{
	int viewIndices[MAX_CAMERA_POSE_COUNT];
};

uniform int targetViewIndex;

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
		sum += getSampleWeight(currentViewIndex) * getSample(currentViewIndex);
	}
	
	vec4 lfSample = getSample(targetViewIndex);
	
	if (sum.a <= 0.0)
	{
		return vec2(-1.0, -1.0);
	}
	else
	{
		vec3 diff = sum.rgb / sum.a - lfSample.rgb;
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
