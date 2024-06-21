#version 330

/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

#include "specularFit.glsl"
#include <colorappearance/colorappearance_multi_as_single.glsl>
#line 18 0

uniform sampler2D specularEstimate;

layout(location = 0) out vec4 errorOut;

void main()
{
    vec2 sqrtRoughness_Mask = texture(roughnessMap, fTexCoord).ra;
    float filteredMask = sqrtRoughness_Mask[1];

    float roughness = sqrtRoughness_Mask[0] * sqrtRoughness_Mask[0];

    vec3 diffuseColor = sRGBToLinear(clamp(texture(diffuseMap, fTexCoord).rgb, 0, 1));
    vec3 specularColor = sRGBToLinear(texture(specularEstimate, fTexCoord).rgb);

    vec3 position = getPosition();

    mat3 tangentToObject = constructTBNExact();
    vec3 triangleNormal = tangentToObject[2];

    vec2 fittedNormalXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
    vec3 fittedNormalTS = vec3(fittedNormalXY, sqrt(1 - dot(fittedNormalXY, fittedNormalXY)));
    vec3 fittedNormal = tangentToObject * fittedNormalTS;

    vec4 imgColor = getLinearColor();
    vec3 view = normalize(getViewVector(position));
    float triangleNDotV = max(0.0, dot(triangleNormal, view));

    vec3 lightDisplacement = getLightVector(position);
    vec3 light = normalize(lightDisplacement);
    vec3 halfway = normalize(light + view);
    float nDotH = max(0.0, dot(fittedNormal, halfway));
    float nDotL = max(0.001, dot(fittedNormal, light));
    float nDotV = max(0.001, dot(fittedNormal, view));

    // "Light intensity" is defined in such a way that we need to multiply by pi to be properly normalized.
    vec3 incidentRadiance = PI * lightIntensity / dot(lightDisplacement, lightDisplacement);

    vec3 actualReflectanceTimesNDotL = imgColor.rgb / incidentRadiance;
    if (sRGB)
    {
        actualReflectanceTimesNDotL = linearToSRGB(actualReflectanceTimesNDotL);
    }

    float hDotV = max(0.0, dot(halfway, view));

    vec3 specular = 1 / PI * distTimesPi(nDotH, vec3(roughness))
        * geom(roughness, nDotH, nDotV, nDotL, hDotV)
        * fresnel(specularColor.rgb, vec3(1), hDotV) / (4 * nDotV);

    vec3 reflectanceEstimateTimesNDotL = diffuseColor * nDotL / PI + specular;
    if (sRGB)
    {
        reflectanceEstimateTimesNDotL = linearToSRGB(reflectanceEstimateTimesNDotL);
    }

    vec3 diff = actualReflectanceTimesNDotL - reflectanceEstimateTimesNDotL;
    float error = dot(diff, diff);

    errorOut = vec4(vec3(imgColor.a * error / 3), imgColor.a);
}
