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

// Disable all textures except normal map and occlusion

#ifdef DIFFUSE_TEXTURE_ENABLED
#undef DIFFUSE_TEXTURE_ENABLED
#endif
#define DIFFUSE_TEXTURE_ENABLED 0

#ifdef SPECULAR_TEXTURE_ENABLED
#undef SPECULAR_TEXTURE_ENABLED
#endif
#define SPECULAR_TEXTURE_ENABLED 0

#ifdef ROUGHNESS_TEXTURE_ENABLED
#undef ROUGHNESS_TEXTURE_ENABLED
#endif
#define ROUGHNESS_TEXTURE_ENABLED 0

#ifdef ALBEDO_TEXTURE_ENABLED
#undef ALBEDO_TEXTURE_ENABLED
#endif
#define ALBEDO_TEXTURE_ENABLED 0

#ifdef ORM_TEXTURE_ENABLED
#undef ORM_TEXTURE_ENABLED
#endif
#define ORM_TEXTURE_ENABLED 0

#include <subject/standard.glsl>
