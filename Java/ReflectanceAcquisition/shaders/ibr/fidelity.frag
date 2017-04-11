#version 330

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_PROJECTION_COUNT 1024

uniform bool occlusionEnabled;

uniform float weightExponent;
uniform float occlusionBias;
uniform float gamma;

uniform mat4 model_view;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;


uniform sampler2DArray viewImages;
uniform sampler2DArray depthImages;

uniform CameraPoses
{
	mat4 cameraPoses[MAX_CAMERA_POSE_COUNT];
};

uniform CameraProjections
{
	mat4 cameraProjections[MAX_CAMERA_PROJECTION_COUNT];
};

uniform CameraProjectionIndices
{
	int cameraProjectionIndices[MAX_CAMERA_POSE_COUNT];
};

uniform ViewIndices
{
	int viewIndices[MAX_CAMERA_POSE_COUNT];
};

uniform int targetViewIndex;
uniform int viewCount;

layout(location = 0) out vec2 fidelity;

float computeSampleWeight(vec3 cameraPos, vec3 samplePos, vec3 fragmentPos)
{
	return 1.0 / (1.0 - pow(max(0.0, dot(normalize(samplePos - fragmentPos), 
		normalize(cameraPos - fragmentPos))), weightExponent)) - 1.0;
}

float getSampleWeight(int index)
{
    return computeSampleWeight((cameraPoses[index] * vec4(transpose(mat3(model_view)) 
			* -model_view[3].xyz, 1.0)).xyz, vec3(0.0), 
        (cameraPoses[index] * vec4(fPosition, 1.0)).xyz);
}

vec4 getLightFieldSample(int index)
{
	vec4 fragPos = cameraPoses[index] * vec4(fPosition, 1.0);
	vec4 projTexCoord = cameraProjections[cameraProjectionIndices[index]] * fragPos;
	projTexCoord = projTexCoord / projTexCoord.w;
	projTexCoord = (projTexCoord + vec4(1)) / 2;
	
	if (projTexCoord.x < 0 || projTexCoord.x > 1 || projTexCoord.y < 0 || projTexCoord.y > 1
             || projTexCoord.z < 0 || projTexCoord.z > 1)
	{
		return vec4(0.0);
	}
	else
	{
		if (occlusionEnabled)
		{
			float imageDepth = texture(depthImages, vec3(projTexCoord.xy, index)).r;
			if (projTexCoord.z > imageDepth + occlusionBias)
			{
				// Occluded
				return vec4(0.0);
			}
		}
        
        vec4 color = texture(viewImages, vec3(projTexCoord.xy, index));
		
		return (color.a < 0.9999 ? 0.0 : 1.0) * vec4(pow(color.rgb, vec3(gamma)), 1.0);
	}
}

vec2 computeFidelity()
{
	vec4 sum = vec4(0.0);
	for (int i = 0; i < viewCount; i++)
	{
		int currentViewIndex = viewIndices[i];
		sum += getSampleWeight(currentViewIndex) * getLightFieldSample(currentViewIndex);
	}
	
	vec4 lfSample = getLightFieldSample(targetViewIndex);
	
	if (sum.a <= 0.0)
	{
		return vec2(0.0);
	}
	else
	{
		vec3 diff = sum.rgb / sum.a - lfSample.rgb;
		return clamp(normalize(mat3(model_view) * fNormal).z, 0.0, 1.0) // n dot v
			* lfSample.a * vec2(dot(diff, diff), 1);
		}
}

void main()
{
    fidelity = computeFidelity();
}
