#version 330

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_PROJECTION_COUNT 1024

uniform bool occlusionEnabled;
uniform float occlusionBias;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2D viewImage;
uniform sampler2D depthImage;

uniform mat4 cameraPose;
uniform mat4 cameraProjection;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 projTexCoord;

void main()
{
    projTexCoord = cameraProjection * cameraPose * vec4(fPosition, 1.0);
    projTexCoord /= projTexCoord.w;
    projTexCoord = (projTexCoord + vec4(1)) / 2;
	
	if (projTexCoord.x < 0 || projTexCoord.x > 1 || projTexCoord.y < 0 || projTexCoord.y > 1 ||
            projTexCoord.z < 0 || projTexCoord.z > 1)
	{
		discard;
	}
	else
	{
		if (occlusionEnabled)
		{
			float imageDepth = texture(depthImage, projTexCoord.xy).r;
			if (abs(projTexCoord.z - imageDepth) > occlusionBias)
			{
				// Occluded
				discard;
			}
		}
        
        vec3 view = normalize(transpose(mat3(cameraPose)) * -cameraPose[3].xyz - fPosition);
        vec3 normal = normalize(fNormal);
        fragColor = vec4(texture(viewImage, projTexCoord.xy).rgb, 1.0);
	}
}
