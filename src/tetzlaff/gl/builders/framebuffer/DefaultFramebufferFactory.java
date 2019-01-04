package tetzlaff.gl.builders.framebuffer;

import java.io.File;
import java.io.IOException;
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
        public void readColorBufferARGB(int attachmentIndex, ByteBuffer destination, int x, int y, int width, int height)
        {
            frontFBO.readColorBufferARGB(attachmentIndex, destination, x, y, width, height);
        }

        @Override
        public void readFloatingPointColorBufferRGBA(int attachmentIndex, FloatBuffer destination, int x, int y, int width, int height)
        {
            frontFBO.readFloatingPointColorBufferRGBA(attachmentIndex, destination, x, y, width, height);
        }

        @Override
        public void readIntegerColorBufferRGBA(int attachmentIndex, IntBuffer destination, int x, int y, int width, int height)
        {
            frontFBO.readIntegerColorBufferRGBA(attachmentIndex, destination, x, y, width, height);
        }

        @Override
        public void readDepthBuffer(ShortBuffer destination, int x, int y, int width, int height)
        {
            frontFBO.readDepthBuffer(x, y, width, height);
        }

        @Override
        public void readColorBufferARGB(int attachmentIndex, ByteBuffer destination)
        {
            frontFBO.readColorBufferARGB(attachmentIndex, destination);
        }

        @Override
        public void readFloatingPointColorBufferRGBA(int attachmentIndex, FloatBuffer destination)
        {
            frontFBO.readFloatingPointColorBufferRGBA(attachmentIndex, destination);
        }

        @Override
        public void readIntegerColorBufferRGBA(int attachmentIndex, IntBuffer destination)
        {
            frontFBO.readIntegerColorBufferRGBA(attachmentIndex, destination);
        }

        @Override
        public void readDepthBuffer(ShortBuffer destination)
        {
            frontFBO.readDepthBuffer(destination);
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
