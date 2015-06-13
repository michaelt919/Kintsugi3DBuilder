#version 330

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_PROJECTION_COUNT 1024
#define MAX_LIGHT_POSITION_COUNT 1024

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2D diffuse;
uniform sampler2D normalMap;
uniform sampler2DArray viewImages;
uniform sampler2DArray depthImages;
uniform int viewIndex;
uniform float gamma;
uniform bool occlusionEnabled;
uniform float occlusionBias;
uniform float diffuseRemovalFactor;

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

uniform LightPositions
{
	vec4 lightPositions[MAX_LIGHT_POSITION_COUNT];
};

uniform LightIndices
{
	int lightIndices[MAX_CAMERA_POSE_COUNT];
};

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 rDotV;

vec4 getOriginalColor(int index)
{
    vec4 projTexCoord = cameraProjections[cameraProjectionIndices[index]] * cameraPoses[index] * 
                            vec4(fPosition, 1.0);
    projTexCoord /= projTexCoord.w;
    projTexCoord = (projTexCoord + vec4(1)) / 2;
	
	if (projTexCoord.x < 0 || projTexCoord.x > 1 || projTexCoord.y < 0 || projTexCoord.y > 1 ||
            projTexCoord.z < 0 || projTexCoord.z > 1)
	{
		return vec4(0);
	}
	else
	{
		if (occlusionEnabled)
		{
			float imageDepth = texture(depthImages, vec3(projTexCoord.xy, index)).r;
			if (abs(projTexCoord.z - imageDepth) > occlusionBias)
			{
				// Occluded
				return vec4(0);
			}
		}
        
        return pow(texture(viewImages, vec3(projTexCoord.xy, index)), vec4(gamma));
	}
}

vec3 getViewVector(int index)
{
    return normalize(transpose(mat3(cameraPoses[index])) * -cameraPoses[index][3].xyz - fPosition);
}

vec3 getLightVector(int index)
{
    return normalize(transpose(mat3(cameraPoses[index])) * 
        (lightPositions[lightIndices[index]].xyz - cameraPoses[index][3].xyz) - fPosition);
}

vec3 getNormalVector()
{
    return normalize(texture(normalMap, fTexCoord).xyz * 2 - vec3(1));
}

vec3 getDiffuseColor(int index)
{
    return pow(texture(diffuse, fTexCoord), vec4(gamma)).rgb * max(0, dot(fNormal, getLightVector(index)));
}

vec3 getReflectionVector(vec3 normalVector, vec3 lightVector)
{
    return normalize(2 * dot(lightVector, normalVector) * normalVector - lightVector);
}

void main()
{
	vec4 original = getOriginalColor(viewIndex);
    fragColor = vec4(original.rgb - diffuseRemovalFactor * getDiffuseColor(viewIndex), original.a);
    
    vec3 view = getViewVector(viewIndex);
    vec3 light = getLightVector(viewIndex);
    vec3 normal = getNormalVector();
    vec3 reflection = getReflectionVector(normal, light);
    rDotV = vec4(vec3(dot(reflection, view)), 1.0);
}
