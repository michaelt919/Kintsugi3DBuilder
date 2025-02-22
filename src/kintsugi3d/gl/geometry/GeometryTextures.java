/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.geometry;

import kintsugi3d.gl.core.*;

public interface GeometryTextures<ContextType extends Context<ContextType>> extends Resource, ContextBound<ContextType>
{
    int getWidth();
    int getHeight();

    Texture2D<ContextType> getPositionTexture();
    Texture2D<ContextType> getNormalTexture();
    Texture2D<ContextType> getTangentTexture();

    void setupShaderProgram(Program<ContextType> program);

    /**
     *
     * @param x
     * @param y
     * @param viewportWidth
     * @param viewportHeight
     * @return A newly created resource containing a copy of the geometry textures within the specified viewport
     */
    GeometryTextures<ContextType> createViewportCopy(int x, int y, int viewportWidth, int viewportHeight);
}
