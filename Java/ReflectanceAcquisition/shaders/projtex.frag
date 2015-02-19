#version 330

#define MAX_CAMERA_POSE_COUNT 256
#define MAX_CAMERA_PROJECTION_COUNT 256

uniform bool occlusionEnabled;
uniform float occlusionBias;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2DArray imageTextures;
uniform sampler2DArray depthTextures;

uniform CameraPoses
{
	mat4 cameraPoses[MAX_CAMERA_POSE_COUNT];
};

uniform int cameraPoseIndex;

uniform CameraProjections
{
	mat4 cameraProjections[MAX_CAMERA_PROJECTION_COUNT];
};

uniform CameraProjectionIndices
{
	int cameraProjectionIndices[MAX_CAMERA_POSE_COUNT];
};

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 projPosMap;

void main()
{
	vec4 fragPos = cameraPoses[cameraPoseIndex] * vec4(fPosition, 1.0);
	vec4 projPos = cameraProjections[cameraProjectionIndices[cameraPoseIndex]] * fragPos;
	projPos = projPos / projPos.w;
	
	vec2 texCoord = vec2(projPos.x / 2 + 0.5, projPos.y / 2 + 0.5);
	
	if (texCoord.x < 0 || texCoord.x > 1 || texCoord.y < 0 || texCoord.y > 1)
	{
		discard;
	}
	else
	{
		if (occlusionEnabled)
		{
			float imageDepth = 2*texture(depthTextures, vec3(texCoord.xy, cameraPoseIndex)).x - 1;
			if (abs(projPos.z - imageDepth) > occlusionBias)
			{
				// Occluded
				discard;
			}
		}
		
		fragColor = vec4(texture(imageTextures, vec3(texCoord.xy, cameraPoseIndex)).rgb, 
                            max(0.0, (cameraPoses[cameraPoseIndex] * vec4(normalize(fNormal), 0.0)).z));
                            
        projPosMap = (projPos + vec4(1)) / 2;
	}
}
