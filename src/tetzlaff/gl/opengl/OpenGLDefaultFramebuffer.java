/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.opengl;

import tetzlaff.gl.core.DoubleFramebuffer;
import tetzlaff.gl.core.FramebufferSize;

import static org.lwjgl.opengl.GL11.*;

class OpenGLDefaultFramebuffer extends OpenGLFramebuffer implements DoubleFramebuffer<OpenGLContext>
{
    private final ContentsImpl contentsImpl = new ContentsImpl();

    OpenGLDefaultFramebuffer(OpenGLContext context)
    {
        super(context);
    }

    @Override
    public FramebufferSize getSize()
    {
        return this.context.getDefaultFramebufferSize();
    }

    @Override
    public void swapBuffers()
    {
        this.context.swapDefaultFramebuffer();
    }

    @Override
    public ContentsImpl getContentsForRead()
    {
        return contentsImpl;
    }

    @Override
    public ContentsImpl getContentsForWrite()
    {
        return contentsImpl;
    }

    private class ContentsImpl extends ContentsBase
    {
        @Override
        int getId()
        {
            return 0;
        }

        @Override
        void selectColorSourceForRead(int index)
        {
            if (index == 0)
            {
                glReadBuffer(GL_BACK);
                OpenGLContext.errorCheck();
            }
            else
            {
                throw new IllegalArgumentException("The default framebuffer does not have multiple color attachments.");
            }
        }
    }
}
