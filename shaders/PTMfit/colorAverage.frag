#version 330
#include "PTMfit.glsl"
#line 4 0

out vec4 averageColor;
out vec4 outNormal;

void main()
{
    vec3 normal = normalize(fNormal);

    vec4 sum = vec4(0);

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        vec4 color = getLinearColor(i);
        vec3 lightDisplacement = getLightVector(i);
        vec3 lightDir = normalize(lightDisplacement);

        float nDotL = max(0, dot(normal, lightDir));

        // Technically a linear regression, not a simple average.
        sum += color.a * nDotL * vec4(color.rgb, nDotL);
    }

    averageColor = sum / max(1.0, sum.a);
    outNormal = vec4(normal, 1);
}
