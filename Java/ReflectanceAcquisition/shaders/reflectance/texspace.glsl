#ifndef TEXSPACE_GLSL
#define TEXSPACE_GLSL

#include "reflectance.glsl"

#line 7 1100

uniform sampler2DArray viewImages;
uniform vec2 minTexCoord;
uniform vec2 maxTexCoord;

vec4 getColor(int index)
{
    return pow(texture(viewImages, vec3((fTexCoord - minTexCoord) / (maxTexCoord - minTexCoord), index)), vec4(gamma));
}

#endif // TEXSPACE_GLSL