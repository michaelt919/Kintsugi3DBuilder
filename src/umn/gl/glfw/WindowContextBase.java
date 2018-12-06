/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.glfw;

import java.nio.IntBuffer;

import org.lwjgl.*;
import org.lwjgl.opengl.*;
import umn.gl.core.Context;
import umn.gl.core.FramebufferSize;

import static org.lwjgl.glfw.GLFW.*;

public abstract class WindowContextBase<ContextType extends WindowContextBase<ContextType>> implements Context<ContextType>
{
    private final long handle;

    protected WindowContextBase(long handle)
    {
        this.handle = handle;
    }

    @Override
    public void makeContextCurrent()
    {
        glfwMakeContextCurrent(handle);
        GL.createCapabilities(false);
    }

    public void swapDefaultFramebuffer()
    {
        glfwSwapBuffers(handle);
    }

    public FramebufferSize getDefaultFramebufferSize()
    {
        IntBuffer widthBuffer = BufferUtils.createByteBuffer(4).asIntBuffer();
        IntBuffer heightBuffer = BufferUtils.createByteBuffer(4).asIntBuffer();
        glfwGetFramebufferSize(handle, widthBuffer, heightBuffer);
        int width = widthBuffer.get(0);
        int height = heightBuffer.get(0);
        return new FramebufferSize(width, height);
    }

    @Override
    public void close()
    {
        glfwDestroyWindow(handle);
    }
}
