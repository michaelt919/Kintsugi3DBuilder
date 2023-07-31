/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.javafx.util;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import kintsugi3d.gl.vecmath.DoubleVector4;
import kintsugi3d.util.AbstractImage;

public final class ImageFactory
{
    private ImageFactory()
    {
    }

    private static double clamp(double unclamped)
    {
        return Math.max(0, Math.min(1, unclamped));
    }


    private static class PixelReaderImpl extends PixelReaderBase
    {
        private final AbstractImage image;

        PixelReaderImpl(AbstractImage image)
        {
            this.image = image;
        }

        @Override
        public Color getColor(int x, int y)
        {
            DoubleVector4 colorVector = image.getRGBA(x, y);
            return new Color(clamp(colorVector.x), clamp(colorVector.y), clamp(colorVector.z), clamp(colorVector.w));
        }
    }

    public static Image createFromAbstractImage(AbstractImage image)
    {
        return new WritableImage(new PixelReaderImpl(image), image.getWidth(), image.getHeight());
    }
}
