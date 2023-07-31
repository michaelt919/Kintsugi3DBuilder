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

#line 14 1004

#ifndef COLOR_APPEARANCE_MODE_GLSL
#define COLOR_APPEARANCE_MODE_GLSL

#define COLOR_APPEARANCE_MODE_ANALYTIC 0
#define COLOR_APPEARANCE_MODE_IMAGE_SPACE 1
#define COLOR_APPEARANCE_MODE_TEXTURE_SPACE 2
#define COLOR_APPEARANCE_MODE_TEXTURE_SPACE_CROP 3

#ifndef COLOR_APPEARANCE_MODE
#define COLOR_APPEARANCE_MODE COLOR_APPEARANCE_MODE_IMAGE_SPACE
#endif

#endif // COLOR_APPEARANCE_MODE_GLSL