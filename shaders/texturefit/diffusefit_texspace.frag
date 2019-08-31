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
layout(location = 1) out vec4 normalMap;
layout(location = 2) out vec4 ambient;
layout(location = 3) out vec4 normalMapTS;

#include "../colorappearance/colorappearance.glsl"
#include "../colorappearance/texspace.glsl"
#include "../texturefit/diffusefit.glsl"

#line 31 0

void main()
{
    DiffuseFit fit = fitDiffuse();
    diffuseColor = vec4(pow(fit.color, vec3(1 / gamma)), 1.0);
    normalMap = vec4(fit.normal * 0.5 + vec3(0.5), 1.0);
    ambient = vec4(pow(fit.ambient, vec3(1 / gamma)), 1.0);
    normalMapTS = vec4(fit.normalTS * 0.5 + vec3(0.5), 1.0);
}
