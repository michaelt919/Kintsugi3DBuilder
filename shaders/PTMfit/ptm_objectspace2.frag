#version 450
#include "PTMfit.glsl"

#line 5 0

uniform sampler2DArray weightMaps;

layout(location = 0) out vec4 objWeight4;
layout(location = 1) out vec4 objWeight5;
layout(location = 2) out vec4 objWeight6;
layout(location = 3) out vec4 objWeight7;
layout(location = 4) out vec4 objWeight8;
layout(location = 5) out vec4 objWeight9;

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
        // Scale by PI to match Unity's convention and maximize precision
        weights[b] = PI * texture(weightMaps, vec3(fTexCoord, b)).xyz;
    }

    mat3 tanBitOuter = outerProduct(tangent, bitangent);
    mat3 tanTanOuter = outerProduct(tangent, tangent);
    mat3 bitBitOuter = outerProduct(bitangent, bitangent);

    //    u*v = dot(l, tangent) * dot(l, bitangent)
    //        = (l.x * t.x + l.y * t.y + l.z * t.z) * (l.x * b.x + l.y * b.y + l.z * b.z)

    // weight 4: u*v; weight 5: u^2+v^2

    // x^2
    objWeight4 = vec4(0.5 * weights[4] * tanBitOuter[0][0] + 0.5 * weights[5] * tanTanOuter[0][0] + 0.5 * weights[5] * bitBitOuter[0][0] + 0.5, 1);

    // y^2
    objWeight5 = vec4(0.5 * weights[4] * tanBitOuter[1][1] + 0.5 * weights[5] * tanTanOuter[1][1] + 0.5 * weights[5] * bitBitOuter[1][1] + 0.5, 1);

    // z^2
    objWeight6 = vec4(0.5 * weights[4] * tanBitOuter[2][2] + 0.5 * weights[5] * tanTanOuter[2][2] + 0.5 * weights[5] * bitBitOuter[2][2] + 0.5, 1);

    // x*y
    // Each term would be multiplied by 2 but then goes into the (0.5 * x + 0.5) formula for non-negative image files.
    objWeight7 = vec4(weights[4] * tanBitOuter[0][1] + weights[5] * tanTanOuter[0][1] + weights[5] * bitBitOuter[0][1] + 0.5, 1);

    // x*z
    objWeight8 = vec4(weights[4] * tanBitOuter[0][2] + weights[5] * tanTanOuter[0][2] + weights[5] * bitBitOuter[0][2] + 0.5, 1);

    // y*z
    objWeight9 = vec4(weights[4] * tanBitOuter[1][2] + weights[5] * tanTanOuter[1][2] + weights[5] * bitBitOuter[1][2] + 0.5, 1);
}
