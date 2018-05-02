#version 330

in vec2 fTexCoord;

layout(location = 0) out vec4 output0;
layout(location = 1) out vec4 output1;
layout(location = 2) out vec4 output2;
layout(location = 3) out vec4 output3;
layout(location = 4) out vec4 output4;
layout(location = 5) out vec4 output5;
layout(location = 6) out vec4 output6;
layout(location = 7) out vec4 output7;

uniform sampler2D input0;
uniform sampler2D input1;
uniform sampler2D input2;
uniform sampler2D input3;
uniform sampler2D input4;
uniform sampler2D input5;
uniform sampler2D input6;
uniform sampler2D input7;
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
        output4 = texture(input4, fTexCoord);
        output5 = texture(input5, fTexCoord);
        output6 = texture(input6, fTexCoord);
        output7 = texture(input7, fTexCoord);
    }
}