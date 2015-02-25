package tetzlaff.reflacq;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLFramebufferObject;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLResource;
import tetzlaff.gl.opengl.OpenGLTextureArray;
import tetzlaff.ulf.UnstructuredLightField;
import tetzlaff.window.Window;
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
    	int fittingIterations = 10;
    	
    	int debugPixelX = 512, debugPixelY = 1004;
    	
        try
        {
	    	OpenGLProgram worldToTextureProgram = new OpenGLProgram(new File("shaders", "texspace.vert"), new File("shaders", "projtex.frag"));
    		OpenGLProgram diffuseFitProgram = new OpenGLProgram(new File("shaders", "texspace.vert"), new File("shaders", "diffusefit.frag"));
    		OpenGLProgram specularFitProgram = new OpenGLProgram(new File("shaders", "texspace.vert"), new File("shaders", "specularfit.frag"));
    		OpenGLProgram specularDebugProgram = new OpenGLProgram(new File("shaders", "texspace.vert"), new File("shaders", "speculardebug.frag"));
    		
    		JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
    		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
    		fileChooser.setFileFilter(new FileNameExtensionFilter("View Set files (.vset)", "vset"));
    		
    		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
    		{
    	    	File vsetFile = fileChooser.getSelectedFile();
    	    	File lightFieldDirectory = vsetFile.getParentFile();
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
		    	
		    	new File(lightFieldDirectory, "output/debug/diffuse/projpos").mkdirs();
		    	
		    	for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
		    	{
		    		worldToTextureFBO.setColorAttachment(0, imageTextures.getLayerAsFramebufferAttachment(i));
		    		worldToTextureFBO.setColorAttachment(1, depthTextures.getLayerAsFramebufferAttachment(i));
		    		worldToTextureRenderable.program().setUniform("cameraPoseIndex", i);
		    		
		    		worldToTextureFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    		worldToTextureFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    		worldToTextureFBO.clearDepthBuffer();
		    		worldToTextureRenderable.draw(PrimitiveMode.TRIANGLES, worldToTextureFBO);
		    		
		    		//worldToTextureFBO.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, String.format("output/debug/diffuse/%04d.png", i)));
		    		//worldToTextureFBO.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, String.format("output/debug/diffuse/projpos/%04d.png", i)));
		    	}		    	
		    	
		    	worldToTextureFBO.delete();
		    	for (OpenGLResource r : worldToTextureVBOResources)
		    	{
		    		r.delete();
		    	}
		    	
				System.out.println("Projections completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	
		    	System.out.println("Fitting to model...");
		    	timestamp = new Date();
		    	
		    	OpenGLRenderable diffuseFitRenderable = new OpenGLRenderable(diffuseFitProgram);
		    	
		    	diffuseFitRenderable.addVertexMesh("position", "texCoord", null, lightField.proxy);
		    	diffuseFitRenderable.program().setTexture("imageTextures", imageTextures);
		    	diffuseFitRenderable.program().setTexture("depthTextures", depthTextures);
		    	diffuseFitRenderable.program().setUniform("textureCount", lightField.viewSet.getCameraPoseCount());
		    	diffuseFitRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	diffuseFitRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
		    	diffuseFitRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
		    	diffuseFitRenderable.program().setUniform("gamma", gamma);
		    	diffuseFitRenderable.program().setUniform("guessSpecularColor", guessSpecularColor);
		    	diffuseFitRenderable.program().setUniform("guessSpecularWeight", guessSpecularWeight);
		    	
		    	OpenGLRenderable specularFitRenderable = new OpenGLRenderable(specularFitProgram);
		    	
		    	specularFitRenderable.addVertexMesh("position", "texCoord", null, lightField.proxy);
		    	specularFitRenderable.program().setTexture("imageTextures", imageTextures);
		    	specularFitRenderable.program().setTexture("depthTextures", depthTextures);
		    	specularFitRenderable.program().setUniform("textureCount", lightField.viewSet.getCameraPoseCount());
		    	specularFitRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	specularFitRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
		    	specularFitRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
		    	specularFitRenderable.program().setUniform("gamma", gamma);
		    	specularFitRenderable.program().setUniform("guessSpecularRoughness", guessSpecularRoughness);
		    	specularFitRenderable.program().setUniform("specularRange", specularRange);
		    	specularFitRenderable.program().setUniform("expectedWeightSum", expectedWeightSum);

		    	OpenGLFramebufferObject diffuseFitBackFramebuffer = new OpenGLFramebufferObject(textureSize, textureSize, 8, false);
		    	OpenGLFramebufferObject diffuseFitFrontFramebuffer = new OpenGLFramebufferObject(textureSize, textureSize, 8, false);
		    	OpenGLFramebufferObject specularFitFramebuffer = new OpenGLFramebufferObject(textureSize, textureSize, 8, false);

		    	diffuseFitFrontFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(5, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(6, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(7, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearDepthBuffer();
		    	
		        for (int i = 0; i < fittingIterations; i++)
		        {
		        	System.out.println("Starting iteration " + i);
			    	diffuseFitRenderable.program().setUniform("specularRemovalFactor", (float)i / (float)(fittingIterations - 1));
			    	diffuseFitRenderable.program().setTexture("diffuseEstimate", diffuseFitFrontFramebuffer.getColorAttachmentTexture(0));
			    	diffuseFitRenderable.program().setTexture("normalEstimate", diffuseFitFrontFramebuffer.getColorAttachmentTexture(1));
		        	diffuseFitRenderable.program().setTexture("specularColorEstimate", specularFitFramebuffer.getColorAttachmentTexture(0));
		        	diffuseFitRenderable.program().setTexture("roughnessEstimate", specularFitFramebuffer.getColorAttachmentTexture(1));
			    	
		        	diffuseFitBackFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
			    	diffuseFitBackFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
			    	diffuseFitBackFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
			    	diffuseFitBackFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
			    	diffuseFitBackFramebuffer.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);
			    	diffuseFitBackFramebuffer.clearColorBuffer(5, 0.0f, 0.0f, 0.0f, 0.0f);
			    	diffuseFitBackFramebuffer.clearColorBuffer(6, 0.0f, 0.0f, 0.0f, 0.0f);
			    	diffuseFitBackFramebuffer.clearColorBuffer(7, 0.0f, 0.0f, 0.0f, 0.0f);
			    	diffuseFitBackFramebuffer.clearDepthBuffer();
			    	
			        diffuseFitRenderable.draw(PrimitiveMode.TRIANGLES, diffuseFitBackFramebuffer);

			    	specularFitRenderable.program().setUniform("diffuseRemovalFactor", (float)i / (float)(fittingIterations - 1));
			    	specularFitRenderable.program().setTexture("diffuseEstimate", diffuseFitBackFramebuffer.getColorAttachmentTexture(0));
			    	specularFitRenderable.program().setTexture("normalEstimate", diffuseFitBackFramebuffer.getColorAttachmentTexture(1));
			        
			        specularFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
			    	specularFitFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
			    	specularFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
			    	specularFitFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
			    	specularFitFramebuffer.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);
			    	specularFitFramebuffer.clearColorBuffer(5, 0.0f, 0.0f, 0.0f, 0.0f);
			    	specularFitFramebuffer.clearColorBuffer(6, 0.0f, 0.0f, 0.0f, 0.0f);
			    	specularFitFramebuffer.clearColorBuffer(7, 0.0f, 0.0f, 0.0f, 0.0f);
			    	specularFitFramebuffer.clearDepthBuffer();

			        specularFitRenderable.draw(PrimitiveMode.TRIANGLES, specularFitFramebuffer);
			        
			        // Swap diffuse fit buffers
			        OpenGLFramebufferObject tmp = diffuseFitBackFramebuffer;
			        diffuseFitBackFramebuffer = diffuseFitFrontFramebuffer;
			        diffuseFitFrontFramebuffer = tmp;
			        
			    	new File(lightFieldDirectory, String.format("output/debug/stages/%04d", i)).mkdirs();
			        
			        diffuseFitFrontFramebuffer.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuse.png", i)));
			        diffuseFitFrontFramebuffer.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/normal.png", i)));
			        diffuseFitFrontFramebuffer.saveColorBufferToFile(2, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuseDebug0.png", i)));
			        //diffuseFitFrontFramebuffer.saveColorBufferToFile(3, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuseDebug1.png", i)));
			        //diffuseFitFrontFramebuffer.saveColorBufferToFile(4, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuseDebug2.png", i)));
			        //diffuseFitFrontFramebuffer.saveColorBufferToFile(5, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuseDebug3.png", i)));
			        //diffuseFitFrontFramebuffer.saveColorBufferToFile(6, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuseDebug4.png", i)));
			        //diffuseFitFrontFramebuffer.saveColorBufferToFile(7, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuseDebug5.png", i)));

			        specularFitFramebuffer.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specular.png", i)));
			        specularFitFramebuffer.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/roughness.png", i)));
			        specularFitFramebuffer.saveColorBufferToFile(2, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specularDebug0.png", i)));
			        specularFitFramebuffer.saveColorBufferToFile(3, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specularDebug1.png", i)));
			        specularFitFramebuffer.saveColorBufferToFile(4, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specularDebug2.png", i)));
			        specularFitFramebuffer.saveColorBufferToFile(5, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specularDebug3.png", i)));
//			        specularFitFramebuffer.saveColorBufferToFile(6, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specularDebug4.png", i)));
//			        specularFitFramebuffer.saveColorBufferToFile(7, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specularDebug5.png", i)));
			        
			        ulfToTexContext.swapBuffers(); // Signal the OS that we're still here (prevents TDR errors)
		        }

		    	System.out.println("Model fitting completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	
		    	System.out.println("Saving textures...");
		    	timestamp = new Date();
		    	
		    	new File(lightFieldDirectory, "output/textures").mkdirs();
		        
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, "output/textures/diffuse.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, "output/textures/normal.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(2, "PNG", new File(lightFieldDirectory, "output/debug/diffuse0.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(3, "PNG", new File(lightFieldDirectory, "output/debug/diffuse1.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(4, "PNG", new File(lightFieldDirectory, "output/debug/diffuse2.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(5, "PNG", new File(lightFieldDirectory, "output/debug/diffuse3.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(6, "PNG", new File(lightFieldDirectory, "output/debug/diffuse4.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(7, "PNG", new File(lightFieldDirectory, "output/debug/diffuse5.png"));

		        specularFitFramebuffer.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, "output/textures/specular.png"));
		        specularFitFramebuffer.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, "output/textures/roughness.png"));
		        specularFitFramebuffer.saveColorBufferToFile(2, "PNG", new File(lightFieldDirectory, "output/debug/specular0.png"));
		        specularFitFramebuffer.saveColorBufferToFile(3, "PNG", new File(lightFieldDirectory, "output/debug/specular1.png"));
		        specularFitFramebuffer.saveColorBufferToFile(4, "PNG", new File(lightFieldDirectory, "output/debug/specular2.png"));
		        specularFitFramebuffer.saveColorBufferToFile(5, "PNG", new File(lightFieldDirectory, "output/debug/specular3.png"));
		        specularFitFramebuffer.saveColorBufferToFile(6, "PNG", new File(lightFieldDirectory, "output/debug/specular4.png"));
		        specularFitFramebuffer.saveColorBufferToFile(7, "PNG", new File(lightFieldDirectory, "output/debug/specular5.png"));
		        
//		        System.out.println();
//		        int[] debug1Data = diffuseFitBackFramebuffer.readColorBufferARGB(5, debugPixelX, debugPixelY, 1, 1);
//		        double a = ((debug1Data[0] & 0x00FF0000) >>> 16) / 255.0 * 4.0;
//	    		double b = ((debug1Data[0] & 0x0000FF00) >>> 8) / 255.0 * 8.0 - 6.0;
//		        System.out.println(a);
//		        System.out.println(b);
//		        System.out.println(Math.exp(b));
//		        System.out.println(1.0 / Math.sqrt(2.0 * a));
//		        System.out.println();
//		        
//	    		int[] debug2Data = diffuseFitBackFramebuffer.readColorBufferARGB(6, debugPixelX, debugPixelY, 1, 1);
//	    		System.out.println(-((debug2Data[0] & 0x00FF0000) >>> 16) / 255.0);
//	    		System.out.println(((debug2Data[0] & 0x0000FF00) >>> 8) / 255.0);
//	    		System.out.println((debug2Data[0] & 0x000000FF) / 255.0);
//		        System.out.println();
//		        
//	    		int[] debug3Data = diffuseFitBackFramebuffer.readColorBufferARGB(7, debugPixelX, debugPixelY, 1, 1);
//	    		System.out.println(-((debug3Data[0] & 0x00FF0000) >>> 16) / 255.0);
//	    		System.out.println(((debug3Data[0] & 0x0000FF00) >>> 8) / 255.0);
//	    		System.out.println();
	    		
		    	System.out.println("Textures saved in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
//		    	
//		    	System.out.println("Generating specular debug info...");
//		    	timestamp = new Date();
//	    		
//		    	OpenGLFramebufferObject specularDebugFBO = new OpenGLFramebufferObject(textureSize, textureSize, 2, false);
//		    	OpenGLRenderable specularDebugRenderable = new OpenGLRenderable(specularDebugProgram);
		    	
//		    	Iterable<OpenGLResource> specularDebugResources = specularDebugRenderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
//		    	specularDebugRenderable.program().setTexture("textures", imageTextures);
//		    	specularDebugRenderable.program().setTexture("diffuse", diffuseFitFramebuffer.getColorAttachmentTexture(0));
//		    	specularDebugRenderable.program().setTexture("normalMap", diffuseFitFramebuffer.getColorAttachmentTexture(1));
//		    	specularDebugRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
//		    	specularDebugRenderable.program().setUniform("gamma", gamma);
//		    	specularDebugRenderable.program().setUniform("diffuseRemovalFactor", diffuseRemovalFactor);
//		    	
//		    	new File(lightFieldDirectory, "output/debug/specular/rDotV").mkdirs();
		    	
		    	//ulfToTexContext.setAlphaBlendingFunction(new AlphaBlendingFunction(
		    	//		AlphaBlendingFunction.Weight.SRC_ALPHA, 
		    	//		AlphaBlendingFunction.Weight.ONE_MINUS_SRC_ALPHA));
		    	
//		    	PrintStream debugInfo = new PrintStream(new File(lightFieldDirectory, "output/debug/debugInfo.txt"));
//		    	
//		    	for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
//		    	{
//		    		specularDebugRenderable.program().setUniform("textureIndex", i);
//		    		
//		    		specularDebugFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
//		    		specularDebugFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
//		    		specularDebugFBO.clearDepthBuffer();
//		    		specularDebugRenderable.draw(PrimitiveMode.TRIANGLES, specularDebugFBO);
		    		
		    		//specularDebugFBO.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory + String.format("output/debug/specular/%04d.png", i)));
		    		//specularDebugFBO.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory + String.format("output/debug/specular/rDotV/%04d.png", i)));
		    		
//		    		int[] colorData = specularDebugFBO.readColorBufferARGB(0, debugPixelX, debugPixelY, 1, 1);
//		    		int[] rDotVData = specularDebugFBO.readColorBufferARGB(1, debugPixelX, debugPixelY, 1, 1);
//		    		debugInfo.println(	(rDotVData[0] & 0x000000FF) + "\t" +
//										((colorData[0] & 0xFF000000) >>> 24) + "\t" + 
//		    							((colorData[0] & 0x00FF0000) >>> 16) + "\t" + 
//		    							((colorData[0] & 0x0000FF00) >>> 8) + "\t" +
//		    							(colorData[0] & 0x000000FF));
//		    	}
		    	
//		    	debugInfo.flush();
//		    	debugInfo.close();
//		    	
//		    	specularDebugFBO.delete();
//		    	for (OpenGLResource r : specularDebugResources)
//		    	{
//		    		r.delete();
//		    	}
//		    	
//				System.out.println("Specular debug info completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		        
		        imageTextures.delete();
		    	diffuseFitBackFramebuffer.delete();
		    	specularFitFramebuffer.delete();
		        lightField.deleteOpenGLResources();
    		}
    		worldToTextureProgram.delete();
    		diffuseFitProgram.delete();
    		specularFitProgram.delete();
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
        GLFWWindow.closeAllWindows();
        System.exit(0);
	}
}
