#version 400

uniform mat4 model_view;
uniform mat4 projection;

in vec3 position;
in vec2 texCoord;
in vec3 normal;

out vec3 fPosition;
out vec2 fTexCoord;
out vec3 fNormal;

out vec3 fViewPos;

void main(void)
{
	gl_Position = projection * model_view * vec4(position, 1.0);
	fPosition = position;
	fTexCoord = texCoord;
	fNormal = normal;
	
	fViewPos = transpose(mat3(model_view)) * -model_view[3].xyz;
}
