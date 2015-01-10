#version 330

#define MAX_CAMERA_POSE_COUNT 256
#define MAX_CAMERA_PROJECTION_COUNT 256

uniform bool occlusionEnabled;

uniform float weightExponent;
uniform float occlusionBias;
uniform float gamma;

uniform mat4 model_view;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

in vec3 fViewPos;

uniform sampler2DArray imageTextures;
uniform sampler2DArray depthTextures;
uniform sampler2D testTexture;

uniform CameraPoses
{
	mat4 cameraPoses[MAX_CAMERA_POSE_COUNT];
};

uniform int cameraPoseCount;

uniform CameraProjections
{
	mat4 cameraProjections[MAX_CAMERA_PROJECTION_COUNT];
};

uniform CameraProjectionIndices
{
	int cameraProjectionIndices[MAX_CAMERA_POSE_COUNT];
};

uniform mat4 cameraProj;

layout(location = 0) out vec4 fragColor;

float computeSampleWeight(vec3 cameraPos, vec3 samplePos, vec3 fragmentPos)
{
	return 1.0 / (1.0 - pow(max(0.0, dot(normalize(samplePos - fragmentPos), 
		normalize(cameraPos - fragmentPos))), weightExponent)) - 1.0;
}

vec4 getLightFieldSample(int index)
{
	vec4 fragPos = cameraPoses[index] * vec4(fPosition, 1.0);
	vec4 projPos = cameraProjections[cameraProjectionIndices[index]] * fragPos;
	projPos = projPos / projPos.w;
	
	vec2 texCoord = vec2(projPos.x / 2 + 0.5, projPos.y / 2 + 0.5);
	
	if (texCoord.x < 0 || texCoord.x > 1 || texCoord.y < 0 || texCoord.y > 1)
	{
		return vec4(0.0);
	}
	else
	{
		if (occlusionEnabled)
		{
			float imageDepth = 2*texture(depthTextures, vec3(texCoord.xy, index)).x - 1;
			if (abs(projPos.z - imageDepth) > occlusionBias)
			{
				// Occluded
				return vec4(0.0);
			}
		}
		
		return computeSampleWeight((cameraPoses[index] * vec4(fViewPos, 1.0)).xyz, vec3(0.0), fragPos.xyz)
			* pow(texture(imageTextures, vec3(texCoord.xy, index)), vec4(gamma));
	}
}

vec4 computeLightField()
{
	vec4 sum = vec4(0.0);
	for (int i = 0; i < cameraPoseCount; i++)
	{
		sum += getLightFieldSample(i);
	}
	return pow(sum / sum.w, vec4(1 / gamma));
}

void main()
{
	fragColor = computeLightField();
}
