/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.resources;

import java.io.FileNotFoundException;
import java.util.function.*;

import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.builders.framebuffer.FramebufferObjectBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.util.ColorList;

/**
 * A class that both functions as a graphics stream for hybrid GPU / CPU processing operations
 * and a resource that manages its own shader program and framebuffer object and can be auto-closed.
 * @param <ContextType>
 */
public class GraphicsStreamResource<ContextType extends Context<ContextType>> implements GraphicsStream<ColorList[]>, AutoCloseable
{
    private final ProgramObject<ContextType> program;
    private final FramebufferObject<ContextType> framebuffer;
    private final GraphicsStream<ColorList[]> base;

    GraphicsStreamResource(ProgramBuilder<ContextType> programBuilder,
        FramebufferObjectBuilder<ContextType> framebufferBuilder,
        BiFunction<Program<ContextType>, FramebufferObject<ContextType>, GraphicsStream<ColorList[]>> streamFactory) throws FileNotFoundException
    {
        program = programBuilder.createProgram();
        framebuffer = framebufferBuilder.createFramebufferObject();
        base = streamFactory.apply(program, framebuffer);
    }

    @Override
    public void close()
    {
        program.close();
        framebuffer.close();
    }

    /**
     * Retrieves the program managed by this graphics stream resource.
     * @return The shader program.
     */
    public Program<ContextType> getProgram()
    {
        return program;
    }

    /**
     * Retrieves the framebuffer managed by this graphics stream resource.
     * @return The framebuffer.
     */
    public Framebuffer<ContextType> getFramebuffer()
    {
        return framebuffer;
    }

    @Override
    public GraphicsStream<ColorList[]> sequential()
    {
        return base.sequential();
    }

    @Override
    public GraphicsStream<ColorList[]> parallel()
    {
        return base.parallel();
    }

    @Override
    public GraphicsStream<ColorList[]> parallel(int maxRunningThreads)
    {
        return base.parallel(maxRunningThreads);
    }

    @Override
    public int getCount()
    {
        return base.getCount();
    }

    @Override
    public void forEach(Consumer<? super ColorList[]> action)
    {
        base.forEach(action);
    }

    @Override
    public <R> GraphicsStream<R> map(Function<ColorList[], ? extends R> mapper)
    {
        return base.map(mapper);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super ColorList[]> accumulator)
    {
        return base.collect(supplier, accumulator);
    }

}
