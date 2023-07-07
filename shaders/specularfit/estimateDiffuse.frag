#version 330

/*
 *  Copyright (c) Michael Tetzlaff 2022
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#include "specularFit.glsl"
#line 18 0

layout(location = 0) out vec4 diffuseOut;

void main()
{
    float sqrtRoughness = texture(roughnessEstimate, fTexCoord)[0];
    float roughness = sqrtRoughness * sqrtRoughness;

    vec3 position = getPosition();

    mat3 tangentToObject = constructTBNExact();
    vec3 triangleNormal = tangentToObject[2];

    vec2 fittedNormalXY = texture(normalEstimate, fTexCoord).xy * 2 - vec2(1.0);
    vec3 fittedNormalTS = vec3(fittedNormalXY, sqrt(1 - dot(fittedNormalXY, fittedNormalXY)));
    vec3 fittedNormal = tangentToObject * fittedNormalTS;

    vec4 diffuseSum = vec4(0);

    for (int k = 0; k < CAMERA_POSE_COUNT; k++)
    {
        vec4 imgColor = getLinearColor(k);
        vec3 view = normalize(getViewVector(k, position));
        float triangleNDotV = max(0.0, dot(triangleNormal, view));

        if (imgColor.a > 0.0 && triangleNDotV > 0.0)
        {
            vec3 lightDisplacement = getLightVector(k, position);
            vec3 light = normalize(lightDisplacement);
            vec3 halfway = normalize(light + view);
            float nDotH = max(0.0, dot(fittedNormal, halfway));
            float nDotL = max(0.0, dot(fittedNormal, light));
            float nDotV = max(0.0, dot(fittedNormal, view));

            // "Light intensity" is defined in such a way that we need to multiply by pi to be properly normalized.
            vec3 incidentRadiance = PI * getLightIntensity(k) / dot(lightDisplacement, lightDisplacement);

            vec3 actualReflectanceTimesNDotL = imgColor.rgb / incidentRadiance;

            if (nDotH > 0.0 && nDotL > 0.0 && nDotV > 0.0)
            {
                float hDotV = max(0.0, dot(halfway, view));
                float maskingShadowing = geom(roughness, nDotH, nDotV, nDotL, hDotV);
                vec3 specularEstimate = getMFDEstimate(nDotH) * maskingShadowing / (4 * nDotV);

                // Avoid overfitting to specular dominated samples
                float weight = sqrt(max(0, 1 - nDotH * nDotH));//max(0, getLuminance(getMFDEstimate(0.7)) - getLuminance(actualReflectanceTimesNDotL));

                vec3 diffuse = PI * (actualReflectanceTimesNDotL - specularEstimate); // could be negative
//                diffuseSum += vec4(pow(vec4(diffuse, nDotL), vec4(1.0 / gamma)));
                diffuseSum += vec4(weight * diffuse * nDotL * triangleNDotV, weight * nDotL * nDotL * triangleNDotV);
            }
        }
    }

    diffuseOut = vec4(pow(max(vec3(0), diffuseSum.rgb / diffuseSum.a), vec3(1.0 / gamma)), 1.0);
}
