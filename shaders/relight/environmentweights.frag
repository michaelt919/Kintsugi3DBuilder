#version 330
#extension GL_ARB_texture_query_lod : enable

in vec2 fTexCoord;

layout(location = 0) out vec4 baseFresnel0;
layout(location = 1) out vec4 fresnelAdj0;
layout(location = 2) out vec4 baseFresnel1;
layout(location = 3) out vec4 fresnelAdj1;
layout(location = 4) out vec4 baseFresnel2;
layout(location = 5) out vec4 fresnelAdj2;
layout(location = 6) out vec4 baseFresnel3;
layout(location = 7) out vec4 fresnelAdj3;

uniform sampler2D positionMap;
uniform ivec2 blockOffset;

vec3 getPosition(vec2 texCoord)
{
    ivec2 mapSize = textureSize(positionMap, 0);
    return texelFetch(positionMap, min(ivec2(floor(texCoord * (mapSize - 1))), mapSize - 2) + blockOffset, 0).xyz;
}

#define fPosition (getPosition(fTexCoord))

uniform vec3 viewPos;
uniform mat4 envMapMatrix;
uniform mat4 model_view;
uniform mat4 fullProjection;

#include "environmentweights.glsl"

#line 34 0

uniform int startingSVIndex;
uniform sampler2D normalMap;
uniform sampler2D roughnessMap;

vec3 getNormal(vec2 texCoord)
{
    ivec2 mapSize = textureSize(normalMap, 0);
    vec3 normal = texelFetch(normalMap, min(ivec2(floor(texCoord * (mapSize - 1))), mapSize - 2) + blockOffset, 0).xyz;

    if (normal == vec3(0))
    {
        return vec3(0);
    }
    else
    {
        return normalize(normal);
    }
}

void main()
{
    // TODO weight RGB components by specular albedo RGB
    vec3 roughnessRGB = texture(roughnessMap, fTexCoord).rgb;
    float roughness = sqrt(1.0 / (getLuminance(1.0 / roughnessRGB * roughnessRGB)));

    EnvironmentResult[ACTIVE_EIGENTEXTURE_COUNT] results = computeSVDEnvironmentShading(startingSVIndex, fPosition, getNormal(fTexCoord), roughness);

    if (startingSVIndex == -1)
    {
        baseFresnel0 = results[0].baseFresnel;
    }
    else
    {
        baseFresnel0 = results[0].baseFresnel * vec4(0.5, 0.5, 0.5, 1.0) + vec4(0.5, 0.5, 0.5, 0.0);
    }

    baseFresnel1 = results[1].baseFresnel * vec4(0.5, 0.5, 0.5, 1.0) + vec4(0.5, 0.5, 0.5, 0.0);
    baseFresnel2 = results[2].baseFresnel * vec4(0.5, 0.5, 0.5, 1.0) + vec4(0.5, 0.5, 0.5, 0.0);
    baseFresnel3 = results[3].baseFresnel * vec4(0.5, 0.5, 0.5, 1.0) + vec4(0.5, 0.5, 0.5, 0.0);

    if (startingSVIndex == -1)
    {
        fresnelAdj0 = results[0].fresnelAdjustment;
    }
    else
    {
        fresnelAdj0 = results[0].fresnelAdjustment * vec4(0.5, 0.5, 0.5, 1.0) + vec4(0.5, 0.5, 0.5, 0.0);
    }

    fresnelAdj1 = results[1].fresnelAdjustment * vec4(0.5, 0.5, 0.5, 1.0) + vec4(0.5, 0.5, 0.5, 0.0);
    fresnelAdj2 = results[2].fresnelAdjustment * vec4(0.5, 0.5, 0.5, 1.0) + vec4(0.5, 0.5, 0.5, 0.0);
    fresnelAdj3 = results[3].fresnelAdjustment * vec4(0.5, 0.5, 0.5, 1.0) + vec4(0.5, 0.5, 0.5, 0.0);
}
