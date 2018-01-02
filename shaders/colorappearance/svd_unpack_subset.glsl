#ifndef SVD_UNPACK_GLSL
#define SVD_UNPACK_GLSL

#include "colorappearance_subset.glsl"

#line 7 1102

uniform sampler2DArray eigentextures;
uniform usampler2DArray viewWeightTextures;
uniform ivec2 blockSize;

#define MAX_EIGENTEXTURES 16
#define VIEW_WEIGHT_PACKING_X 4
#define VIEW_WEIGHT_PACKING_Y 4

vec4 getColor(int virtualIndex)
{
    int viewIndex = getViewIndex(virtualIndex);

    ivec3 eigentexturesSize = textureSize(eigentextures, 0);
    ivec3 blockStart = ivec3((min(ivec2(floor(fTexCoord * eigentexturesSize.xy)), eigentexturesSize.xy - ivec2(1))
                            / blockSize) * ivec2(VIEW_WEIGHT_PACKING_X, VIEW_WEIGHT_PACKING_Y), viewIndex);

    vec3 color = vec3(0.0);

    for (int k = 0; k < MAX_EIGENTEXTURES; k++)
    {
        uint packedViewWeights = texelFetch(viewWeightTextures, blockStart + ivec3(k % VIEW_WEIGHT_PACKING_X, k / VIEW_WEIGHT_PACKING_X, 0), 0)[0];
        uvec3 unpackedViewWeights = uvec3((packedViewWeights >> 9) & 0x7Fu, (packedViewWeights >> 4) & 0x1Fu, packedViewWeights & 0x0Fu);

        if (unpackedViewWeights[0] != 0u)
        {
            // TODO get mipmaps to work
            color += vec3(int(unpackedViewWeights[2] * 2u + unpackedViewWeights[0]) - 80,
                          int(unpackedViewWeights[0]) - 64,
                          int(unpackedViewWeights[1] * 2u + unpackedViewWeights[0]) - 96) / 63.0
                     * ((vec3(texture(eigentextures, vec3(fTexCoord, k))[0]) * 255 - 127) / 127.0);
        }
    }

    return vec4(color + vec3(0.5), 1.0);

}

#endif // SVD_UNPACK_GLSL
