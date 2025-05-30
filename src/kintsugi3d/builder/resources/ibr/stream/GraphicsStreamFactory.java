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

package kintsugi3d.builder.resources.ibr.stream;

import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.builders.framebuffer.FramebufferObjectBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Drawable;
import kintsugi3d.gl.core.Framebuffer;
import kintsugi3d.util.ColorList;

import java.io.FileNotFoundException;
import java.io.IOException;

public class GraphicsStreamFactory<ContextType extends Context<ContextType>>
{
    private final ReadonlyIBRResources<ContextType> resources;

    public GraphicsStreamFactory(ReadonlyIBRResources<ContextType> resources)
    {
        this.resources = resources;
    }

    /**
     * Returns a sequential stream with the views in this IBR instance as its source.
     * A shader program and a framebuffer (which may contain multiple attachments) must be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param drawable A drawable (typically obtained using IBRResources.createDrawable)
     *                 that contains the shader program to be invoked on each view in this instance.
     * @param framebuffer The GPU framebuffer which will store the result of invoking the specified drawable.
     * @param attachmentCount The number of attachments that the framebuffer contains.
     * @return a sequential Stream over the views in this instance.
     */
    public GraphicsStream<ColorList[]> stream(
            Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount)
    {
        return new SequentialViewRenderStream<>(this.resources.getViewSet().getCameraPoseCount(), drawable, framebuffer, attachmentCount);
    }

    /**
     * Returns a sequential stream with the views in this IBR instance as its source.
     * A shader program and a framebuffer (with a single attachment for this overload) must be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param drawable A drawable (typically obtained using IBRResources.createDrawable)
     *                 that contains the shader program to be invoked on each view in this instance.
     * @param framebuffer The GPU framebuffer which will store the result of invoking the specified drawable.
     * @return a sequential Stream over the views in this instance.
     */
    public GraphicsStream<ColorList> stream(
            Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        return new SequentialViewRenderStream<>(this.resources.getViewSet().getCameraPoseCount(), drawable, framebuffer, 1)
            .map(singletonList -> singletonList[0]);
    }

    /**
     * Returns a sequential stream with the views in this IBR instance as its source.
     * Unlike stream(), this function manages the allocation of the shader program and the framebuffer object
     * and returns an AutoCloseable so that they may be automatically deallocated using a try-with-resources block.
     * Builders for the shader program and framebuffer object must still be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param programBuilder A builder for the shader program to be invoked on each view in this instance.
     * @param framebufferBuilder A builder for the GPU framebuffer which will store the result of invoking the
     *                           specified drawable.
     * @return a sequential Stream over the views in this instance,
     * with a dual function as an AutoCloseable that manages the associated GPU resources.
     * @throws FileNotFoundException if the shader program files cannot be found.
     */
    public GraphicsStreamResource<ContextType> streamAsResource(
            ProgramBuilder<ContextType> programBuilder,
            FramebufferObjectBuilder<ContextType> framebufferBuilder) throws IOException
    {
        return new GraphicsStreamResource<>(programBuilder, framebufferBuilder,
            (program, framebuffer) -> new SequentialViewRenderStream<>(
                this.resources.getViewSet().getCameraPoseCount(), this.resources.createDrawable(program), framebuffer,
                framebuffer.getColorAttachmentCount()));
    }

    /**
     * Returns a parallel stream with the views in this IBR instance as its source.
     * A shader program and a framebuffer (which may contain multiple attachments) must be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param drawable A drawable (typically obtained using IBRResources.createDrawable)
     *                 that contains the shader program to be invoked on each view in this instance.
     * @param framebuffer The GPU framebuffer which will store the result of invoking the specified drawable.
     * @param attachmentCount The number of attachments that the framebuffer contains.
     * @param maxRunningThreads The maximum number of threads allowed to be running at once.  The fact that one thread
     *                          will be dedicated to GPU rendering should be considered when specifying this parameter.
     * @return a parallel Stream over the views in this instance.
     */
    public GraphicsStream<ColorList[]> parallelStream(
            Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount, int maxRunningThreads)
    {
        return new ParallelViewRenderStream<>(this.resources.getViewSet().getCameraPoseCount(), drawable, framebuffer,
                attachmentCount, maxRunningThreads);
    }

    /**
     * Returns a parallel stream with the views in this IBR instance as its source,
     * with a default limit for the number of threads running at once.
     * A shader program and a framebuffer (which may contain multiple attachments) must be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param drawable A drawable (typically obtained using IBRResources.createDrawable)
     *                 that contains the shader program to be invoked on each view in this instance.
     * @param framebuffer The GPU framebuffer which will store the result of invoking the specified drawable.
     * @param attachmentCount The number of attachments that the framebuffer contains.
     * @return a parallel Stream over the views in this instance.
     */
    public GraphicsStream<ColorList[]> parallelStream(
            Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount)
    {
        return new ParallelViewRenderStream<>(this.resources.getViewSet().getCameraPoseCount(), drawable, framebuffer, attachmentCount);
    }

    /**
     * Returns a parallel stream with the views in this IBR instance as its source.
     * A shader program and a framebuffer (with a single attachment for this overload) must be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param drawable A drawable (typically obtained using IBRResources.createDrawable)
     *                 that contains the shader program to be invoked on each view in this instance.
     * @param framebuffer The GPU framebuffer which will store the result of invoking the specified drawable.
     * @return a parallel Stream over the views in this instance.
     */
    public GraphicsStream<ColorList> parallelStream(
            Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        return new ParallelViewRenderStream<>(this.resources.getViewSet().getCameraPoseCount(), drawable, framebuffer, 1)
            .map(singletonList -> singletonList[0]);
    }

    /**
     * Returns a parallel stream with the views in this IBR instance as its source.
     * Unlike parallelStream(), this function manages the allocation of the shader program and the framebuffer object
     * and returns an AutoCloseable so that they may be automatically deallocated using a try-with-resources block.
     * Builders for the shader program and framebuffer object must still be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param programBuilder A builder for the shader program to be invoked on each view in this instance.
     * @param framebufferBuilder A builder for the GPU framebuffer which will store the result of invoking the
     *                           specified drawable.
     * @param maxRunningThreads The maximum number of threads allowed to be running at once.  The fact that one thread
     *                          will be dedicated to GPU rendering should be considered when specifying this parameter.
     * @return a parallel Stream over the views in this instance,
     * with a dual function as an AutoCloseable that manages the associated GPU resources.
     * @throws FileNotFoundException if the shader program files cannot be found.
     */
    public GraphicsStreamResource<ContextType> parallelStreamAsResource(
            ProgramBuilder<ContextType> programBuilder,
            FramebufferObjectBuilder<ContextType> framebufferBuilder,
            int maxRunningThreads) throws IOException
    {
        return new GraphicsStreamResource<>(programBuilder, framebufferBuilder,
            (program, framebuffer) -> new ParallelViewRenderStream<>(
                this.resources.getViewSet().getCameraPoseCount(), this.resources.createDrawable(program), framebuffer,
                framebuffer.getColorAttachmentCount(), maxRunningThreads));
    }

    /**
     * Returns a parallel stream with the views in this IBR instance as its source,
     * with a default limit for the number of threads running at once.
     * Unlike parallelStream(), this function manages the allocation of the shader program and the framebuffer object
     * and returns an AutoCloseable so that they may be automatically deallocated using a try-with-resources block.
     * Builders for the shader program and framebuffer object must still be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param programBuilder A builder for the shader program to be invoked on each view in this instance.
     * @param framebufferBuilder A builder for the GPU framebuffer which will store the result of invoking the
     *                           specified drawable.
     * @return a parallel Stream over the views in this instance,
     * with a dual function as an AutoCloseable that manages the associated GPU resources.
     * @throws FileNotFoundException if the shader program files cannot be found.
     */
    public GraphicsStreamResource<ContextType> parallelStreamAsResource(
            ProgramBuilder<ContextType> programBuilder,
            FramebufferObjectBuilder<ContextType> framebufferBuilder) throws IOException
    {
        return new GraphicsStreamResource<>(programBuilder, framebufferBuilder,
            (program, framebuffer) -> new ParallelViewRenderStream<>(
                this.resources.getViewSet().getCameraPoseCount(), this.resources.createDrawable(program), framebuffer,
                framebuffer.getColorAttachmentCount()));
    }
}
