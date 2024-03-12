#version 330

/*
 *  Copyright (c) Michael Tetzlaff 2024
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

layout(location = 0) out vec4 lightOut;

void main()
{
    vec3 position = getPosition();

    mat3 tangentToObject = constructTBNExact();
    vec3 triangleNormal = tangentToObject[2];

    // Normal optimization will likely not have taken place yet,
    // but we can import a normal map from Metashape.
    vec2 detailNormalXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
    vec3 detailNormalTS = vec3(detailNormalXY, sqrt(1 - dot(detailNormalXY, detailNormalXY)));
    vec3 detailNormal = tangentToObject * detailNormalTS;

    mat4 mATA = mat4(0);
    vec4 vATb = vec4(0);

    for (int k = 0; k < CAMERA_POSE_COUNT; k++)
    {
        vec4 imgColor = getLinearColor(k);
        vec3 viewUnnorm = getViewVector(k, position);
        float viewLength = length(viewUnnorm);
        vec3 view = viewLength / viewUnnorm;
        float triangleNDotV = max(0.0, dot(triangleNormal, view));

        if (imgColor.a > 0.0 && triangleNDotV > 0.0)
        {
            float nDotV = max(0.0, dot(detailNormal, view));

            if (nDotV > 0.0)
            {
                // "Light intensity" is defined in such a way that we need to multiply by pi to be properly normalized.
                vec3 incidentRadiance = PI * getLightIntensity(k) / dot(lightDisplacement, lightDisplacement);

                vec3 reflectanceTimesNDotL = imgColor.rgb / incidentRadiance;

                // Avoid overfitting to specular dominated samples
                float weight = sqrt(max(0, 1 - nDotV * nDotV));

                mat4 camPose = getCameraPose(k);
                vec3 normalCamSpace = (camPose * vec4(detailNormal, 0)).xyz;

                // Lo/Li = fD * (nx * (Lx+Vx-Px) + ny * (Ly+Vy-Py) + nz * (Lz+Vz-Pz)) / length(V-P)
                // Lo/Li = nx/length(v) * (fD*Lx) + ny/length(v) * (fD*Ly) + fD * (n dot v) // In camera space
                // Add constant term for translucency, ambient, etc.
                vec4 w = vec4(normalCamSpace.xy / viewLength, nDotV, 1);

                mATA += weight * triangleNDotV * outerProduct(w, w) * 3; // * 3 for R/G/B channels
                vATb += weight * triangleNDotV * w * dot(reflectanceTimesNDotL, vec3(1)); // sum R/G/B channels
            }
        }
    }

    if (determinant(mATA) == 0)
    {
        lightOut = vec4(0);
    }
    else
    {
        vec4 solution = inverse(mATA) * vATb;

        // * 0.5 + 0.5 is temp code while debugging
        lightOut = vec4(solution.xy / solution.z * 0.5 + 0.5, 0, 1);
    }
}
