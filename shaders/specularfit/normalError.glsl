#line 2 4010

float calculateError(vec3 triangleNormal, vec3 estimatedNormal)
{
    float error = 0.0;

    for (int k = 0; k < CAMERA_POSE_COUNT; k++)
    {
        vec4 imgColor = getLinearColor(k);
        vec3 view = normalize(getViewVector(k));
        float triangleNDotV = max(0.0, dot(triangleNormal, view));

        vec3 lightDisplacement = getLightVector(k);
        vec3 light = normalize(lightDisplacement);
        vec3 halfway = normalize(light + view);
        float nDotH = max(0.0, dot(estimatedNormal, halfway));
        float nDotL = max(0.0, dot(estimatedNormal, light));
        float nDotV = max(0.0, dot(estimatedNormal, view));

        // "Light intensity" is defined in such a way that we need to multiply by pi to be properly normalized.
        vec3 incidentRadiance = PI * getLightIntensity(k) / dot(lightDisplacement, lightDisplacement);

        vec3 actualReflectanceTimesNDotL = imgColor.rgb / incidentRadiance;

        // n dot l is already incorporated by virtue of the fact that radiance is being optimized, not reflectance.
        float weight = imgColor.a * triangleNDotV * sqrt(max(0, 1 - nDotH * nDotH));

        if (nDotH > COSINE_CUTOFF && nDotL > COSINE_CUTOFF && nDotV > COSINE_CUTOFF)
        {
            float hDotV = max(0.0, dot(halfway, view));

            float roughness = texture(roughnessEstimate, fTexCoord)[0];
            float maskingShadowing = geom(roughness, nDotH, nDotV, nDotL, hDotV);

            vec3 reflectanceEstimate = getBRDFEstimate(nDotH, maskingShadowing / (4 * nDotL * nDotV));

            vec3 diff = reflectanceEstimate * nDotL - actualReflectanceTimesNDotL;
            error += weight * dot(diff, diff);
        }
        else
        {
            error += sign(imgColor.a * triangleNDotV) * weight * dot(actualReflectanceTimesNDotL, actualReflectanceTimesNDotL);
        }
    }

    return error;
}
