package openGL.wrappers.implementations;
import static openGL.OpenGLHelper.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;

import java.util.AbstractCollection;

import openGL.wrappers.interfaces.Framebuffer;
import openGL.wrappers.interfaces.FramebufferAttachment;

public abstract class OpenGLFramebuffer implements Framebuffer 
{	
	public static Framebuffer defaultFramebuffer()
	{
		return OpenGLDefaultFramebuffer.getInstance();
	}
	
	protected abstract int getId();
	
	public abstract int getWidth();
	public abstract int getHeight();

	@Override
	public void bindForDraw(int x, int y, int width, int height)
	{
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.getId());
		openGLErrorCheck();
		glViewport(x, y, width, height);
		openGLErrorCheck();
	}
	
	@Override
	public void bindForDraw()
	{
		this.bindForDraw(0, 0, this.getWidth(), this.getHeight());
	}
	
	protected abstract void selectColorSourceForRead(int index);
	
	protected void bindForRead(int attachmentIndex)
	{
		glBindFramebuffer(GL_READ_FRAMEBUFFER, this.getId());
		openGLErrorCheck();
		selectColorSourceForRead(attachmentIndex);
	}
	
	@Override
	public int[] readPixelsRGBA(int attachmentIndex, int x, int y, int width, int height)
	{
		this.bindForRead(attachmentIndex);
		ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * 4);
		glReadPixels(x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);
		openGLErrorCheck();
		int[] pixelArray = new int[width * height];
		for (int i = 0; i < pixelArray.length; i++) { pixelArray[i] = 0xFF0000FF; }
		pixelBuffer.asIntBuffer().get(pixelArray);
		return pixelArray;
	}

	@Override
	public int[] readPixelsRGBA(int attachmentIndex)
	{
		return this.readPixelsRGBA(attachmentIndex, 0, 0, this.getWidth(), this.getHeight());
	}
	
	@Override
	public void saveToFile(int attachmentIndex, String fileFormat, String filename) throws IOException
	{
        int[] pixels = this.readPixelsRGBA(attachmentIndex);
        for (int i = 0; i < pixels.length; i++)
        {
        	// Switch from RGBA to ARGB
        	if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN)
        	{
        		pixels[i] = (pixels[i] << 8) | (pixels[i] >>> 24);
        	}
        	else
        	{
        		pixels[i] = (pixels[i] >>> 8) | (pixels[i] << 24);
        	}
        }
        BufferedImage outImg = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
        outImg.setRGB(0, 0, this.getWidth(), this.getHeight(), pixels, 0, this.getWidth());
        File outputFile = new File(filename);
        ImageIO.write(outImg, fileFormat, outputFile);
	}
}
