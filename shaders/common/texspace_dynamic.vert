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

#define GEOMETRY_MODE_PROJECT_3D_TO_2D 0
#define GEOMETRY_MODE_RECTANGLE 1

#ifndef GEOMETRY_MODE
#define GEOMETRY_MODE GEOMETRY_MODE_PROJECT_3D_TO_2D
#endif

#if (GEOMETRY_MODE == GEOMETRY_MODE_PROJECT_3D_TO_2D)

in vec3 position;
in vec2 texCoord;
in vec3 normal;
in vec4 tangent;

out vec3 fPosition;
out vec2 fTexCoord;
out vec3 fNormal;
out vec3 fTangent;
out vec3 fBitangent;

void main(void)
{
    gl_Position = vec4(2.0 * texCoord - vec2(1.0), 0.0, 1.0);
    fPosition = position;
    fTexCoord = texCoord;
    fNormal = normal;
    fTangent = tangent.xyz;
    fBitangent = tangent.w * normalize(cross(normal, tangent.xyz));
}

#elif (GEOMETRY_MODE == GEOMETRY_MODE_RECTANGLE)

in vec2 position;
out vec2 fTexCoord;

void main()
{
    gl_Position = vec4(position, 0.0, 1.0);
    fTexCoord = (position + vec2(1)) * 0.5;
}

#endif
