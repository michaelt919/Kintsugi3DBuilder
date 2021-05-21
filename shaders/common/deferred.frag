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

uniform sampler2D normalMap;
uniform bool useNormalMap;

layout(location = 0) out vec4 position;
layout(location = 1) out vec3 normal;

void main() 
{
    position = vec4(fPosition, 1.0);

    vec3 geometricNormal = normalize(fNormal);

    if (useNormalMap)
    {
        vec3 tangent = normalize(fTangent - dot(geometricNormal, fTangent));
        vec3 bitangent = normalize(fBitangent
            - dot(geometricNormal, fBitangent) * geometricNormal
            - dot(tangent, fBitangent) * tangent);

        mat3 tangentToObject = mat3(tangent, bitangent, geometricNormal);

        vec2 normalDirXY = texture(normalMap, fTexCoord).xy * 2 - vec2(1.0);
        normal = tangentToObject * vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    }
    else
    {
        normal = geometricNormal;
    }
}
