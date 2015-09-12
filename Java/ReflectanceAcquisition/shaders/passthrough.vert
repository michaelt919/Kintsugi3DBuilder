#version 330

in vec3 position;
in vec2 texCoord;
in vec3 normal;

out vec3 fPosition;
out vec2 fTexCoord;
out vec3 fNormal;

void main(void)
{
	gl_Position = vec4(position, 1.0);
	fPosition = position;
	fTexCoord = texCoord;
	fNormal = normal;
}
