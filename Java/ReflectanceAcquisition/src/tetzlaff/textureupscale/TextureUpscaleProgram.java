package tetzlaff.textureupscale;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLFramebufferObject;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLResource;
import tetzlaff.gl.opengl.OpenGLTexture2D;
import tetzlaff.gl.opengl.OpenGLTextureArray;
import tetzlaff.gl.opengl.OpenGLVertexBuffer;
import tetzlaff.ulf.UnstructuredLightField;
import tetzlaff.window.glfw.GLFWWindow;

public class TextureUpscaleProgram
{
	private static final float GAMMA = 2.2f;
	private static final int SCALE_FACTOR = 16;
	private static final float CLOUD_AMPLITUDE = 8.0f;
	private static final int CLOUD_SCALE = 16;
	private static final int CLOUD_DEPTH = 8;
	private static final int SAMPLE_RADIUS = 8;
	private static final float WEIGHT_EXPONENT = 1.0f;
	private static final float SHARPNESS = 0.0f;
	private static final int MAX_SAMPLES = 32;
	
	public static void main(String[] args)
    {
    	OpenGLContext context = new GLFWWindow(800, 800, "Texture Upscale");
        try
        {
	    	OpenGLProgram perlinNoiseProgram = new OpenGLProgram(new File("shaders", "passthrough2d.vert"), new File("shaders", "perlintex.frag"));
	    	OpenGLTexture2D permTexture = OpenGLTexture2D.createPerlinNoise();
	    	perlinNoiseProgram.setTexture("permTexture", permTexture);
    		
    		JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
    		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
    		fileChooser.setFileFilter(new FileNameExtensionFilter("PNG images (.png)", "png"));
    		
    		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
    		{
    	    	File imageFile = fileChooser.getSelectedFile();
    	    	OpenGLTexture2D imageTexture = new OpenGLTexture2D(imageFile, true, true, true);
    	    	OpenGLTexture2D segmentTexture = new OpenGLTexture2D(new File(new File(imageFile.getParent(), "segment"), imageFile.getName()), true, false, false);
    	    	perlinNoiseProgram.setTexture("imageTexture", imageTexture);
    	    	perlinNoiseProgram.setTexture("segmentTexture", segmentTexture);
    	    	int targetWidth = imageTexture.getWidth() * SCALE_FACTOR;
    	    	int targetHeight = imageTexture.getHeight() * SCALE_FACTOR;
    	    	perlinNoiseProgram.setUniform("gamma", GAMMA);
    	    	perlinNoiseProgram.setUniform("imageWidth", imageTexture.getWidth());
    	    	perlinNoiseProgram.setUniform("imageHeight", imageTexture.getHeight());
    	    	perlinNoiseProgram.setUniform("targetWidth", targetWidth);
    	    	perlinNoiseProgram.setUniform("targetHeight", targetHeight);
    	    	perlinNoiseProgram.setUniform("cloudAmplitude", CLOUD_AMPLITUDE);
    	    	perlinNoiseProgram.setUniform("cloudScale", CLOUD_SCALE);
    	    	perlinNoiseProgram.setUniform("cloudDepth", CLOUD_DEPTH);
    	    	perlinNoiseProgram.setUniform("sampleRadius", SAMPLE_RADIUS);
    	    	perlinNoiseProgram.setUniform("weightExponent", WEIGHT_EXPONENT);
    	    	perlinNoiseProgram.setUniform("sharpness", SHARPNESS);
    	    	perlinNoiseProgram.setUniform("maxSamples", MAX_SAMPLES);
    	    	perlinNoiseProgram.setUniform("blackPoint", new Vector4(0.0f, 0.0f, 0.0f, 0.0f));
    	    	perlinNoiseProgram.setUniform("whitePoint", new Vector4(1.0f, 1.0f, 1.0f, 1.0f));
		    	OpenGLFramebufferObject fbo = new OpenGLFramebufferObject(targetWidth, targetHeight, 1, false, false);
		    	OpenGLRenderable renderable = new OpenGLRenderable(perlinNoiseProgram);
		    	renderable.addVertexBuffer("position", OpenGLVertexBuffer.createRectangle(), true);
		    	fbo.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	renderable.draw(PrimitiveMode.TRIANGLE_FAN, fbo);
		    	new File(imageFile.getParentFile(), "output").mkdirs();
		        fbo.saveColorBufferToFile(0, "PNG", new File(new File(imageFile.getParentFile(), "output"), imageFile.getName()));
		    	fbo.delete();
		    	imageTexture.delete();
    		}
    		
    		perlinNoiseProgram.delete();
    		permTexture.delete();
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
        System.out.println("Process terminated without errors.");
        GLFWWindow.closeAllWindows();
        System.exit(0);
	}
}
