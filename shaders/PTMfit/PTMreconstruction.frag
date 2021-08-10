#version 330
#include "PTMfit.glsl"

#line 14 0

uniform sampler2DArray weightMaps;
uniform int width;
uniform int length;

#ifndef BASIS_COUNT
#define BASIS_COUNT 6
#endif

layout(location = 0) out vec4 result;

void main()
{
    result=vec4(0,0,0,1);

    vec3 lightDisplacement = getLightVector();
    vec3 lightDir=normalize(lightDisplacement);
    float u=lightDir.x;
    float v=lightDir.y;
    float w=lightDir.z;

    vec3 weights[BASIS_COUNT];
    float row[BASIS_COUNT];
    row[0]=1.0f;
    row[1]=u;
    row[2]=v;
    row[3]=w;
    row[4]=u*u;
    row[5]=u*v;

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        weights[b] = texture(weightMaps, vec3(fTexCoord, b)).xyz;
        result=result+vec4(weights[b]*row[b],0);
        //result= vec4(vec3(fTexCoord, 0),1);
        result=pow(result,vec4(1/2.2));
    }

}
