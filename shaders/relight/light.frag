#version 330

uniform int objectID;
uniform sampler2D lightTexture;
uniform vec3 color;

in vec3 fPosition;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out int fragObjectID;

void main()
{
    //fragColor = vec4(color, 1.0);
	fragColor = vec4(color * texture(lightTexture, fPosition.xy / 2 + vec2(0.5))[0], 1.0);
	fragObjectID = objectID;
}