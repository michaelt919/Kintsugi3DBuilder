#version 330

uniform vec4 color;
uniform int objectID;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out int fragObjectID;

void main()
{
    fragColor = color;
    fragObjectID = objectID;
}