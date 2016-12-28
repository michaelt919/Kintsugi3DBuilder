#version 330

uniform sampler2D env;
in vec2 fTexCoord;
out vec4 fragColor;

uniform mat4 model_view;
uniform mat4 projection;

uniform mat4 envMapMatrix;

#define PI 3.1415926535897932384626433832795

void main()
{
	vec4 unprojected = inverse(projection) * vec4(fTexCoord * 2 - vec2(1), 0, 1);

	vec4 viewDir = envMapMatrix * inverse(model_view) * (unprojected / unprojected.w);

    fragColor = texture(env, vec2(0.5 * atan(-viewDir.x, -viewDir.z), 
					atan(viewDir.y)) / PI + vec2(0.5));
}