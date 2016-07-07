#version 330

in vec2 fTexCoord;

layout(location = 0) out vec4 output0;
layout(location = 1) out vec4 output1;
layout(location = 2) out vec4 output2;
layout(location = 3) out vec4 output3;

uniform sampler2D input0;
uniform sampler2D input1;
uniform sampler2D input2;
uniform sampler2D input3;
uniform sampler2D alphaMask;

void main()
{
	if (texture(alphaMask, fTexCoord).x < 1.0)
	{
		discard;
	}
	else
	{
		output0 = texture(input0, fTexCoord);
		output1 = texture(input1, fTexCoord);
		output2 = texture(input2, fTexCoord);
		output3 = texture(input3, fTexCoord);
	}
}