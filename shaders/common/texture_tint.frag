#version 330

uniform vec3 color;
uniform sampler2D tex;
in vec2 fTexCoord;
out vec4 fragColor;

void main()
{
    fragColor = vec4(color, 1) * texture(tex, fTexCoord);
}