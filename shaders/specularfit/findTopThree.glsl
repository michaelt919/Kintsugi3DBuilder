#line 2 4020

struct Sample
{
    float luminance;
    vec3 color;
    vec3 direction;
};

Sample mixSamples(Sample sample1, Sample sample2, float alpha)
{
    Sample result;
    result.luminance = mix(sample1.luminance, sample2.luminance, alpha);
    result.color = mix(sample1.color, sample2.color, alpha);
    result.direction = mix(sample1.direction, sample2.direction, alpha);
    return result;
}

Sample[3] findTopThree()
{
    Sample[3] topThree;

    for (int k = 0; k < CAMERA_POSE_COUNT; k++)
    {
        vec4 imgColor = getLinearColor(k);
        vec3 view = normalize(getViewVector(k));
        vec3 lightDisplacement = getLightVector(k);
        vec3 light = normalize(lightDisplacement);

        // "Light intensity" is defined in such a way that we need to multiply by pi to be properly normalized.
        vec3 incidentRadiance = PI * getLightIntensity(k) / dot(lightDisplacement, lightDisplacement);

        Sample newSample;
        newSample.direction = normalize(light + view); // halfway
        newSample.color = imgColor.rgb / incidentRadiance; // actual reflectance times n.l
        newSample.luminance = getLuminance(newSample.color);

        // 1 if new view should replace old 3rd highest (newSample >= topThree[2]); 0 otherwise (newSample < topThree[2])
        float step3 = step(topThree[2].luminance, newSample.luminance);

        // Potentially replace 3rd highest with current
        topThree[2] = mix(topThree[2], newSample, step3);

        // 1 if new view should replace old 2nd highest (newSample >= topThree[1]); 0 otherwise (newSample < topThree[1])
        float step2 = step(topThree[1].luminance, newSample.luminance);

        // Potentially replace 3rd highest with 2nd highest (shifting)
        topThree[2] = mix(topThree[2], topThree[1], step2);

        // Potentially replace 2nd highest with current
        topThree[1] = mix(topThree[1], newSample, step2);

        // 1 if new view should replace old 1st highest (newSample >= topThree[0]); 0 otherwise (newSample < topThree[0])
        float step1 = step(topThree[0].luminance, newSample.luminance);

        // Potentially replace 2nd highest with 1st highest (shifting; 2nd would already have been shifted earlier)
        topThree[1] = mix(topThree[1], topThree[0], step1);

        // Potentially replace 1st highest with current
        topThree[0] = mix(topThree[0], newSample, step1);
    }

    return topThree;
}