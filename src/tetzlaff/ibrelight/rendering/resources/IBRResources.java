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

package tetzlaff.ibrelight.rendering.resources;

import java.io.FileNotFoundException;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.StandardRenderingMode;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.util.ColorList;

public interface IBRResources<ContextType extends Context<ContextType>> extends Resource
{
    ContextType getContext();
    ViewSet getViewSet();
    ProgramBuilder<ContextType> getIBRShaderProgramBuilder(StandardRenderingMode renderingMode);
    ProgramBuilder<ContextType> getIBRShaderProgramBuilder();
    void setupShaderProgram(Program<ContextType> program);
    Drawable<ContextType> createDrawable(Program<ContextType> program);
    GraphicsStream<ColorList[]> stream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount);
    GraphicsStream<ColorList> stream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer);
    GraphicsStreamResource<ContextType> streamAsResource(
        ProgramBuilder<ContextType> programBuilder,
        FramebufferObjectBuilder<ContextType> framebufferBuilder) throws FileNotFoundException;
    GraphicsStream<ColorList[]> parallelStream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount, int maxRunningThreads);
    GraphicsStream<ColorList[]> parallelStream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount);
    GraphicsStream<ColorList> parallelStream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer);
    GraphicsStreamResource<ContextType> parallelStreamAsResource(
        ProgramBuilder<ContextType> programBuilder,
        FramebufferObjectBuilder<ContextType> framebufferBuilder,
        int maxRunningThreads) throws FileNotFoundException;
    GraphicsStreamResource<ContextType> parallelStreamAsResource(
        ProgramBuilder<ContextType> programBuilder,
        FramebufferObjectBuilder<ContextType> framebufferBuilder) throws FileNotFoundException;
}
