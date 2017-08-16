package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.FramebufferSize;

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

    abstract int getId();

    void bindForDraw(int x, int y, int width, int height)
    {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getId());
        this.context.openGLErrorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            this.context.throwInvalidFramebufferOperationException();
        }
        this.context.openGLErrorCheck();

        glViewport(x, y, width, height);
        this.context.openGLErrorCheck();
    }

    void bindForDraw()
    {
        FramebufferSize size = this.getSize();
        this.bindForDraw(0, 0, size.width, size.height);
    }

    abstract void selectColorSourceForRead(int index);

    void bindForRead(int attachmentIndex)
    {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, this.getId());
        this.context.openGLErrorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            this.context.throwInvalidFramebufferOperationException();
        }
        this.context.openGLErrorCheck();

        selectColorSourceForRead(attachmentIndex);
    }

    @Override
    public int[] readColorBufferARGB(int attachmentIndex, int x, int y, int width, int height)
    {
        this.bindForRead(attachmentIndex);
        ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(width * height * 4);

        glPixelStorei(GL_PACK_ALIGNMENT, 4);
        this.context.openGLErrorCheck();

        // use BGRA because due to byte order differences it ends up being ARGB
        glReadPixels(x, y, width, height, GL_BGRA, GL_UNSIGNED_BYTE, pixelBuffer);
        this.context.openGLErrorCheck();

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
        this.bindForRead(attachmentIndex);
        FloatBuffer pixelBuffer = BufferUtils.createFloatBuffer(width * height * 4);

        glPixelStorei(GL_PACK_ALIGNMENT, 4);
        this.context.openGLErrorCheck();

        glReadPixels(x, y, width, height, GL_RGBA, GL_FLOAT, pixelBuffer);
        this.context.openGLErrorCheck();

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
        this.bindForRead(attachmentIndex);
        IntBuffer pixelBuffer = BufferUtils.createIntBuffer(width * height * 4);

        glPixelStorei(GL_PACK_ALIGNMENT, 4);
        this.context.openGLErrorCheck();

        glReadPixels(x, y, width, height, GL_RGBA_INTEGER, GL_INT, pixelBuffer);
        this.context.openGLErrorCheck();

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
        this.bindForRead(0);
        ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(width * height * 2);

        glPixelStorei(GL_PACK_ALIGNMENT, 2);
        this.context.openGLErrorCheck();

        glReadPixels(x, y, width, height, GL_DEPTH_COMPONENT, GL_UNSIGNED_SHORT, pixelBuffer);
        this.context.openGLErrorCheck();

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
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getId());
        this.context.openGLErrorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            this.context.throwInvalidFramebufferOperationException();
        }
        this.context.openGLErrorCheck();

        FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
        buffer.put(r);
        buffer.put(g);
        buffer.put(b);
        buffer.put(a);
        buffer.flip();
        glClearBufferfv(GL_COLOR, attachmentIndex, buffer);
        this.context.openGLErrorCheck();
    }

    @Override
    public void clearIntegerColorBuffer(int attachmentIndex, int r, int g, int b, int a)
    {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getId());
        this.context.openGLErrorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            this.context.throwInvalidFramebufferOperationException();
        }
        this.context.openGLErrorCheck();

        IntBuffer buffer = BufferUtils.createIntBuffer(4);
        buffer.put(r);
        buffer.put(g);
        buffer.put(b);
        buffer.put(a);
        buffer.flip();
        glClearBufferiv(GL_COLOR, attachmentIndex, buffer);
        this.context.openGLErrorCheck();
    }

    @Override
    public void clearDepthBuffer(float depth)
    {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getId());
        this.context.openGLErrorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            this.context.throwInvalidFramebufferOperationException();
        }
        this.context.openGLErrorCheck();

        FloatBuffer buffer = BufferUtils.createFloatBuffer(1);
        buffer.put(depth);
        buffer.flip();
        glClearBufferfv(GL_DEPTH, 0, buffer);
        this.context.openGLErrorCheck();
    }

    @Override
    public void clearDepthBuffer()
    {
        this.clearDepthBuffer(1.0f);
    }

    @Override
    public void clearStencilBuffer(int stencilIndex)
    {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getId());
        this.context.openGLErrorCheck();

        if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            this.context.throwInvalidFramebufferOperationException();
        }
        this.context.openGLErrorCheck();

        IntBuffer buffer = BufferUtils.createIntBuffer(1);
        buffer.put(stencilIndex);
        buffer.flip();
        glClearBufferiv(GL_STENCIL, 0, buffer);
        this.context.openGLErrorCheck();
    }
}
