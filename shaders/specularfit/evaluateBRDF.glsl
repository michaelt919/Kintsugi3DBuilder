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

#ifndef EVALUATE_BRDF_GLSL
#define EVALUATE_BRDF_GLSL

#line 17 4001

uniform sampler2DArray weightMaps;
uniform sampler1DArray basisFunctions;

#ifndef PI
#define PI 3.1415926535897932384626433832795
#endif

#ifndef BASIS_COUNT
#define BASIS_COUNT 8
#endif

layout(std140) uniform DiffuseColors
{
    vec4 diffuseColors[BASIS_COUNT];
};

vec3 getMFDEstimateRaw(float w)
{
    vec3 estimate = vec3(0);

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        estimate += texture(weightMaps, vec3(fTexCoord, b))[0] * texture(basisFunctions, vec2(w, b)).rgb;
    }

    return estimate;
}

float getMFDLookupCoord(float nDotH)
{
    return sqrt(max(0.0, acos(nDotH) * 3.0 / PI));
}

vec3 getMFDEstimate(float nDotH)
{
    float w = getMFDLookupCoord(nDotH);
    return getMFDEstimateRaw(w);
}

vec3 getDiffuseEstimate()
{
    vec3 estimate = vec3(0);

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        estimate += texture(weightMaps, vec3(fTexCoord, b))[0] * diffuseColors[b].rgb;
    }

    return estimate;
}

vec3 getBRDFEstimate(float nDotH, float geomFactor)
{
    vec3 estimate = vec3(0);
    float w = getMFDLookupCoord(nDotH);

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        estimate += texture(weightMaps, vec3(fTexCoord, b))[0] * (diffuseColors[b].rgb / PI + texture(basisFunctions, vec2(w, b)).rgb * geomFactor);
    }

    return estimate;
}

#ifndef BASIS_RESOLUTION
#define BASIS_RESOLUTION 90
#endif

#endif // EVALUATE_BRDF_GLSL