#version 330

#define MAX_CAMERA_POSE_COUNT 256
#define MAX_CAMERA_PROJECTION_COUNT 256

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2D diffuse;
uniform sampler2DArray textures;
uniform int textureIndex;
uniform float gamma;

uniform CameraPoses
{
	mat4 cameraPoses[MAX_CAMERA_POSE_COUNT];
};

layout(location = 0) out vec4 fragColor;

vec4 getOriginalColor(int index)
{
    return pow(texture(textures, vec3(fTexCoord, index)), vec4(gamma));
}

vec3 getViewVector(int index)
{
    return normalize(transpose(mat3(cameraPoses[index])) * -cameraPoses[index][3].xyz - fPosition);
}

vec3 getLightVector(int index)
{
    // TODO
    return getViewVector(index);
}

vec3 getDiffuseColor(int index)
{
    return pow(texture(diffuse, fTexCoord), vec4(gamma)).rgb * max(0, dot(fNormal, getLightVector(index)));
}

void main()
{
	vec4 original = getOriginalColor(textureIndex);
    fragColor = vec4(original.rgb - getDiffuseColor(textureIndex), original.a);
}
