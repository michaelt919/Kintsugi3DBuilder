/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

#ifndef SOURCE_PIXEL_WEIGHTS_GLSL
#define SOURCE_PIXEL_WEIGHTS_GLSL

#line 17 1030

#ifndef EDGE_PROXIMITY_WEIGHT_ENABLED
#define EDGE_PROXIMITY_WEIGHT_ENABLED 0
#endif

#ifndef EDGE_PROXIMITY_MARGIN
#define EDGE_PROXIMITY_MARGIN 0.1
#endif

float getSourcePixelWeight(vec2 imageSpaceCoord)
{
#if EDGE_PROXIMITY_WEIGHT_ENABLED
    vec2 weights = clamp((0.5 - abs(imageSpaceCoord - 0.5)) / EDGE_PROXIMITY_MARGIN, 0.0, 1.0);
    return min(weights.x, weights.y);
#else
    return 1.0;
#endif
}

#endif // SOURCE_PIXEL_WEIGHTS_GLSL