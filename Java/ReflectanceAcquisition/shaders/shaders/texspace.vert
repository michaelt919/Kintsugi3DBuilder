#version 330

uniform vec2 minTexCoord;
uniform vec2 maxTexCoord;

in vec3 position;
in vec2 texCoord;
in vec3 normal;

out vec3 fPosition;
out vec2 fTexCoord;
out vec3 fNormal;

void main(void)
{
	gl_Position = vec4(2 * (texCoord - minTexCoord) / (maxTexCoord - minTexCoord) - vec2(1), 0.0, 1.0);
	fPosition = position;
	fTexCoord = texCoord;
	fNormal = normal;
}
