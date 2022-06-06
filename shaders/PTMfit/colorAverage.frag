#version 330

/*
 *  Copyright (c) Zhangchi (Josh) Lyu, Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

#include "PTMfit.glsl"
#line 17 0

out vec4 averageColor;
out vec4 unlitColor;

void main()
{
    vec3 normal = normalize(fNormal);

    vec4 sum = vec4(0);

    mat2 mATA = mat2(0);
    mat3x2 mATB = mat3x2(0);

    for (int i = 0; i < VIEW_COUNT; i++)
    {
        vec3 lightDisplacement = getLightVector(i);
        vec3 lightDir = normalize(lightDisplacement);

        // physical radiance = PI * numeric radiance
        vec3 incidentRadiance = PI * getLightIntensity(i) / dot(lightDisplacement, lightDisplacement);

        vec4 color = getLinearColor(i) / vec4(incidentRadiance, 1);
        float nDotL = max(0, dot(normal, lightDir));

        // Technically a linear regression, not a simple average.
        sum += color.a * nDotL * vec4(color.rgb, nDotL);

        // Linear regression with intercept / constant term
        mATA += color.a * outerProduct(vec2(nDotL, 1), vec2(nDotL, 1));
        mATB += color.a * outerProduct(vec2(nDotL, 1), color.rgb);
    }

    mat3x2 result = inverse(mATA) * mATB;

    float maxMagnitude = abs(result[0][0]);
    maxMagnitude = max(maxMagnitude, abs(result[0][1]));
    maxMagnitude = max(maxMagnitude, abs(result[1][0]));
    maxMagnitude = max(maxMagnitude, abs(result[1][1]));
    maxMagnitude = max(maxMagnitude, abs(result[2][0]));
    maxMagnitude = max(maxMagnitude, abs(result[2][1]));

    float alpha = clamp(min(determinant(mATA), 10 * (1 - maxMagnitude * PI)), 0, 1);

    averageColor = mix(sum / max(1.0, sum.a), vec4(transpose(result)[0], 1), alpha);
    unlitColor = vec4(transpose(result)[1] * alpha, 1);
}
