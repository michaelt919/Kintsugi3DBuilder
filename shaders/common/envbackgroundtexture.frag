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

#include <colorappearance/linearize.glsl>
#line 17 0

in vec2 fTexCoord;
layout(location = 0) out vec4 fragColor;
layout(location = 1) out int fragObjectID;

uniform int objectID;
uniform samplerCube env;
uniform mat4 model_view;
uniform mat4 projection;
uniform mat4 envMapMatrix;
uniform vec3 envMapIntensity;
uniform bool sRGBEncoded;

#ifndef PI
#define PI 3.1415926535897932384626433832795
#endif

void main()
{
    vec4 unprojected = inverse(projection) * vec4(fTexCoord * 2 - vec2(1), 0, 1);

    vec3 viewDir =
        normalize((envMapMatrix * inverse(model_view) * vec4(unprojected.xyz / unprojected.w, 0.0)).xyz);

    if (sRGBEncoded)
    {
        fragColor = vec4(envMapIntensity * texture(env, viewDir).rgb, 1.0);

        // Use this version for a blurred background
        //fragColor = vec4(envMapIntensity * textureLod(env, viewDir, 3).rgb, 1.0);
    }
    else
    {
        fragColor = vec4(envMapIntensity * linearToSRGB(texture(env, viewDir).rgb), 1.0);

        // Use this version for a blurred background
        //fragColor = vec4(envMapIntensity * linearToSRGB(textureLod(env, viewDir, 3).rgb), 1.0);
    }


    fragObjectID = objectID;
}
