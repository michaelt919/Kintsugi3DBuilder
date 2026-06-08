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

#ifdef TEXTURE_DIFFUSE
#undef TEXTURE_DIFFUSE
#endif
#define TEXTURE_DIFFUSE 0

#ifdef TEXTURE_NORMAL
#undef TEXTURE_NORMAL
#endif
#define TEXTURE_NORMAL 0

#ifdef TEXTURE_SPECULAR
#undef TEXTURE_SPECULAR
#endif
#define TEXTURE_SPECULAR 0

#ifdef TEXTURE_ROUGHNESS
#undef TEXTURE_ROUGHNESS
#endif
#define TEXTURE_ROUGHNESS 0

#ifdef TEXTURE_ALBEDO
#undef TEXTURE_ALBEDO
#endif
#define TEXTURE_ALBEDO 0

#ifdef TEXTURE_ORM
#undef TEXTURE_ORM
#endif
#define TEXTURE_ORM 0


#include <subject/standard.glsl>
