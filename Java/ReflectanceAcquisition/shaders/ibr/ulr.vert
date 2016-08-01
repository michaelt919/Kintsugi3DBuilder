#version 330

uniform mat4 model_view;
uniform mat4 projection;

in vec3 position;
in vec2 texCoord;
in vec3 normal;
in vec4 tangent;

out vec3 fPosition;
out vec2 fTexCoord;
out vec3 fNormal;
out vec3 fTangent;
out vec3 fBitangent;

out vec3 fViewPos;

#define MAX_CAMERA_PROJECTION_COUNT 1024

uniform CameraProjections
{
	mat4 cameraProjections[MAX_CAMERA_PROJECTION_COUNT];
};

void main(void)
{
    gl_Position = projection * model_view * vec4(position, 1.0);
	fPosition = position;
	fTexCoord = texCoord;
	fNormal = normal;
    fTangent = tangent.xyz;
    fBitangent = tangent.w * normalize(cross(normal, tangent.xyz));
	
	fViewPos = transpose(mat3(model_view)) * -model_view[3].xyz;
}
