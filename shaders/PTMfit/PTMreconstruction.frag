#version 330
#include "PTMfit.glsl"

#line 14 0

uniform sampler2DArray weightMaps;

#ifndef BASIS_COUNT
#define BASIS_COUNT 6
#endif

layout(location = 0) out float result;

void main()
{
    float result=0;
    vec3 lightDisplacement = getLightVector();
    vec3 lightDir=normalize(lightDisplacement);
    float u=lightDir.x;
    float v=lightDir.y;
    float w=lightDir.w;

    float weights[BASIS_COUNT];
    float row[BASIS_COUNT];
    row[0]=1.0f;
    row[1]=u;
    row[2]=v;
    row[3]=w;
    row[4]=u*u;
    row[5]=u*v;
    for (int b = 0; b < BASIS_COUNT; b++)
    {
        weights[b] = texture(weightMaps, vec3(fTexCoord, b))[0];
        result=result+weights[b]*row[b];
    }


}
