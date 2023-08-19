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

// Disable all textures

#ifdef DIFFUSE_TEXTURE_ENABLED
#undef DIFFUSE_TEXTURE_ENABLED
#define DIFFUSE_TEXTURE_ENABLED 0
#endif

#ifdef NORMAL_TEXTURE_ENABLED
#undef NORMAL_TEXTURE_ENABLED
#define NORMAL_TEXTURE_ENABLED 0
#endif

#ifdef SPECULAR_TEXTURE_ENABLED
#undef SPECULAR_TEXTURE_ENABLED
#define SPECULAR_TEXTURE_ENABLED 0
#endif

#ifdef ROUGHNESS_TEXTURE_ENABLED
#undef ROUGHNESS_TEXTURE_ENABLED
#define ROUGHNESS_TEXTURE_ENABLED 0
#endif

#include <subject/ibr.glsl>
