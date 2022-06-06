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

package tetzlaff.gl.opengl;

import java.util.function.Function;

import tetzlaff.gl.core.DoubleFramebuffer;
import tetzlaff.gl.glfw.ContextFactory;

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
        context.setDefaultFramebuffer(createDefaultFramebuffer.apply(context));
        return context;
    }
}
