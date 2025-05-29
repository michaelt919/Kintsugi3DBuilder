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

package kintsugi3d.gl.opengl;

import java.util.function.Function;

import kintsugi3d.gl.core.DoubleFramebuffer;
import kintsugi3d.gl.glfw.ContextFactory;

public class OpenGLContextFactory implements ContextFactory<OpenGLContext>
{
    private static final OpenGLContextFactory INSTANCE = new OpenGLContextFactory();

    public static OpenGLContextFactory getInstance()
    {
        return INSTANCE;
    }

    @Override
    public OpenGLContext createContext(long glfwHandle)
    {
        OpenGLContext context = new OpenGLContext(glfwHandle);
        context.setDefaultFramebuffer(new OpenGLDefaultFramebuffer(context));
        return context;
    }

    @Override
    public OpenGLContext createContext(long glfwHandle, Function<OpenGLContext, DoubleFramebuffer<OpenGLContext>> createDefaultFramebuffer)
    {
        OpenGLContext context = new OpenGLContext(glfwHandle);
        context.makeContextCurrent();
        context.setDefaultFramebuffer(createDefaultFramebuffer.apply(context));
        return context;
    }
}
