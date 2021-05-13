#version 330

/*
 *  Copyright (c) Michael Tetzlaff 2020
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
#include "../texturefit/diffusefit.glsl"
#line 17 0

layout(location = 0) out vec4 normalTS;

void main()
{
    DiffuseFit fit = fitDiffuse();

//    // for debugging
//    vec2 scaledTexCoord = ANALYTIC_UV_SCALE * fTexCoord;
//    normalTS = vec4(normalize((getNormal(scaledTexCoord - floor(scaledTexCoord)) + vec3(fTexCoord * 2 - 1, 0)) * vec3(ANALYTIC_BUMP_HEIGHT, ANALYTIC_BUMP_HEIGHT, 1.0)) * 0.5 + 0.5, 1.0);

     normalTS = vec4(fit.normalTS * 0.5 + vec3(0.5), 1.0);
}
