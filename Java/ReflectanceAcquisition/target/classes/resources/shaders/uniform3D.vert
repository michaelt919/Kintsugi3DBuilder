#version 330

uniform mat4 model_view;
uniform mat4 projection;

in vec3 position;

void main(void)
{
	gl_Position = projection * model_view * vec4(position, 1.0);
}
