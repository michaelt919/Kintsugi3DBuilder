#ifndef SVD_UNPACK_GLSL
#define SVD_UNPACK_GLSL

#include "colorappearance_subset.glsl"

#line 7 1102

uniform sampler2DArray eigentextures;
uniform usampler2DArray viewWeightTextures;
uniform ivec2 blockSize;
uniform ivec2 viewWeightPacking;

vec4 getColor(int virtualIndex)
{
    int viewIndex = getViewIndex(virtualIndex);

    ivec3 eigentexturesSize = textureSize(eigentextures, 0);
    ivec3 blockStart = ivec3(min(floor(fTexCoord * eigentexturesSize.xy), eigentexturesSize.xy - ivec2(1))
                            / blockSize * viewWeightPacking, viewIndex);

    vec3 color = vec3(0.0);

    for (int k = 0; k < eigentexturesSize[2]; k++)
    {
        uint packedViewWeights = texelFetch(viewWeightTextures, blockStart + ivec3(k % viewWeightPacking[0], k / viewWeightPacking[0], 0), 0)[0];
        uvec3 unpackedViewWeights = uvec3(packedViewWeights & 0x1Fu, (packedViewWeights >> 5) & 0x3Fu, (packedViewWeights >> 11) & 0x1Fu);

        if (unpackedViewWeights.x != 0u && unpackedViewWeights.y != 0u && unpackedViewWeights.z != 0u)
        {
            color += (unpackedViewWeights - vec3(16, 32, 16)) / vec3(15, 31, 15) * texture(eigentextures, vec3(fTexCoord, k))[0];
        }
    }

    return vec4(color, 1.0);
}

#endif // SVD_UNPACK_GLSL
