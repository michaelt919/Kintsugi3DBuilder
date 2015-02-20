package tetzlaff.reflacq;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.gl.AlphaBlendingFunction;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLFramebufferObject;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLResource;
import tetzlaff.gl.opengl.OpenGLTextureArray;
import tetzlaff.gl.opengl.OpenGLVertexBuffer;
import tetzlaff.ulf.UnstructuredLightField;
import tetzlaff.window.glfw.GLFWWindow;

public class TexGenProgram
{
	public static void main(String[] args)
    {
    	Date timestamp;
		
    	OpenGLContext ulfToTexContext = new GLFWWindow(800, 800, "Texture Generation");
    	ulfToTexContext.enableDepthTest();
    	ulfToTexContext.enableBackFaceCulling();
    	
    	int textureSize = 1024;
    	float gamma = 1.0f; // 2.2f;
    	Vector3 guessSpecularColor = new Vector3(1.0f, 1.0f, 1.0f);
    	float guessSpecularRoughness = 0.5f;
    	float guessSpecularWeight = 10.0f;
    	int specularRange = 10; // +/- n pixels in each direction
    	float expectedWeightSum = 0.125f;
    	float diffuseRemovalFactor = 0.0f;
    	
    	int debugPixelX = 512, debugPixelY = 1004;
    	
        try
        {
	    	OpenGLProgram worldToTextureProgram = new OpenGLProgram(new File("shaders\\texspace.vert"), new File("shaders\\projtex.frag"));
    		OpenGLProgram modelFitProgram = new OpenGLProgram(new File("shaders\\texspace.vert"), new File("shaders\\modelfit.frag"));
    		OpenGLProgram specularDebugProgram = new OpenGLProgram(new File("shaders\\texspace.vert"), new File("shaders\\speculardebug.frag"));
    		
    		JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
    		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
    		fileChooser.setFileFilter(new FileNameExtensionFilter("View Set files (.vset)", "vset"));
    		
    		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
    		{
    	    	String vsetFile = fileChooser.getSelectedFile().getPath();
    	    	String lightFieldDirectory = fileChooser.getSelectedFile().getParent();
    	    	UnstructuredLightField lightField = UnstructuredLightField.loadFromVSETFile(vsetFile);
    		
	    		lightField.settings.setOcclusionBias(0.02f); // For the cube test
	    		
	    		System.out.println("Projecting light field images into texture space...");
		    	timestamp = new Date();
	    		
	    		OpenGLTextureArray imageTextures = new OpenGLTextureArray(textureSize, textureSize, lightField.viewSet.getCameraPoseCount(), true, false);
	    		OpenGLTextureArray depthTextures = new OpenGLTextureArray(textureSize, textureSize, lightField.viewSet.getCameraPoseCount(), true, false);
	    		//OpenGLTextureArray depthTextures = OpenGLTextureArray.createDepthTextureArray(textureSize, textureSize, lightField.viewSet.getCameraPoseCount(), true, false);
		    	OpenGLFramebufferObject worldToTextureFBO = new OpenGLFramebufferObject(textureSize, textureSize, 2, false);
		    	OpenGLRenderable worldToTextureRenderable = new OpenGLRenderable(worldToTextureProgram);
		    	
		    	Iterable<OpenGLResource> worldToTextureVBOResources = worldToTextureRenderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
		    	worldToTextureRenderable.program().setTexture("imageTextures", lightField.viewSet.getTextures());
		    	worldToTextureRenderable.program().setTexture("depthTextures", lightField.depthTextures);
		    	worldToTextureRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	worldToTextureRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
		    	worldToTextureRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
		    	worldToTextureRenderable.program().setUniform("occlusionEnabled", lightField.settings.isOcclusionEnabled());
		    	worldToTextureRenderable.program().setUniform("occlusionBias", lightField.settings.getOcclusionBias());
		    	
		    	new File(lightFieldDirectory + "\\output\\debug\\diffuse\\projpos").mkdirs();
		    	
		    	for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
		    	{
		    		worldToTextureFBO.setColorAttachment(0, imageTextures.getLayerAsFramebufferAttachment(i));
		    		worldToTextureFBO.setColorAttachment(1, depthTextures.getLayerAsFramebufferAttachment(i));
		    		worldToTextureRenderable.program().setUniform("cameraPoseIndex", i);
		    		
		    		worldToTextureFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    		worldToTextureFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    		worldToTextureFBO.clearDepthBuffer();
		    		worldToTextureRenderable.draw(PrimitiveMode.TRIANGLES, worldToTextureFBO);
		    		
		    		//worldToTextureFBO.saveColorBufferToFile(0, "PNG", String.format(lightFieldDirectory + "\\output\\debug\\diffuse\\%04d.png", i));
		    		//worldToTextureFBO.saveColorBufferToFile(1, "PNG", String.format(lightFieldDirectory + "\\output\\debug\\diffuse\\projpos\\%04d.png", i));
		    	}		    	
		    	
		    	worldToTextureFBO.delete();
		    	for (OpenGLResource r : worldToTextureVBOResources)
		    	{
		    		r.delete();
		    	}
		    	
				System.out.println("Projections completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	
		    	System.out.println("Fitting to model...");
		    	timestamp = new Date();
		    	
		    	OpenGLRenderable renderable = new OpenGLRenderable(modelFitProgram);
		    	
		    	renderable.addVertexMesh("position", "texCoord", null, lightField.proxy);
		    	renderable.program().setTexture("imageTextures", imageTextures);
		    	renderable.program().setTexture("depthTextures", depthTextures);
		    	renderable.program().setUniform("textureCount", lightField.viewSet.getCameraPoseCount());
		    	renderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	renderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
		    	renderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
		    	renderable.program().setUniform("gamma", gamma);
		    	renderable.program().setUniform("guessSpecularColor", guessSpecularColor);
		    	renderable.program().setUniform("guessSpecularRoughness", guessSpecularRoughness);
		    	renderable.program().setUniform("guessSpecularWeight", guessSpecularWeight);
		    	renderable.program().setUniform("specularRange", specularRange);
		    	renderable.program().setUniform("expectedWeightSum", expectedWeightSum);
		    	renderable.program().setUniform("diffuseRemovalFactor", diffuseRemovalFactor);
		    	
		    	OpenGLFramebufferObject framebuffer = new OpenGLFramebufferObject(textureSize, textureSize, 8, false);
		    	
		    	new File(lightFieldDirectory + "\\output\\textures").mkdirs();
		    	
		    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    	framebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		    	framebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
		    	framebuffer.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);
		    	framebuffer.clearColorBuffer(5, 0.0f, 0.0f, 0.0f, 0.0f);
		    	framebuffer.clearColorBuffer(6, 0.0f, 0.0f, 0.0f, 0.0f);
		    	framebuffer.clearColorBuffer(7, 0.0f, 0.0f, 0.0f, 0.0f);
		    	framebuffer.clearDepthBuffer();

		    	System.out.println("Model fitting completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	
		    	System.out.println("Saving textures...");
		    	timestamp = new Date();
		    	
		        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
		        
		        framebuffer.saveColorBufferToFile(0, "PNG", lightFieldDirectory + "\\output\\textures\\diffuse.png");
		        framebuffer.saveColorBufferToFile(1, "PNG", lightFieldDirectory + "\\output\\textures\\normal.png");
		        framebuffer.saveColorBufferToFile(2, "PNG", lightFieldDirectory + "\\output\\textures\\specular.png");
		        framebuffer.saveColorBufferToFile(3, "PNG", lightFieldDirectory + "\\output\\textures\\roughness.png");
		        framebuffer.saveColorBufferToFile(4, "PNG", lightFieldDirectory + "\\output\\debug\\debug0.png");
		        framebuffer.saveColorBufferToFile(5, "PNG", lightFieldDirectory + "\\output\\debug\\debug1.png");
		        framebuffer.saveColorBufferToFile(6, "PNG", lightFieldDirectory + "\\output\\debug\\debug2.png");
		        framebuffer.saveColorBufferToFile(7, "PNG", lightFieldDirectory + "\\output\\debug\\debug3.png");
		        
		        System.out.println();
		        int[] debug1Data = framebuffer.readColorBufferARGB(5, debugPixelX, debugPixelY, 1, 1);
		        double a = ((debug1Data[0] & 0x00FF0000) >>> 16) / 255.0 * 4.0;
	    		double b = ((debug1Data[0] & 0x0000FF00) >>> 8) / 255.0 * 8.0 - 6.0;
		        System.out.println(a);
		        System.out.println(b);
		        System.out.println(Math.exp(b));
		        System.out.println(1.0 / Math.sqrt(2.0 * a));
		        System.out.println();
		        
	    		int[] debug2Data = framebuffer.readColorBufferARGB(6, debugPixelX, debugPixelY, 1, 1);
	    		System.out.println(-((debug2Data[0] & 0x00FF0000) >>> 16) / 255.0);
	    		System.out.println(((debug2Data[0] & 0x0000FF00) >>> 8) / 255.0);
	    		System.out.println((debug2Data[0] & 0x000000FF) / 255.0);
		        System.out.println();
		        
	    		int[] debug3Data = framebuffer.readColorBufferARGB(7, debugPixelX, debugPixelY, 1, 1);
	    		System.out.println(-((debug3Data[0] & 0x00FF0000) >>> 16) / 255.0);
	    		System.out.println(((debug3Data[0] & 0x0000FF00) >>> 8) / 255.0);
	    		System.out.println();
	    		
		    	System.out.println("Textures saved in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	
		    	System.out.println("Generating specular debug info...");
		    	timestamp = new Date();
	    		
		    	OpenGLFramebufferObject specularDebugFBO = new OpenGLFramebufferObject(textureSize, textureSize, 2, false);
		    	OpenGLRenderable specularDebugRenderable = new OpenGLRenderable(specularDebugProgram);
		    	
		    	Iterable<OpenGLResource> specularDebugResources = specularDebugRenderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
		    	specularDebugRenderable.program().setTexture("textures", imageTextures);
		    	specularDebugRenderable.program().setTexture("diffuse", framebuffer.getColorAttachmentTexture(0));
		    	specularDebugRenderable.program().setTexture("normalMap", framebuffer.getColorAttachmentTexture(1));
		    	specularDebugRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	specularDebugRenderable.program().setUniform("gamma", gamma);
		    	specularDebugRenderable.program().setUniform("diffuseRemovalFactor", diffuseRemovalFactor);
		    	
		    	new File(lightFieldDirectory + "\\output\\debug\\specular\\rDotV").mkdirs();
		    	
		    	//ulfToTexContext.setAlphaBlendingFunction(new AlphaBlendingFunction(
		    	//		AlphaBlendingFunction.Weight.SRC_ALPHA, 
		    	//		AlphaBlendingFunction.Weight.ONE_MINUS_SRC_ALPHA));
		    	
		    	PrintStream debugInfo = new PrintStream(lightFieldDirectory + "\\output\\debug\\debugInfo.txt");
		    	
		    	for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
		    	{
		    		specularDebugRenderable.program().setUniform("textureIndex", i);
		    		
		    		specularDebugFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    		specularDebugFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    		specularDebugFBO.clearDepthBuffer();
		    		specularDebugRenderable.draw(PrimitiveMode.TRIANGLES, specularDebugFBO);
		    		
		    		//specularDebugFBO.saveColorBufferToFile(0, "PNG", String.format(lightFieldDirectory + "\\output\\debug\\specular\\%04d.png", i));
		    		//specularDebugFBO.saveColorBufferToFile(1, "PNG", String.format(lightFieldDirectory + "\\output\\debug\\specular\\rDotV\\%04d.png", i));
		    		
		    		int[] colorData = specularDebugFBO.readColorBufferARGB(0, debugPixelX, debugPixelY, 1, 1);
		    		int[] rDotVData = specularDebugFBO.readColorBufferARGB(1, debugPixelX, debugPixelY, 1, 1);
		    		debugInfo.println(	(rDotVData[0] & 0x000000FF) + "\t" +
										((colorData[0] & 0xFF000000) >>> 24) + "\t" + 
		    							((colorData[0] & 0x00FF0000) >>> 16) + "\t" + 
		    							((colorData[0] & 0x0000FF00) >>> 8) + "\t" +
		    							(colorData[0] & 0x000000FF));
		    	}
		    	
		    	debugInfo.flush();
		    	debugInfo.close();
		    	
		    	specularDebugFBO.delete();
		    	for (OpenGLResource r : specularDebugResources)
		    	{
		    		r.delete();
		    	}
		    	
				System.out.println("Specular debug info completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		        
		        imageTextures.delete();
		    	framebuffer.delete();
		        lightField.deleteOpenGLResources();
    		}
    		worldToTextureProgram.delete();
    		modelFitProgram.delete();
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
        GLFWWindow.closeAllWindows();
        System.exit(0);
	}
}
