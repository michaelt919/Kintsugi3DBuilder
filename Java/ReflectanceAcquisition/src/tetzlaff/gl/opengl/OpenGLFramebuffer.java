package tetzlaff.gl.opengl;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;

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

public abstract class OpenGLFramebuffer implements Framebuffer 
{		
	protected abstract int getId();
	
	public abstract FramebufferSize getSize();

	void bindForDraw(int x, int y, int width, int height)
	{
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getId());
		openGLErrorCheck();
		glViewport(x, y, width, height);
		openGLErrorCheck();
	}
	
	void bindForDraw()
	{
		FramebufferSize size = this.getSize();
		this.bindForDraw(0, 0, size.width, size.height);
	}
	
	protected abstract void selectColorSourceForRead(int index);
	
	protected void bindForRead(int attachmentIndex)
	{
		glBindFramebuffer(GL_READ_FRAMEBUFFER, this.getId());
		openGLErrorCheck();
		selectColorSourceForRead(attachmentIndex);
	}
	
	@Override
	public int[] readColorBufferARGB(int attachmentIndex, int x, int y, int width, int height)
	{
		this.bindForRead(attachmentIndex);
		ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(width * height * 4);
		
		// use BGRA because due to byte order differences it ends up being ARGB
		glReadPixels(x, y, width, height, GL_BGRA, GL_UNSIGNED_BYTE, pixelBuffer);
		openGLErrorCheck();
		
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
		
		// use BGRA because due to byte order differences it ends up being ARGB
		glReadPixels(x, y, width, height, GL_RGBA, GL_FLOAT, pixelBuffer);
		openGLErrorCheck();
		
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
	public short[] readDepthBuffer(int x, int y, int width, int height)
	{
		this.bindForRead(0);
		ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(width * height * 4);
		
		glReadPixels(x, y, width, height, GL_DEPTH_COMPONENT, GL_UNSIGNED_SHORT, pixelBuffer);
		openGLErrorCheck();
		
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
		openGLErrorCheck();
		FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
		buffer.put(r);
		buffer.put(g);
		buffer.put(b);
		buffer.put(a);
		buffer.flip();
		glClearBuffer(GL_COLOR, attachmentIndex, buffer);
		openGLErrorCheck();
	}
	
	@Override
	public void clearDepthBuffer(float depth)
	{
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getId());
		openGLErrorCheck();
		FloatBuffer buffer = BufferUtils.createFloatBuffer(1);
		buffer.put(depth);
		buffer.flip();
		glClearBuffer(GL_DEPTH, 0, buffer);
		openGLErrorCheck();
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
		openGLErrorCheck();
		IntBuffer buffer = BufferUtils.createIntBuffer(1);
		buffer.put(stencilIndex);
		buffer.flip();
		glClearBuffer(GL_STENCIL, 0, buffer);
		openGLErrorCheck();
	}
}
