#version 330
#include "PTMfit.glsl"

#line 6 0

uniform sampler2DArray weightMaps;
uniform int width;
uniform int length;

#ifndef BASIS_COUNT
#define BASIS_COUNT 6
#endif

uniform vec3 reconstructionLightPos;
uniform vec3 reconstructionLightIntensity;

layout(location = 0) out vec4 result;

void main()
{
    result=vec4(0,0,0,1);

    vec3 lightDisplacement = reconstructionLightPos - fPosition;
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
    }

    vec3 incidentRadiance = PI * reconstructionLightIntensity / dot(lightDisplacement, lightDisplacement);

    result = pow(result * vec4(incidentRadiance, 1),vec4(1/2.2));
}
