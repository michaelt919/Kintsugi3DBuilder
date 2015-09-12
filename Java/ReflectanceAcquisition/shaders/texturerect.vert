#version 330

in vec2 position;
out vec2 fTexCoord;

void main()
{
    gl_Position = vec4(position, 0.0, 1.0);
    fTexCoord = (position + vec2(1)) * 0.5;
}