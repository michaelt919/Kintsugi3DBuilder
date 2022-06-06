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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import javax.imageio.ImageIO;

import org.lwjgl.*;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.FramebufferSize;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;

abstract class OpenGLFramebuffer implements Framebuffer<OpenGLContext>
{
    protected final OpenGLContext context;

    OpenGLFramebuffer(OpenGLContext context)
    {
        this.context = context;
    }

    @Override
    public OpenGLContext getContext()
    {
        return this.context;
    }

    abstract class ContentsBase
    {
        abstract int getId();

        void bindForDraw(int x, int y, int width, int height)
        {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getId());
            OpenGLContext.errorCheck();

            if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            {
                OpenGLContext.throwInvalidFramebufferOperationException();
            }
            OpenGLContext.errorCheck();

            glViewport(x, y, width, height);
            OpenGLContext.errorCheck();
        }

        void bindForDraw()
        {
            FramebufferSize size = getSize();
            this.bindForDraw(0, 0, size.width, size.height);
        }

        abstract void selectColorSourceForRead(int index);

        void bindForRead(int attachmentIndex)
        {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, this.getId());
            OpenGLContext.errorCheck();

            if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            {
                OpenGLContext.throwInvalidFramebufferOperationException();
            }
            OpenGLContext.errorCheck();

            selectColorSourceForRead(attachmentIndex);
        }
    }

    @Override
    public abstract ContentsBase getContentsForRead();

    @Override
    public abstract ContentsBase getContentsForWrite();

    @Override
    public void readColorBufferARGB(int attachmentIndex, ByteBuffer destination, int x, int y, int width, int height)
    {
        if (destination.remaining() < width * height * 4)
        {
            throw new IllegalArgumentException("The destination buffer is not big enough to hold the requested data.");
        }

        this.getContentsForRead().bindForRead(attachmentIndex);

        glPixelStorei(GL_PACK_ALIGNMENT, 4);
        OpenGLContext.errorCheck();

        // use BGRA because due to byte order differences it ends up being ARGB
        glReadPixels(x, y, width, height, GL_BGRA, GL_UNSIGNED_BYTE, destination);
        OpenGLContext.errorCheck();
    }

    @Override
    public void readFloatingPointColorBufferRGBA(int attachmentIndex, FloatBuffer destination, int x, int y, int width, int height)
    {
        if (destination.remaining() < width * height * 4)
        {
            throw new IllegalArgumentException("The destination buffer is not big enough to hold the requested data.");
        }

        this.getContentsForRead().bindForRead(attachmentIndex);

        glPixelStorei(GL_PACK_ALIGNMENT, 4);
        OpenGLContext.errorCheck();

        glReadPixels(x, y, width, height, GL_RGBA, GL_FLOAT, destination);
        OpenGLContext.errorCheck();
    }

    @Override
    public void readIntegerColorBufferRGBA(int attachmentIndex, IntBuffer destination, int x, int y, int width, int height)
    {
        if (destination.remaining() < width * height * 4)
        {
            throw new IllegalArgumentException("The destination buffer is not big enough to hold the requested data.");
        }

        this.getContentsForRead().bindForRead(attachmentIndex);

        glPixelStorei(GL_PACK_ALIGNMENT, 4);
        OpenGLContext.errorCheck();

        glReadPixels(x, y, width, height, GL_RGBA_INTEGER, GL_INT, destination);
        OpenGLContext.errorCheck();
    }

    @Override
    public void readDepthBuffer(ShortBuffer destination, int x, int y, int width, int height)
    {
        if (destination.remaining() < width * height)
        {
            throw new IllegalArgumentException("The destination buffer is not big enough to hold the requested data.");
        }

        this.getContentsForRead().bindForRead(0);

        glPixelStorei(GL_PACK_ALIGNMENT, 2);
        OpenGLContext.errorCheck();

        glReadPixels(x, y, width, height, GL_DEPTH_COMPONENT, GL_UNSIGNED_SHORT, destination);
        OpenGLContext.errorCheck();
    }

    @Override
    public void readColorBufferARGB(int attachmentIndex, ByteBuffer destination)
    {
        FramebufferSize size = this.getSize();
        this.readColorBufferARGB(attachmentIndex, destination, 0, 0, size.width, size.height);
    }

    @Override
    public void readFloatingPointColorBufferRGBA(int attachmentIndex, FloatBuffer destination)
    {
        FramebufferSize size = this.getSize();
        this.readFloatingPointColorBufferRGBA(attachmentIndex, destination, 0, 0, size.width, size.height);
    }

    @Override
    public void readIntegerColorBufferRGBA(int attachmentIndex, IntBuffer destination)
    {
        FramebufferSize size = this.getSize();
        this.readIntegerColorBufferRGBA(attachmentIndex, destination, 0, 0, size.width, size.height);
    }

    @Override
    public void readDepthBuffer(ShortBuffer destination)
    {
        FramebufferSize size = this.getSize();
        this.readDepthBuffer(destination, 0, 0, size.width, size.height);
    }

    @Override
    public int[] readColorBufferARGB(int attachmentIndex, int x, int y, int width, int height)
    {
        this.getContentsForRead().bindForRead(attachmentIndex);
        ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(width * height * 4);

        glPixelStorei(GL_PACK_ALIGNMENT, 4);
        OpenGLContext.errorCheck();

        // use BGRA because due to byte order differences it ends up being ARGB
        glReadPixels(x, y, width, height, GL_BGRA, GL_UNSIGNED_BYTE, pixelBuffer);
        OpenGLContext.errorCheck();

        int[] pixelArray = new int[width * height];
        pixelBuffer.asIntBuffer().get(pixelArray);
        return pixelArray;
    }

    @Override
    public int[] readColorBufferARGB(int attachmentIndex)
    {
        FramebufferSize size = this.getSize();
        return this.readColorBufferARGB(attachmentIndex, 0, 0, size.width, size.height);
    }

    @Override
    public float[] readFloatingPointColorBufferRGBA(int attachmentIndex, int x, int y, int width, int height)
    {
        this.getContentsForRead().bindForRead(attachmentIndex);
        FloatBuffer pixelBuffer = BufferUtils.createFloatBuffer(width * height * 4);

        glPixelStorei(GL_PACK_ALIGNMENT, 4);
        OpenGLContext.errorCheck();

        glReadPixels(x, y, width, height, GL_RGBA, GL_FLOAT, pixelBuffer);
        OpenGLContext.errorCheck();

        float[] pixelArray = new float[width * height * 4];
        pixelBuffer.get(pixelArray);
        return pixelArray;
    }

    @Override
    public float[] readFloatingPointColorBufferRGBA(int attachmentIndex)
    {
        FramebufferSize size = this.getSize();
        return this.readFloatingPointColorBufferRGBA(attachmentIndex, 0, 0, size.width, size.height);
    }

    @Override
    public int[] readIntegerColorBufferRGBA(int attachmentIndex, int x, int y, int width, int height)
    {
        this.getContentsForRead().bindForRead(attachmentIndex);
        IntBuffer pixelBuffer = BufferUtils.createIntBuffer(width * height * 4);

        glPixelStorei(GL_PACK_ALIGNMENT, 4);
        OpenGLContext.errorCheck();

        glReadPixels(x, y, width, height, GL_RGBA_INTEGER, GL_INT, pixelBuffer);
        OpenGLContext.errorCheck();

        int[] pixelArray = new int[width * height * 4];
        pixelBuffer.get(pixelArray);
        return pixelArray;
    }

    @Override
    public int[] readIntegerColorBufferRGBA(int attachmentIndex)
    {
        FramebufferSize size = this.getSize();
        return this.readIntegerColorBufferRGBA(attachmentIndex, 0, 0, size.width, size.height);
    }

    @Override
    public void saveColorBufferToFile(int attachmentIndex, String fileFormat, File file) throws IOException
    {
        int[] pixels = this.readColorBufferARGB(attachmentIndex);
        
        // Flip the array vertically
        FramebufferSize size = this.getSize();
        for (int y = 0; y < size.height / 2; y++)
        {
            int limit = (y + 1) * size.width;
            for (int i1 = y * size.width, i2 = (size.height - y - 1) * size.width; i1 < limit; i1++, i2++)
            {
                int tmp = pixels[i1];
                pixels[i1] = pixels[i2];
                pixels[i2] = tmp;
            }
        }
        
        BufferedImage outImg = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        outImg.setRGB(0, 0, size.width, size.height, pixels, 0, size.width);
        ImageIO.write(outImg, fileFormat, file);
    }

    @Override
    public void saveColorBufferToFile(int attachmentIndex, int x, int y, int width, int height, String fileFormat, File file) throws IOException
    {
        int[] pixels = this.readColorBufferARGB(attachmentIndex, x, y, width, height);
        
        // Flip the array vertically
        for (int row = 0; row < height / 2; row++)
        {
            int limit = (row + 1) * width;
            for (int i1 = row * width, i2 = (height - row - 1) * width; i1 < limit; i1++, i2++)
            {
                int tmp = pixels[i1];
                pixels[i1] = pixels[i2];
                pixels[i2] = tmp;
            }
        }
        
        BufferedImage outImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        outImg.setRGB(0, 0, width, height, pixels, 0, width);
        ImageIO.write(outImg, fileFormat, file);
    }

    @Override
    public short[] readDepthBuffer(int x, int y, int width, int height)
    {
        this.getContentsForRead().bindForRead(0);
        ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(width * height * 2);

        glPixelStorei(GL_PACK_ALIGNMENT, 2);
        OpenGLContext.errorCheck();

        glReadPixels(x, y, width, height, GL_DEPTH_COMPONENT, GL_UNSIGNED_SHORT, pixelBuffer);
        OpenGLContext.errorCheck();

        short[] pixelArray = new short[width * height];
        pixelBuffer.asShortBuffer().get(pixelArray);
        return pixelArray;
    }

    @Override
    public short[] readDepthBuffer()
    {
        FramebufferSize size = this.getSize();
        return this.readDepthBuffer(0, 0, size.width, size.height);
    }

    @Override
    public void clearColorBuffer(int attachmentIndex, float r, float g, float b, float a)
    {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getContentsForWrite().getId());
        OpenGLContext.errorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            OpenGLContext.throwInvalidFramebufferOperationException();
        }
        OpenGLContext.errorCheck();

        FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
        buffer.put(r);
        buffer.put(g);
        buffer.put(b);
        buffer.put(a);
        buffer.flip();
        glClearBufferfv(GL_COLOR, attachmentIndex, buffer);
        OpenGLContext.errorCheck();
    }

    @Override
    public void clearIntegerColorBuffer(int attachmentIndex, int r, int g, int b, int a)
    {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getContentsForWrite().getId());
        OpenGLContext.errorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            OpenGLContext.throwInvalidFramebufferOperationException();
        }
        OpenGLContext.errorCheck();

        IntBuffer buffer = BufferUtils.createIntBuffer(4);
        buffer.put(r);
        buffer.put(g);
        buffer.put(b);
        buffer.put(a);
        buffer.flip();
        glClearBufferiv(GL_COLOR, attachmentIndex, buffer);
        OpenGLContext.errorCheck();
    }

    @Override
    public void clearDepthBuffer(float depth)
    {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getContentsForWrite().getId());
        OpenGLContext.errorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            OpenGLContext.throwInvalidFramebufferOperationException();
        }
        OpenGLContext.errorCheck();

        FloatBuffer buffer = BufferUtils.createFloatBuffer(1);
        buffer.put(depth);
        buffer.flip();
        glClearBufferfv(GL_DEPTH, 0, buffer);
        OpenGLContext.errorCheck();
    }

    @Override
    public void clearDepthBuffer()
    {
        this.clearDepthBuffer(1.0f);
    }

    @Override
    public void clearStencilBuffer(int stencilIndex)
    {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getContentsForWrite().getId());
        OpenGLContext.errorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            OpenGLContext.throwInvalidFramebufferOperationException();
        }
        OpenGLContext.errorCheck();

        IntBuffer buffer = BufferUtils.createIntBuffer(1);
        buffer.put(stencilIndex);
        buffer.flip();
        glClearBufferiv(GL_STENCIL, 0, buffer);
        OpenGLContext.errorCheck();
    }
}
