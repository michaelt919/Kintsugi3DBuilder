/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.rendering;

import java.util.function.Consumer;
import java.util.stream.IntStream;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.PrimitiveMode;
import tetzlaff.util.ColorList;

public class SequentialViewRenderStream<ContextType extends Context<ContextType>> extends GraphicsStreamBase<ColorList[]>
{
    private final int viewCount;
    private final Drawable<ContextType> drawable;
    private final Framebuffer<ContextType> framebuffer;
    private final int attachmentCount;

    SequentialViewRenderStream(int viewCount, Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount)
    {
        this.viewCount = viewCount;
        this.drawable = drawable;
        this.framebuffer = framebuffer;
        this.attachmentCount = attachmentCount;
    }

    @Override
    public GraphicsStream<ColorList[]> sequential()
    {
        return this;
    }

    @Override
    public GraphicsStream<ColorList[]> parallel()
    {
        return new ParallelViewRenderStream<>(viewCount, drawable, framebuffer, attachmentCount);
    }

    @Override
    public GraphicsStream<ColorList[]> parallel(int maxRunningThreads)
    {
        return new ParallelViewRenderStream<>(viewCount, drawable, framebuffer, attachmentCount, maxRunningThreads);
    }

    @Override
    public int count()
    {
        return viewCount;
    }

    @Override
    public void forEach(Consumer<? super ColorList[]> action)
    {
        for (int k = 0; k < viewCount; k++)
        {
            for (int i = 0; i < attachmentCount; i++)
            {
                // Clear framebuffer
                framebuffer.clearColorBuffer(i, 0.0f, 0.0f, 0.0f, 0.0f);
            }

            // Run shader program to fill framebuffer with per-pixel information.
            drawable.program().setUniform("viewIndex", k);
            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            // Copy framebuffer from GPU to main memory.
            ColorList[] framebufferData = IntStream.range(0, attachmentCount)
                .mapToObj(i -> new ColorList(framebuffer.readFloatingPointColorBufferRGBA(i)))
                .toArray(ColorList[]::new);

            action.accept(framebufferData);
        }
    }
}
