#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

uniform sampler2D normalMap;
uniform bool useNormalMap;

layout(location = 0) out vec4 position;
layout(location = 1) out vec3 normal;

void main() 
{
    position = vec4(fPosition, 1.0);

    vec3 geometricNormal = normalize(fNormal);

    if (useNormalMap)
    {
        vec3 tangent = normalize(fTangent - dot(geometricNormal, fTangent));
        vec3 bitangent = normalize(fBitangent
            - dot(geometricNormal, fBitangent) * geometricNormal
            - dot(tangent, fBitangent) * tangent);

        mat3 tangentToObject = mat3(tangent, bitangent, geometricNormal);

        vec2 normalDirXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
        normal = tangentToObject * vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    }
    else
    {
        normal = geometricNormal;
    }
}
