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

package tetzlaff.gl.builders.framebuffer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
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
        implements DoubleFramebufferObject<ContextType>, Framebuffer<ContextType>
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

        private final List<Consumer<Framebuffer<ContextType>>> swapListeners = new ArrayList<>(1);

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
        public FramebufferReadContents<ContextType> getReadContents()
        {
            return this.frontFBO.getReadContents();
        }

        @Override
        public FramebufferDrawContents<ContextType> getDrawContents()
        {
            return this.backFBO.getDrawContents();
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
        public int getColorAttachmentCount()
        {
            return 1;
        }

        @Override
        public ColorTextureReader getTextureReaderForColorAttachment(int attachmentIndex)
        {
            return frontFBO.getTextureReaderForColorAttachment(attachmentIndex);
        }

        @Override
        public DepthTextureReader getTextureReaderForDepthAttachment()
        {
            return frontFBO.getTextureReaderForDepthAttachment();
        }

        @Override
        public void clearColorBuffer(int attachmentIndex, float r, float g, float b, float a, int x, int y, int width, int height)
        {
            backFBO.clearColorBuffer(attachmentIndex, r, g, b, a, x, y, width, height);
        }

        @Override
        public void clearIntegerColorBuffer(int attachmentIndex, int r, int g, int b, int a, int x, int y, int width, int height)
        {
            backFBO.clearIntegerColorBuffer(attachmentIndex, r, g, b, a, x, y, width, height);
        }

        @Override
        public void clearDepthBuffer(float depth, int x, int y, int width, int height)
        {
            backFBO.clearDepthBuffer(depth, x, y, width, height);
        }

        @Override
        public void clearStencilBuffer(int stencilIndex, int x, int y, int width, int height)
        {
            backFBO.clearStencilBuffer(stencilIndex, x, y, width, height);
        }

        @Override
        public void blitColorAttachmentFromFramebufferViewport(int drawAttachmentIndex, int destX, int destY, int destWidth, int destHeight,
            FramebufferViewport<ContextType> readFramebuffer, int readAttachmentIndex, boolean linearFiltering)
        {
            backFBO.blitColorAttachmentFromFramebufferViewport(drawAttachmentIndex, destX, destY, destWidth, destHeight,
                readFramebuffer, readAttachmentIndex, linearFiltering);
        }

        @Override
        public void blitDepthAttachmentFromFramebufferViewport(int destX, int destY, int destWidth, int destHeight,
            FramebufferViewport<ContextType> readFramebuffer)
        {
            backFBO.blitDepthAttachmentFromFramebufferViewport(destX, destY, destWidth, destHeight, readFramebuffer);
        }

        @Override
        public void blitStencilAttachmentFromFramebufferViewport(int destX, int destY, int destWidth, int destHeight,
            FramebufferViewport<ContextType> readFramebuffer)
        {
            backFBO.blitStencilAttachmentFromFramebufferViewport(destX, destY, destWidth, destHeight, readFramebuffer);
        }

        @Override
        public void blitDepthStencilAttachmentFromFramebufferViewport(int destX, int destY, int destWidth, int destHeight,
            FramebufferViewport<ContextType> readFramebuffer)
        {
            backFBO.blitDepthStencilAttachmentFromFramebufferViewport(destX, destY, destWidth, destHeight, readFramebuffer);
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
