/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.geometry;

import tetzlaff.gl.core.ColorFormat;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Texture2D;

public class GeometryTexturesNonRendered<ContextType extends Context<ContextType>> extends GeometryTexturesBase<ContextType>
{
    private final Texture2D<ContextType> positionTexture;
    private final Texture2D<ContextType> normalTexture;
    private final Texture2D<ContextType> tangentTexture;

    public GeometryTexturesNonRendered(ContextType context, int width, int height)
    {
        super(context);
        positionTexture = context.getTextureFactory()
            .build2DColorTexture(width, height)
            .setInternalFormat(ColorFormat.RGB32F)
            .setLinearFilteringEnabled(true)
            .setMipmapsEnabled(true)
            .createTexture();

        normalTexture = context.getTextureFactory()
            .build2DColorTexture(width, height)
            .setInternalFormat(ColorFormat.RGB32F)
            .setLinearFilteringEnabled(true)
            .setMipmapsEnabled(true)
            .createTexture();

        tangentTexture = context.getTextureFactory()
            .build2DColorTexture(width, height)
            .setInternalFormat(ColorFormat.RGB32F)
            .setLinearFilteringEnabled(true)
            .setMipmapsEnabled(true)
            .createTexture();
    }

    @Override
    public Texture2D<ContextType> getPositionTexture()
    {
        return positionTexture;
    }

    @Override
    public Texture2D<ContextType> getNormalTexture()
    {
        return normalTexture;
    }

    @Override
    public Texture2D<ContextType> getTangentTexture()
    {
        return tangentTexture;
    }

    @Override
    public void close()
    {
        if (this.positionTexture != null)
        {
            this.positionTexture.close();
        }

        if (this.normalTexture != null)
        {
            this.normalTexture.close();
        }

        if (this.tangentTexture != null)
        {
            this.tangentTexture.close();
        }
    }

}
