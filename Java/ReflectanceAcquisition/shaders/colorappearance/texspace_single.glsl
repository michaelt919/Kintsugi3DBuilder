#ifndef TEXSPACE_SINGLE_GLSL
#define TEXSPACE_SINGLE_GLSL

#include "colorappearance_single.glsl"

#line 7 1110

uniform sampler2D viewImage;
uniform vec2 minTexCoord;
uniform vec2 maxTexCoord;

vec4 getColor()
{
    return texture(viewImage, (fTexCoord - minTexCoord) / (maxTexCoord - minTexCoord));
}

#endif // TEXSPACE_SINGLE_GLSL