package openGL.wrappers.implementations;
import static openGL.OpenGLHelper.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

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
	public int[] readPixelsARGB(int attachmentIndex, int x, int y, int width, int height)
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
	public int[] readPixelsARGB(int attachmentIndex)
	{
		return this.readPixelsARGB(attachmentIndex, 0, 0, this.getWidth(), this.getHeight());
	}
	
	@Override
	public void saveToFile(int attachmentIndex, String fileFormat, String filename) throws IOException
	{
        int[] pixels = this.readPixelsARGB(attachmentIndex);
        BufferedImage outImg = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
        outImg.setRGB(0, 0, this.getWidth(), this.getHeight(), pixels, 0, this.getWidth());
        File outputFile = new File(filename);
        ImageIO.write(outImg, fileFormat, outputFile);
	}
}
