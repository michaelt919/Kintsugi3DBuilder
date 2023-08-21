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
#line 17 0

layout(location = 0) out vec4 diffuseOut;
layout(location = 1) out vec4 constantOut;
//layout(location = 2) out vec4 quadraticOut;

void main()
{
    float sqrtRoughness = texture(roughnessMap, fTexCoord)[0];
    float roughness = sqrtRoughness * sqrtRoughness;

    vec3 position = getPosition();

    mat3 tangentToObject = constructTBNExact();
    vec3 triangleNormal = tangentToObject[2];

    vec2 fittedNormalXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
    vec3 fittedNormalTS = vec3(fittedNormalXY, sqrt(1 - dot(fittedNormalXY, fittedNormalXY)));
    vec3 fittedNormal = tangentToObject * fittedNormalTS;

    float sumWeights = 0.0;
    vec4 weightedSums = vec4(0);
    vec4 cosineWeightedSums = vec4(0);

//    mat3 mATA = mat3(0);
//    mat3 vATb = mat3(0);

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
                float weight = sqrt(max(0, 1 - nDotH * nDotH));

                vec3 diffuse = PI * (actualReflectanceTimesNDotL - specularEstimate); // could be negative

                sumWeights += weight * triangleNDotV;
                weightedSums +=  weight * triangleNDotV *  vec4(diffuse, nDotL);
                cosineWeightedSums += weight *  triangleNDotV * vec4(diffuse * nDotL, nDotL * nDotL);

//                vec3 w = vec3(1, nDotL, nDotL * nDotL);
//                mATA += weight * triangleNDotV * outerProduct(w, w);
//                vATb += weight * triangleNDotV * outerProduct(w, diffuse);
            }
        }
    }

//    if (determinant(mATA) > 0)
//    {
//        // In linear system to solve, each row is a term of the model (constant, linear, quadratic),
//        // while RGB is stored across each row of vATb and the corresponding solution
//        // We want RGB to be stored across the columns and for each column to be a term, so we transpose the solution
//        mat3 solution = transpose(inverse(mATA) * vATb);
//        constantOut = vec4(pow(solution[0], vec3(1.0 / gamma)), 1.0);
//        diffuseOut = vec4(pow(solution[1], vec3(1.0 / gamma)), 1.0);
//        quadraticOut = vec4(pow(solution[2], vec3(1.0 / gamma)), 1.0);
//    }
//    else
//    {
//        discard;
//    }

    // just a simple linear regression
    vec4 diffuseColorSum = sumWeights * cosineWeightedSums - weightedSums.a * weightedSums;
    vec4 diffuseColor = diffuseColorSum / max(1.0, diffuseColorSum.a);

    vec4 constantColorSum = vec4(weightedSums.rgb - diffuseColor.rgb * weightedSums.a, sumWeights);
    vec4 constantColor = constantColorSum / max(1.0, constantColorSum.a);

    vec4 diffuseFallback = cosineWeightedSums / max(1.0, cosineWeightedSums.a); // Constraining constant term to 0

    diffuseOut = pow(
        max(vec4(0), mix(diffuseFallback, diffuseColor, step(0.0, vec4(constantColor.rgb, 1.0)))), // use fallback if constant color is < 0
        vec4(vec3(1.0 / gamma), 1.0));
    constantOut = pow(constantColor, vec4(vec3(1.0 / gamma), 1.0));
}
