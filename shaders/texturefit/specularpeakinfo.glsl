#ifndef PREPROCESSINFO_GLSL
#define PREPROCESSINFO_GLSL

#line 5 2007

struct SpecularPeakInfo
{
    vec4 peak;
    vec4 offPeakSum;
    float peakNDotH;
};

SpecularPeakInfo computeSpecularPeakInfo()
{
    SpecularPeakInfo result;

    vec3 normal = normalize(fNormal);

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        vec4 color = getLinearColor(i);
        LightInfo lightInfo = getLightInfo(i);
        vec3 colorAdj = color.rgb / lightInfo.attenuatedIntensity;

        vec3 view = normalize(getViewVector(i));
        float nDotV = max(0, dot(normal, view));
        float nDotH = max(0, dot(normal, normalize(lightInfo.normalizedDirection + view)));
        float offPeakWeight = 1 - nDotH * nDotH;
        vec4 colorWeighted = getCameraWeight(i) * offPeakWeight * offPeakWeight * nDotV * color.a * vec4(colorAdj, 1);

        result.offPeakSum += colorWeighted;

        result.peak = max(result.peak, sign(max(0, nDotV - 0.5) * color.a) * vec4(colorAdj, 1));
        result.peakNDotH = max(result.peakNDotH, nDotH);
    }

    result.offPeakSum = pow(result.offPeakSum / result.offPeakSum.a, vec4(vec3(1.0 / 2.2), 1.0));

#if SINGLE_VIEW_MASKING_ENABLED
    LightInfo lightInfo = getLightInfo();
    vec4 color = getLinearColor();
    vec3 colorAdj = color.rgb / lightInfo.attenuatedIntensity;

    vec3 view = normalize(getViewVector());
    float nDotV = dot(normal, view);
    result.peak *= max(0, sign(max(0, nDotV - 0.5) * color.a * max(0, getLuminance(colorAdj) - getLuminance(result.peak.rgb) + 0.001)));
#endif

    return result;
}

#endif // PREPROCESSINFO_GLSL