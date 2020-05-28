#version 330

/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

layout(location = 0) out vec4 diffuseColor;
layout(location = 1) out vec4 normal;
layout(location = 2) out vec4 specularColor;
layout(location = 3) out vec4 roughness;

#include "../colorappearance/colorappearance.glsl"
#include "../colorappearance/imgspace.glsl"
#include "specularfit.glsl"

#line 17 0

void main()
{
    ParameterizedFit fit = fitSpecular();
    diffuseColor = vec4(pow(fit.diffuseColor.rgb, vec3(1 / gamma)), fit.diffuseColor.a);
    normal = vec4(fit.normal.xyz * 0.5 + vec3(0.5), fit.normal.w);
    specularColor = vec4(pow(fit.specularColor.rgb, vec3(1 / gamma)), fit.specularColor.a);
    roughness = vec4(sqrt(fit.roughness.xyz), fit.roughness.w);
}
