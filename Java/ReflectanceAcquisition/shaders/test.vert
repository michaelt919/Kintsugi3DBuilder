#version 130

uniform mat4 model_view;
uniform mat4 projection;

in vec3 position;
in vec2 texCoord;

out vec2 fTexCoord;

void main(void)
{
	gl_Position = projection * model_view * vec4(position, 1.0);
	fTexCoord = texCoord;
}
