#version 450
#include "PTMfit.glsl"

#line 5 0

uniform sampler2DArray weightMaps;

layout(location = 0) out vec4 objWeight0;
layout(location = 1) out vec4 objWeight1;
layout(location = 2) out vec4 objWeight2;
layout(location = 3) out vec4 objWeight3;

void main()
{
    vec3 triangleNormal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(triangleNormal, fBitangent) * triangleNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);

    vec3 weights[BASIS_COUNT];
    for (int b = 0; b < BASIS_COUNT; b++)
    {
        weights[b] = texture(weightMaps, vec3(fTexCoord, b)).xyz;
    }

    objWeight0 = vec4(weights[0]*0.5+0.5, 1); // Constant term is the same

    // Transformation from tangent-space direction to weights
    mat3 linearWeights = mat3(weights[1], weights[2], weights[3]); // 1: tangent; 2: bitangent; 3: normal

    // Transformation from object-space direction to weights
    mat3 objectSpaceLinearWeights = linearWeights * transpose(tangentToObject);
    objWeight1 = vec4(vec3(triangleNormal.x*0.5+0.5), 1); //vec4(objectSpaceLinearWeights[0]*0.5+0.5, 1);
    objWeight2 = vec4(vec3(triangleNormal.y*0.5+0.5), 1); //vec4(objectSpaceLinearWeights[1]*0.5+0.5, 1);
    objWeight3 = vec4(vec3(triangleNormal.z*0.5+0.5), 1); //vec4(objectSpaceLinearWeights[2]*0.5+0.5, 1);
}
