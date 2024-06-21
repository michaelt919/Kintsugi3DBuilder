/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.util;

import kintsugi3d.gl.builders.ColorTextureBuilder;
import kintsugi3d.gl.core.ColorFormat;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture2D;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;

import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;

public class RadialTextureGenerator<ContextType extends Context<ContextType>>
{
    private final ContextType context;

    public RadialTextureGenerator(ContextType context)
    {
        this.context = context;
    }

    public ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> buildRadialTexture(
        int size, DoubleUnaryOperator radialFunction)
    {
        NativeVectorBuffer textureData = NativeVectorBufferFactory.getInstance()
            .createEmpty(NativeDataType.FLOAT, 1, size * size);

        int k = 0;
        for (int i = 0; i < size; i++)
        {
            double x = i * 2.0 / (size - 1) - 1.0;

            for (int j = 0; j < size; j++)
            {
                double y = j * 2.0 / (size - 1) - 1.0;

                double rSq = x*x + y*y;
                textureData.set(k, 0, radialFunction.applyAsDouble(rSq));

                k++;
            }
        }

        return context.getTextureFactory().build2DColorTextureFromBuffer(size, size, textureData)
            .setInternalFormat(ColorFormat.R8);
    }

    public ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> buildBloomTexture(int size)
    {
        return buildRadialTexture(64, rSq -> (Math.cos(Math.min(Math.sqrt(rSq), 1.0) * Math.PI) + 1.0) * 0.5);
    }

    public ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> buildCircleTexture(int size)
    {
        return buildRadialTexture(64, rSq -> rSq <= 1.0 ? 1.0 : 0.0);
    }
}
