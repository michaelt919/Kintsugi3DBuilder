#version 330

uniform sampler2D lightTexture;
uniform vec3 color;
in vec3 fPosition;
out vec4 fragColor;

void main()
{
    //fragColor = vec4(color, 1.0);
	fragColor = vec4(color * texture(lightTexture, fPosition.xy / 2 + vec2(0.5))[0], 1.0);
}