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

in vec3 fPosition;

#include "../colorappearance/colorappearance_dynamic.glsl"
#include "../colorappearance/reflectanceequations.glsl"
#include "../specularfit/evaluateBRDF.glsl"
#include "../common/constructTBN.glsl"
#include "../specularfit/lightingParameters.glsl"
#line 23 0

uniform vec3 diffuseColor = vec3(0.5);
uniform vec3 specularColor = vec3(0.04);
uniform float roughness = 0.25;
uniform float noiseScale = 0.0;

uniform vec3 reconstructionCameraPos;
uniform vec3 reconstructionLightPos;

layout(location = 0) out vec4 fragColor;

// https://gist.github.com/patriciogonzalezvivo/670c22f3966e662d2f83
float rand(vec2 n)
{
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}

void main()
{
    LightingParameters l = calculateLightingParameters(reconstructionCameraPos, reconstructionLightPos, normalize(fNormal));
    vec3 specular = distTimesPi(l.nDotH, vec3(roughness))
        * geom(roughness, l.nDotH, l.nDotV, l.nDotL, l.hDotV)
        * fresnel(specularColor.rgb, vec3(1), l.hDotV) / (4 * l.nDotV * PI);

    // Reflectance is implicitly multiplied by n dot l.
    // Gamma correction intentionally omitted for error calculation.
    fragColor = vec4(diffuseColor * l.nDotL / PI + specular + vec3((rand(floor(gl_FragCoord.xy)) - 0.5) * noiseScale), 1.0);
}
