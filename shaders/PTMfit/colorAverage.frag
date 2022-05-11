#version 330
#include "PTMfit.glsl"
#line 4 0

out vec4 averageColor;
out vec4 unlitColor;

void main()
{
    vec3 normal = normalize(fNormal);

    vec4 sum = vec4(0);

    mat2 mATA = mat2(0);
    mat3x2 mATB = mat3x2(0);

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        vec4 color = getLinearColor(i);
        vec3 lightDisplacement = getLightVector(i);
        vec3 lightDir = normalize(lightDisplacement);

        float nDotL = max(0, dot(normal, lightDir));

        // Technically a linear regression, not a simple average.
        sum += color.a * nDotL * vec4(color.rgb, nDotL);

        mATA += color.a * outerProduct(vec2(nDotL, 1), vec2(nDotL, 1));
        mATB += color.a * outerProduct(vec2(nDotL, 1), color.rgb);
    }

    mat3x2 result = inverse(mATA) * mATB;

    averageColor = vec4(transpose(result)[0], 1);//sum / max(1.0, sum.a);
    unlitColor = vec4(transpose(result)[1], 1);
}
