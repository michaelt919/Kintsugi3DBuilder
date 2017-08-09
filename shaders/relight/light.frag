#version 330

uniform int objectID;
uniform sampler2D lightTexture;
uniform vec3 color;

in vec3 fPosition;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out int fragObjectID;

void main()
{
	float intensity = texture(lightTexture, fPosition.xy / 2 + vec2(0.5))[0];
	
	if (intensity == 0.0)
	{
		discard;
	}
	else
	{
		fragColor = vec4(color * intensity, 1.0);
		fragObjectID = objectID;
	}
}