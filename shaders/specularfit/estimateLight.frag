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

struct IndexedLuminance
{
    int index;
    float luminance;
};

void main()
{
    vec3 position = getPosition();

    mat3 tangentToObject = constructTBNExact();
//    vec3 triangleNormal = tangentToObject[2];

//    // Normal optimization will likely not have taken place yet,
//    // but we can import a normal map from Metashape.
    vec2 detailNormalXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
    vec3 detailNormalTS = vec3(detailNormalXY, sqrt(1 - dot(detailNormalXY, detailNormalXY)));
    vec3 detailNormal = tangentToObject * detailNormalTS;

//    mat4 mATA = mat4(0);
//    vec4 vATb = vec4(0);

    IndexedLuminance brightestView = IndexedLuminance(-1, 0.0);
    IndexedLuminance secondBrightestView = IndexedLuminance(-1, 0.0);

    for (int k = 0; k < CAMERA_POSE_COUNT; k++)
    {
        vec4 imgColor = getLinearColor(k);
        vec3 viewUnnorm = getViewVector(k, position);
        float viewLength = length(viewUnnorm);
        vec3 view = viewUnnorm / viewLength;
        //float triangleNDotV = max(0.0, dot(triangleNormal, view));

        if (imgColor.a > 0.0/* && triangleNDotV > 0.0*/)
        {
            //float nDotV = max(0.0, dot(detailNormal, view));

            //if (nDotV > 0.0)
            //{
                // "Light intensity" is defined in such a way that we need to multiply by pi to be properly normalized.
                vec3 incidentRadiance = PI * getLightIntensity(k) / dot(viewLength, viewLength);

                mat4 camPose = getCameraPose(k);
//                vec3 normalCamSpace = normalize((camPose * vec4(detailNormal, 0)).xyz);
                vec3 positionCamSpace = (camPose * vec4(position, 1)).xyz;
                vec3 viewCamSpace = normalize(-positionCamSpace);

                vec3 reflectanceTimesNDotL =
                    //vec3(1.0) * dot(normalCamSpace.xyz, normalize(vec3(0, 1.0, 0) - (camPose * vec4(position, 1)).xyz));
                    imgColor.rgb / incidentRadiance;
                float luminance = getLuminance(reflectanceTimesNDotL);

//                vec3 debugLight = normalize(vec3(0, 1, 0) - positionCamSpace);
//                float luminance = max(0.0, dot(normalize(debugLight + viewCamSpace), normalCamSpace));
//
//                vec3 debugReflect = normalize(reflect(-viewCamSpace, normalCamSpace));
//                float luminance = max(0.0, dot(debugLight, debugReflect));

//                // Avoid overfitting to specular dominated samples or shadows
//                float weight = //1.0;
//                    viewCamSpace.z * clamp(luminance, 0, 1) * clamp(1.0 - luminance, 0, 1);
//
//                // Lo/Li = fD * (nx * (Lx+Vx-Px) + ny * (Ly+Vy-Py) + nz * (Lz+Vz-Pz)) / length(V-P)
//                // Lo/Li = nx/length(v) * (fD*Lx) + ny/length(v) * (fD*Ly) + fD * (n dot v) // In camera space
//                vec4 w = vec4(normalCamSpace.xy / viewLength, nDotV, 1.0);
//
//                mATA += weight * triangleNDotV * outerProduct(w, w) * 3; // * 3 for R/G/B channels
//                vATb += weight * triangleNDotV * w * dot(reflectanceTimesNDotL, vec3(1)); // sum R/G/B channels

                if (luminance > brightestView.luminance)
                {
                    secondBrightestView = brightestView;
                    brightestView = IndexedLuminance(k, luminance);
                }
                else if (luminance > secondBrightestView.luminance)
                {
                    secondBrightestView = IndexedLuminance(k, luminance);
                }
            //}
        }
    }

    if (brightestView.index >= 0)
    {
        vec3 lightDirTS = detailNormalTS * vec3(-1, -1, 1);
        vec3 lightDirObject = tangentToObject * lightDirTS;

        mat4 camPose = getCameraPose(brightestView.index);
        vec3 peakCamSpace = normalize((camPose * vec4(detailNormal, 0)).xyz);
        vec3 lightDirCamSpace = normalize((camPose * vec4(lightDirObject, 0)).xyz);

        // Essentially, we want to rotate around the position being rendered into a hypothetical camera space
        // where the camera was located at the "peak" position predicted by the biased normal map.
        // Orient the hypothetical camera to be facing directly towards the pixel being rendered (at center of viewing frustum)
        // thus, the "peak" direction is the +z axis.
        vec3 peakRight = normalize(cross(vec3(0, 1, 0), peakCamSpace)); // or normalize(vec3(peakCamSpace.z, 0, -peakCamSpace.x));
        vec3 peakUp = cross(peakCamSpace, peakRight);
        mat3 peakToRealCamSpaceRotation = mat3(peakRight, peakUp, peakCamSpace);
        mat3 realToPeakCamSpaceRotation = transpose(peakToRealCamSpaceRotation);

        vec3 positionCamSpace = (camPose * vec4(position, 1)).xyz;

//        vec3 fromViewCamSpace = normalize(positionCamSpace);
//        vec3 lightDirCamSpace = reflect(fromViewCamSpace, normalCamSpace);

        vec3 lightDirPeakSpace = realToPeakCamSpaceRotation * lightDirCamSpace;

//        // Scale to match the z-coordinate of fromView (toLightCamSpace.z = -positionCamSpace.z)
//        vec3 toLightCamSpace = lightDirCamSpace * -positionCamSpace.z / lightDirCamSpace.z;

//        // z-coordinate should be 0
//        vec3 lightOffset = positionCamSpace + toLightCamSpace;
        vec3 lightOffset = vec3(lightDirPeakSpace.xy * length(positionCamSpace) / lightDirPeakSpace.z, 0);
        float alpha = brightestView.luminance - secondBrightestView.luminance;
        lightOut = vec4(lightOffset, alpha);

//        lightOut.z = sqrt(1 - 0.0625 * dot(lightOut.xy, lightOut.xy)); // make results look like a normal map for debugging
//        lightOut.xy = lightOut.xy * 0.5 + 0.5; // * 0.5 + 0.5 is temp code while debugging
//        lightOut.w = clamp(lightOut.w, 0, 1);
    }

//    if (determinant(mATA) < 0.000000001)
//    {
//        lightOut = vec4(0);
//    }
//    else
//    {
//        vec4 solution = inverse(mATA) * vATb;
//
//        lightOut = vec4(solution.xy / solution.z, 0, clamp(solution.z / max(0, solution.w), 0, 1));
//
//        lightOut.z = sqrt(1 - 0.0625 * dot(lightOut.xy, lightOut.xy)); // make results look like a normal map for debugging
//        lightOut.xy = lightOut.xy * 0.125 + 0.5; // * 0.5 + 0.5 is temp code while debugging
//    }
}
