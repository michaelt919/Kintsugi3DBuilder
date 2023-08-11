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

package kintsugi3d.builder.resources.ibr.stream;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Drawable;
import kintsugi3d.gl.core.Framebuffer;
import kintsugi3d.util.ColorList;
import kintsugi3d.util.ColorNativeBufferList;

public class ParallelViewRenderStream<ContextType extends Context<ContextType>> extends GraphicsStreamBase<ColorList[]>
{
    private static final Logger log = LoggerFactory.getLogger(ParallelViewRenderStream.class);
    private final int viewCount;
    private final Drawable<ContextType> drawable;
    private final Framebuffer<ContextType> framebuffer;
    private final int attachmentCount;
    private final int maxRunningThreads;

    private static final int DEFAULT_MAX_RUNNING_THREADS = 5;

    private final Object threadsRunningLock = new Object();
    private int threadsRunning = 0;

    private final Deque<ColorNativeBufferList[]> unusedColorBuffers;


    ParallelViewRenderStream(int viewCount, Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount,
        int maxRunningThreads)
    {
        this.viewCount = viewCount;
        this.drawable = drawable;
        this.framebuffer = framebuffer;
        this.attachmentCount = attachmentCount;
        this.maxRunningThreads = maxRunningThreads;
        unusedColorBuffers = IntStream.range(0, maxRunningThreads)
            .mapToObj(i -> IntStream.range(0, attachmentCount)
                .mapToObj(j -> new ColorNativeBufferList(framebuffer.getSize().width * framebuffer.getSize().height))
                .toArray(ColorNativeBufferList[]::new))
            .collect(Collectors.toCollection(ArrayDeque::new));
    }

    ParallelViewRenderStream(int viewCount, Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount)
    {
        this(viewCount, drawable, framebuffer, attachmentCount, DEFAULT_MAX_RUNNING_THREADS);
    }

    @Override
    public GraphicsStream<ColorList[]> sequential()
    {
        return new SequentialViewRenderStream<>(viewCount, drawable, framebuffer, attachmentCount);
    }

    @Override
    public GraphicsStream<ColorList[]> parallel()
    {
        return this;
    }

    @Override
    public GraphicsStream<ColorList[]> parallel(int maxRunningThreads)
    {
        if (maxRunningThreads == this.maxRunningThreads)
        {
            return this;
        }
        else
        {
            return new ParallelViewRenderStream<>(viewCount, drawable, framebuffer, attachmentCount, maxRunningThreads);
        }
    }

    @Override
    public int getCount()
    {
        return viewCount;
    }

    @Override
    public void forEach(Consumer<? super ColorList[]> action)
    {
        for (int k = 0; k < viewCount; k++)
        {
            ColorNativeBufferList[] colorBuffers;

            synchronized (threadsRunningLock)
            {
                // Make sure that we don't have too many threads running.
                // Wait until a thread finishes if we're at the max.
                while (threadsRunning >= maxRunningThreads)
                {
                    try
                    {
                        threadsRunningLock.wait(30000); // Double check every 30 seconds if notifyAll() was not called
                    }
                    catch (InterruptedException e)
                    {
                        log.error("Error: Operation interrupted:", e);
                    }
                }

                // Grab a color buffer while we still have the lock.
                colorBuffers = unusedColorBuffers.pop();
            }

            for (int i = 0; i < attachmentCount; i++)
            {
                // Clear framebuffer
                framebuffer.clearColorBuffer(i, 0.0f, 0.0f, 0.0f, 0.0f);
            }

            // Run shader program to fill framebuffer with per-pixel information.
            drawable.program().setUniform("viewIndex", k);
            drawable.draw(framebuffer);


            // Copy framebuffer from GPU to main memory.
            IntStream.range(0, attachmentCount).forEach(i -> framebuffer.getTextureReaderForColorAttachment(i).readFloatingPointRGBA(colorBuffers[i].buffer));

            synchronized (threadsRunningLock)
            {
                threadsRunning++;
            }

            Thread actionThread = new Thread(() ->
            {
                try
                {
                    action.accept(colorBuffers);
                }
                finally
                {
                    synchronized (threadsRunningLock)
                    {
                        // Return the buffer to the unused pool while we have the lock.
                        unusedColorBuffers.push(colorBuffers);

                        threadsRunning--;
                        threadsRunningLock.notifyAll();
                    }
                }
            });

            actionThread.start();
        }

        // Wait for all the threads to finish.
        synchronized (threadsRunningLock)
        {
            while (threadsRunning > 0)
            {
                try
                {
                    threadsRunningLock.wait(30000); // Double check every 30 seconds if notifyAll() was not called
                }
                catch (InterruptedException e)
                {
                    log.error("Error: Operation interrupted:", e);
                }
            }
        }
    }
}
