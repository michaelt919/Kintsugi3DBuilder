#version 400

#define MAX_CAMERA_POSE_COUNT 256
#define MAX_CAMERA_PROJECTION_COUNT 256

uniform float weightExponent;

uniform mat4 model_view;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

in vec3 fViewPos;

uniform sampler2DArray imageTextures;

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
	
	vec2 texCoord = vec2((projPos.x / 2 + 0.5), (-projPos.y / 2 + 0.5));
	
	if (texCoord.x < 0 || texCoord.x > 1 || texCoord.y < 0 || texCoord.y > 1)
	{
		return vec4(0.0, 0.0, 0.0, 0.0);
	}
	else
	{
		return computeSampleWeight((cameraPoses[index] * vec4(fViewPos, 1.0)).xyz, vec3(0.0), fragPos.xyz)
			* texture(imageTextures, vec3(texCoord.xy, index));
	}
}

vec4 computeLightField()
{
	vec4 sum = vec4(0.0);
	for (int i = 0; i < cameraPoseCount; i++)
	{
		sum += getLightFieldSample(i);
	}
	return sum / sum.w;
}

void main()
{
	gl_FragColor = computeLightField();
}