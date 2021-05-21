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

uniform sampler2D specularTexture;
uniform sampler2D roughnessTexture;

#include "../colorappearance/linearize.glsl"

#line 11 0

in vec2 fTexCoord;
out vec4 fragColor;

void main() 
{
    vec4 specularColor = texture(specularTexture, fTexCoord);
    vec4 roughness = texture(roughnessTexture, fTexCoord);
    fragColor = vec4(pow(xyzToRGB(rgbToXYZ(pow(specularColor.rgb, vec3(gamma))) / (4 * roughness.xyz * roughness.xyz)), vec3(1.0 / gamma)),
        specularColor.a * roughness.a);
}
