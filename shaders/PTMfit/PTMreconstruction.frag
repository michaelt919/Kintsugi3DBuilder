#version 330
#include "PTMfit.glsl"
#include "evaluateBRDF.glsl"
#line 18 0

uniform sampler2D diffuseEstimate;
uniform sampler2DArray weightMaps;

#ifndef BASIS_COUNT
#define BASIS_COUNT 8
#endif

#ifndef MICROFACET_DISTRIBUTION_RESOLUTION
#define MICROFACET_DISTRIBUTION_RESOLUTION 90
#endif

layout(location = 0) out vec4 Color;

void main()
{
    vec3 f0 = vec3(0);

    float weights[BASIS_COUNT];

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        weights[b] = texture(weightMaps, vec3(fTexCoord, b))[0];
        f0 += weights[b] * texelFetch(basisFunctions, ivec2(0, b), 0).rgb;
    }

    vec3 sqrtF0 = sqrt(f0);

    vec3 sumNumerator = vec3(0);
    vec3 sumDenominator = vec3(0);

    for (int m = 1; m < MICROFACET_DISTRIBUTION_RESOLUTION; m++)
    {
        vec3 f = vec3(0);
        for (int b = 0; b < BASIS_COUNT; b++)
        {
            f += weights[b] * texelFetch(basisFunctions, ivec2(m, b), 0).rgb;
        }

        float sqrtAngle = float(m) / float(MICROFACET_DISTRIBUTION_RESOLUTION);
        float nDotH = cos(sqrtAngle * sqrtAngle * PI / 3.0);
        float nDotHSq = nDotH * nDotH;

        vec3 numerator = (1.0 - nDotHSq) * sqrt(f);
        vec3 denominator = sqrt(f0) - nDotHSq * sqrt(f);

        sumNumerator += pow(numerator * denominator, vec3(1.0 / fittingGamma));
        sumDenominator += pow(denominator * denominator, vec3(1.0 / fittingGamma));
    }

    vec3 fresnel = PI * f0 * pow(sumNumerator / sumDenominator, vec3(fittingGamma));
    Color = vec4(pow(fresnel, vec3(1.0 / gamma)), 1.0);
}
