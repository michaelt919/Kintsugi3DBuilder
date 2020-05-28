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

package tetzlaff.gl.builders.framebuffer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import tetzlaff.gl.core.*;

public final class DefaultFramebufferFactory
{
    private DefaultFramebufferFactory()
    {
    }

    public static <ContextType extends Context<ContextType>> DoubleFramebufferObject<ContextType> create(
        ContextType context, int initWidth, int initHeight)
    {
        return new DefaultFramebufferObjectImpl<>(context, initWidth, initHeight);
    }

    private static class DefaultFramebufferObjectImpl<ContextType extends Context<ContextType>>
        implements DoubleFramebufferObject<ContextType>
    {
        private final ContextType context;
        private FramebufferObject<ContextType> fbo1;
        private FramebufferObject<ContextType> fbo2;

        private Framebuffer<ContextType> frontFBO;
        private Framebuffer<ContextType> backFBO;

        private int width;
        private int height;

        private int newWidth;
        private int newHeight;

        private List<Consumer<Framebuffer<ContextType>>> swapListeners = new ArrayList<>(1);

        DefaultFramebufferObjectImpl(ContextType context, int initWidth, int initHeight)
        {
            this.context = context;

            this.fbo1 = context.buildFramebufferObject(initWidth, initHeight)
                .addColorAttachment(ColorFormat.RGBA8)
                .addDepthAttachment()
                .createFramebufferObject();

            this.fbo2 = context.buildFramebufferObject(initWidth, initHeight)
                .addColorAttachment(ColorFormat.RGBA8)
                .addDepthAttachment()
                .createFramebufferObject();

            this.frontFBO = fbo1;
            this.backFBO = fbo2;

            this.width = initWidth;
            this.height = initHeight;

            this.newWidth = initWidth;
            this.newHeight = initHeight;
        }

        private void resize()
        {
            if (width != backFBO.getSize().width || height !=  backFBO.getSize().height)
            {
                if (backFBO == fbo1)
                {
                    fbo1.close();

                    this.fbo1 = context.buildFramebufferObject(newWidth, newHeight)
                        .addColorAttachment(ColorFormat.RGBA8)
                        .addDepthAttachment()
                        .createFramebufferObject();

                    this.backFBO = fbo1;
                }
                else
                {
                    fbo2.close();

                    this.fbo2 = context.buildFramebufferObject(newWidth, newHeight)
                        .addColorAttachment(ColorFormat.RGBA8)
                        .addDepthAttachment()
                        .createFramebufferObject();

                    this.backFBO = fbo2;
                }
            }
        }

        @Override
        public Object getContentsForRead()
        {
            return this.frontFBO.getContentsForRead();
        }

        @Override
        public Object getContentsForWrite()
        {
            return this.backFBO.getContentsForWrite();
        }

        @Override
        public ContextType getContext()
        {
            return context;
        }

        @Override
        public FramebufferSize getSize()
        {
            return frontFBO.getSize();
        }

        @Override
        public int[] readColorBufferARGB(int attachmentIndex)
        {
            return frontFBO.readColorBufferARGB(attachmentIndex);
        }

        @Override
        public int[] readColorBufferARGB(int attachmentIndex, int x, int y, int width, int height)
        {
            return frontFBO.readColorBufferARGB(attachmentIndex, x, y, width, height);
        }

        @Override
        public float[] readFloatingPointColorBufferRGBA(int attachmentIndex, int x, int y, int width, int height)
        {
            return frontFBO.readFloatingPointColorBufferRGBA(attachmentIndex, x, y, width, height);
        }

        @Override
        public float[] readFloatingPointColorBufferRGBA(int attachmentIndex)
        {
            return frontFBO.readFloatingPointColorBufferRGBA(attachmentIndex);
        }

        @Override
        public int[] readIntegerColorBufferRGBA(int attachmentIndex, int x, int y, int width, int height)
        {
            return frontFBO.readIntegerColorBufferRGBA(attachmentIndex, x, y, width, height);
        }

        @Override
        public int[] readIntegerColorBufferRGBA(int attachmentIndex)
        {
            return frontFBO.readIntegerColorBufferRGBA(attachmentIndex);
        }

        @Override
        public short[] readDepthBuffer(int x, int y, int width, int height)
        {
            return frontFBO.readDepthBuffer(x, y, width, height);
        }

        @Override
        public short[] readDepthBuffer()
        {
            return frontFBO.readDepthBuffer();
        }

        @Override
        public void saveColorBufferToFile(int attachmentIndex, String fileFormat, File file) throws IOException
        {
            frontFBO.saveColorBufferToFile(attachmentIndex, fileFormat, file);
        }

        @Override
        public void saveColorBufferToFile(int attachmentIndex, int x, int y, int width, int height, String fileFormat, File file) throws IOException
        {
            frontFBO.saveColorBufferToFile(attachmentIndex, x, y, width, height, fileFormat, file);
        }

        @Override
        public void clearColorBuffer(int attachmentIndex, float r, float g, float b, float a)
        {
            backFBO.clearColorBuffer(attachmentIndex, r, g, b, a);
        }

        @Override
        public void clearIntegerColorBuffer(int attachmentIndex, int r, int g, int b, int a)
        {
            backFBO.clearIntegerColorBuffer(attachmentIndex, r, g, b, a);
        }

        @Override
        public void clearDepthBuffer(float depth)
        {
            backFBO.clearDepthBuffer(depth);
        }

        @Override
        public void clearDepthBuffer()
        {
            backFBO.clearDepthBuffer();
        }

        @Override
        public void clearStencilBuffer(int stencilIndex)
        {
            backFBO.clearStencilBuffer(stencilIndex);
        }

        @Override
        public void requestResize(int width, int height)
        {
            this.newWidth = width;
            this.newHeight = height;
        }

        @Override
        public void swapBuffers()
        {
            Framebuffer<ContextType> tmp = backFBO;
            backFBO = frontFBO;
            frontFBO = tmp;

            width = newWidth;
            height = newHeight;

            resize();

            for (Consumer<Framebuffer<ContextType>> l : swapListeners)
            {
                l.accept(frontFBO);
            }
        }

        @Override
        public void addSwapListener(Consumer<Framebuffer<ContextType>> listener)
        {
            this.swapListeners.add(listener);
        }

        @Override
        public void close()
        {
            fbo1.close();
            fbo2.close();
        }
    }
}
