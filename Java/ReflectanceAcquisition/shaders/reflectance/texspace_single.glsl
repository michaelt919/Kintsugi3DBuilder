#ifndef TEXSPACE_SINGLE_GLSL
#define TEXSPACE_SINGLE_GLSL

#include "reflectance_single.glsl"

#line 7 1110

uniform sampler2D viewImage;
uniform vec2 minTexCoord;
uniform vec2 maxTexCoord;

vec4 getColor()
{
    return pow(texture(viewImage, (fTexCoord - minTexCoord) / (maxTexCoord - minTexCoord)), vec4(gamma));
}

#endif // TEXSPACE_SINGLE_GLSL