#version 330

in vec2 fTexCoord;

layout(location = 0) out vec2 errorResultOut;
layout(location = 1) out float mask;

uniform sampler2D oldErrorTexture;
uniform sampler2D currentErrorTexture;

void main()
{
    vec4 currentError = texture(currentErrorTexture, fTexCoord);
    vec4 oldError = texture(oldErrorTexture, fTexCoord);

    if ((currentError.w == 1.0 || currentError.w > oldError.w) && oldError.y > currentError.y)
    {
        errorResultOut = vec2(0.0, currentError);
        mask = 1;
    }
    else
    {
        errorResultOut = vec2(0.0, oldError);
        mask = 0;
    }
}
