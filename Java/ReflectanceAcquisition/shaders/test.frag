#version 400

#define MAX_CAMERA_POSE_COUNT 256
#define MAX_CAMERA_PROJECTION_COUNT 256

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2DArray imageTextures;

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

uniform mat4 cameraProj;

void main()
{
	int testIndex = 10;

	vec4 projPos = cameraProjections[cameraProjectionIndices[testIndex]] * cameraPoses[testIndex] * vec4(fPosition, 1.0);
	projPos = projPos / projPos.w;
	
	vec2 texCoord = vec2((projPos.x / 2 + 0.5), (-projPos.y / 2 + 0.5));
	
	if (texCoord.x < 0 || texCoord.x > 1 || texCoord.y < 0 || texCoord.y > 1)
	{
		gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
	}
	else
	{
		gl_FragColor = texture(imageTextures, vec3(texCoord.xy, testIndex));
	}
}