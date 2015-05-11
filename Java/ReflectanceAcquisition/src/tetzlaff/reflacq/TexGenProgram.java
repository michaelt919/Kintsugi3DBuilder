package tetzlaff.reflacq;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLFramebufferObject;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLResource;
import tetzlaff.ulf.UnstructuredLightField;
import tetzlaff.window.glfw.GLFWWindow;

public class TexGenProgram
{
	private static final boolean DEBUG = false;
	
	private static final float GAMMA = 2.2f;
	private static final boolean OCCLUSION_ENABLED = true;
	private static final float OCCLUSION_BIAS = 0.0025f;
	
	private static final int TEXTURE_SIZE = 2048;
	
	private static final float DIFFUSE_DELTA = 0.1f;
	private static final int DIFFUSE_ITERATIONS = 8;
	private static final float DIFFUSE_DETERMINANT_THRESHOLD = 10.0f;
	private static final float DIFFUSE_FIT3_WEIGHT = 1.0f;
	private static final float DIFFUSE_FIT1_WEIGHT = 1.0f;
	
	private static final boolean COMPUTE_ROUGHNESS = true;
	private static final boolean COMPUTE_SPECULAR_NORMAL = true;
	
	private static final float DIFFUSE_REMOVAL_AMOUNT = 0.98f;
	private static final float SPECULAR_INFLUENCE_SCALE = 0.35f;
	private static final float SPECULAR_DETERMINANT_THRESHOLD = 0.02f;
	private static final float SPECULAR_FIT4_WEIGHT = 1.0f;
	private static final float SPECULAR_FIT2_WEIGHT = 0.0f;
	private static final float SPECULAR_FIT1_WEIGHT = 1.0f;
	private static final float DEFAULT_SPECULAR_ROUGHNESS = 0.25f;
	private static final float SPECULAR_ROUGHNESS_SCALE = 0.5f;
	
	private static final int DEBUG_PIXEL_X = 322;
	private static final int DEBUG_PIXEL_Y = TEXTURE_SIZE - 365;
	
	public static void main(String[] args)
    {
    	Date timestamp;
		
    	OpenGLContext ulfToTexContext = new GLFWWindow(800, 800, "Texture Generation");
    	ulfToTexContext.enableDepthTest();
    	ulfToTexContext.enableBackFaceCulling();
    	
        try
        {
	    	OpenGLProgram diffuseDebugProgram = new OpenGLProgram(new File("shaders", "texspace.vert"), new File("shaders", "projtex.frag"));
    		OpenGLProgram diffuseFitProgram = new OpenGLProgram(new File("shaders", "texspace.vert"), new File("shaders", "diffusefit.frag"));
    		OpenGLProgram specularFitProgram = new OpenGLProgram(new File("shaders", "texspace.vert"), new File("shaders", "specularfit.frag"));
    		OpenGLProgram specularDebugProgram = new OpenGLProgram(new File("shaders", "texspace.vert"), new File("shaders", "speculardebug.frag"));
    		OpenGLProgram holeFillProgram = new OpenGLProgram(new File("shaders", "passthrough2d.vert"), new File("shaders", "holefill.frag"));
    		
    		JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
    		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
    		fileChooser.setFileFilter(new FileNameExtensionFilter("View Set files (.vset)", "vset"));
    		
    		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
    		{
    	    	File vsetFile = fileChooser.getSelectedFile();
    	    	File lightFieldDirectory = vsetFile.getParentFile();
    	    	UnstructuredLightField lightField = UnstructuredLightField.loadFromVSETFile(vsetFile);
		    	
		    	System.out.println("Fitting to model...");
		    	timestamp = new Date();
		    	
		    	OpenGLRenderable diffuseFitRenderable = new OpenGLRenderable(diffuseFitProgram);
		    	
		    	diffuseFitRenderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
		    	
		    	diffuseFitRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
		    	diffuseFitRenderable.program().setTexture("depthImages", lightField.depthTextures);
		    	diffuseFitRenderable.program().setUniform("viewCount", lightField.viewSet.getCameraPoseCount());
		    	
		    	diffuseFitRenderable.program().setUniform("gamma", GAMMA);
		    	diffuseFitRenderable.program().setUniform("occlusionEnabled", OCCLUSION_ENABLED);
		    	diffuseFitRenderable.program().setUniform("occlusionBias", OCCLUSION_BIAS);
		    	
		    	diffuseFitRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	diffuseFitRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
		    	diffuseFitRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
		    	
		    	diffuseFitRenderable.program().setUniform("delta", DIFFUSE_DELTA);
		    	diffuseFitRenderable.program().setUniform("iterations", DIFFUSE_ITERATIONS);
		    	diffuseFitRenderable.program().setUniform("determinantThreshold", DIFFUSE_DETERMINANT_THRESHOLD);
		    	diffuseFitRenderable.program().setUniform("fit1Weight", DIFFUSE_FIT1_WEIGHT);
		    	diffuseFitRenderable.program().setUniform("fit3Weight", DIFFUSE_FIT3_WEIGHT);
		    	
		    	OpenGLRenderable specularFitRenderable = new OpenGLRenderable(specularFitProgram);
		    	
		    	specularFitRenderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
		    	
		    	specularFitRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
		    	specularFitRenderable.program().setTexture("depthImages", lightField.depthTextures);
		    	specularFitRenderable.program().setUniform("viewCount", lightField.viewSet.getCameraPoseCount());
		    	
		    	specularFitRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	specularFitRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
		    	specularFitRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());

		    	specularFitRenderable.program().setUniform("occlusionEnabled", OCCLUSION_ENABLED);
		    	specularFitRenderable.program().setUniform("occlusionBias", OCCLUSION_BIAS);
		    	specularFitRenderable.program().setUniform("gamma", GAMMA);
		    	
		    	specularFitRenderable.program().setUniform("computeRoughness", COMPUTE_ROUGHNESS);
		    	specularFitRenderable.program().setUniform("computeNormal", COMPUTE_SPECULAR_NORMAL);

		    	specularFitRenderable.program().setUniform("diffuseRemovalAmount", DIFFUSE_REMOVAL_AMOUNT);
		    	specularFitRenderable.program().setUniform("specularInfluenceScale", SPECULAR_INFLUENCE_SCALE);
		    	specularFitRenderable.program().setUniform("determinantThreshold", SPECULAR_DETERMINANT_THRESHOLD);
		    	specularFitRenderable.program().setUniform("fit1Weight", SPECULAR_FIT1_WEIGHT);
		    	specularFitRenderable.program().setUniform("fit2Weight", SPECULAR_FIT2_WEIGHT);
		    	specularFitRenderable.program().setUniform("fit4Weight", SPECULAR_FIT4_WEIGHT);
		    	specularFitRenderable.program().setUniform("defaultSpecularColor", new Vector3(0.0f, 0.0f, 0.0f));
		    	specularFitRenderable.program().setUniform("defaultSpecularRoughness", DEFAULT_SPECULAR_ROUGHNESS);
		    	specularFitRenderable.program().setUniform("specularRoughnessScale", SPECULAR_ROUGHNESS_SCALE);
		    	
		    	if (lightField.viewSet.getLightPositionBuffer() != null && lightField.viewSet.getLightIndexBuffer() != null)
	    		{
		    		diffuseFitRenderable.program().setUniformBuffer("LightPositions", lightField.viewSet.getLightPositionBuffer());
		    		diffuseFitRenderable.program().setUniformBuffer("LightIndices", lightField.viewSet.getLightIndexBuffer());
		    		specularFitRenderable.program().setUniformBuffer("LightPositions", lightField.viewSet.getLightPositionBuffer());
		    		specularFitRenderable.program().setUniformBuffer("LightIndices", lightField.viewSet.getLightIndexBuffer());
	    		}

		    	OpenGLFramebufferObject diffuseFitFramebuffer = new OpenGLFramebufferObject(TEXTURE_SIZE, TEXTURE_SIZE, 4, true, false);
		    	OpenGLFramebufferObject specularFitFramebuffer = new OpenGLFramebufferObject(TEXTURE_SIZE, TEXTURE_SIZE, 4, true, false);

		    	diffuseFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
		    	
		        diffuseFitRenderable.draw(PrimitiveMode.TRIANGLES, diffuseFitFramebuffer);
		        ulfToTexContext.finish();
		    	
		        specularFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		        specularFitFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		        specularFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		        specularFitFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);

		    	specularFitRenderable.program().setTexture("diffuseEstimate", diffuseFitFramebuffer.getColorAttachmentTexture(0));
		    	specularFitRenderable.program().setTexture("normalEstimate", diffuseFitFramebuffer.getColorAttachmentTexture(1));

	    		specularFitRenderable.draw(PrimitiveMode.TRIANGLES, specularFitFramebuffer);
	    		ulfToTexContext.finish();
	    		
		    	System.out.println("Model fitting completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	
		    	System.out.println("Saving textures...");
		    	timestamp = new Date();
		    	
		    	new File(lightFieldDirectory, "output/textures").mkdirs();
		        
		    	diffuseFitFramebuffer.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, "output/textures/diffuse.png"));
		    	diffuseFitFramebuffer.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, "output/textures/normal.png"));
		    	diffuseFitFramebuffer.saveColorBufferToFile(2, "PNG", new File(lightFieldDirectory, "output/textures/ambient.png"));
		    	//if (DEBUG)
		    	{
		    		diffuseFitFramebuffer.saveColorBufferToFile(3, "PNG", new File(lightFieldDirectory, "output/textures/ddebug.png"));
		    	}

		    	specularFitFramebuffer.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, "output/textures/specular.png"));
		    	specularFitFramebuffer.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, "output/textures/roughness.png"));
		    	specularFitFramebuffer.saveColorBufferToFile(2, "PNG", new File(lightFieldDirectory, "output/textures/snormal.png"));
		    	//if (DEBUG)
		    	{
			    	specularFitFramebuffer.saveColorBufferToFile(3, "PNG", new File(lightFieldDirectory, "output/textures/sdebug.png"));
		    	}

		    	System.out.println("Textures saved in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		        
		    	diffuseFitFramebuffer.delete();
		    	specularFitFramebuffer.delete();
		    	
		    	if (DEBUG)
		    	{
		    		System.out.println("Generating diffuse debug info...");
			    	timestamp = new Date();
	
			    	new File(lightFieldDirectory, "output/debug").mkdirs();
		    		
		    		OpenGLFramebufferObject diffuseDebugFBO = new OpenGLFramebufferObject(TEXTURE_SIZE, TEXTURE_SIZE, 2, true, false);
			    	OpenGLRenderable diffuseDebugRenderable = new OpenGLRenderable(diffuseDebugProgram);
			    	
			    	Iterable<OpenGLResource> worldToTextureVBOResources = diffuseDebugRenderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
			    	diffuseDebugRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
			    	diffuseDebugRenderable.program().setTexture("depthImages", lightField.depthTextures);
			    	diffuseDebugRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
			    	diffuseDebugRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
			    	diffuseDebugRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
			    	diffuseDebugRenderable.program().setUniform("occlusionEnabled", OCCLUSION_ENABLED);
			    	diffuseDebugRenderable.program().setUniform("occlusionBias", OCCLUSION_BIAS);
			    	
			    	//new File(lightFieldDirectory, "output/debug/diffuse/projpos").mkdirs();
			    	
			    	PrintStream diffuseInfo = new PrintStream(new File(lightFieldDirectory, "output/debug/diffuseInfo.txt"));
			    	
			    	for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
			    	{
			    		diffuseDebugRenderable.program().setUniform("viewIndex", i);
			    		
			    		diffuseDebugFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
			    		diffuseDebugFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
			    		diffuseDebugFBO.clearDepthBuffer();
			    		diffuseDebugRenderable.draw(PrimitiveMode.TRIANGLES, diffuseDebugFBO);
			    		
			    		//diffuseDebugFBO.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, String.format("output/debug/diffuse/%04d.png", i)));
			    		//diffuseDebugFBO.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, String.format("output/debug/diffuse/projpos/%04d.png", i)));
			    		
			    		Matrix4 cameraPose = lightField.viewSet.getCameraPose(i);
			    		Vector3 lightPosition = new Matrix3(cameraPose).transpose().times(
		    				lightField.viewSet.getLightPosition(lightField.viewSet.getLightPositionIndex(i))
		    					.minus(new Vector3(cameraPose.getColumn(3))));
			    		int[] colorData = diffuseDebugFBO.readColorBufferARGB(0, DEBUG_PIXEL_X, DEBUG_PIXEL_Y, 1, 1);
			    		float[] positionData = diffuseDebugFBO.readFloatingPointColorBufferRGBA(1, DEBUG_PIXEL_X, DEBUG_PIXEL_Y, 1, 1);
			    		diffuseInfo.println(
		    				lightPosition.x + "\t" +
							lightPosition.y + "\t" +
							lightPosition.z + "\t" +
		    				positionData[0] + "\t" +
		    				positionData[1] + "\t" +
		    				positionData[2] + "\t" +
							((colorData[0] & 0xFF000000) >>> 24) + "\t" + 
							((colorData[0] & 0x00FF0000) >>> 16) + "\t" + 
							((colorData[0] & 0x0000FF00) >>> 8) + "\t" +
							(colorData[0] & 0x000000FF));
			    	}	
			    	
			    	diffuseInfo.flush();
			    	diffuseInfo.close();	    	
			    	
			    	diffuseDebugFBO.delete();
			    	for (OpenGLResource r : worldToTextureVBOResources)
			    	{
			    		r.delete();
			    	}
			    	
					System.out.println("Diffuse debug info completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
			    	
			    	System.out.println("Generating specular debug info...");
			    	timestamp = new Date();
		    		
			    	OpenGLFramebufferObject specularDebugFBO = new OpenGLFramebufferObject(TEXTURE_SIZE, TEXTURE_SIZE, 2, true, false);
			    	OpenGLRenderable specularDebugRenderable = new OpenGLRenderable(specularDebugProgram);
			    	
			    	Iterable<OpenGLResource> specularDebugResources = specularDebugRenderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
			    	specularDebugRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
			    	specularDebugRenderable.program().setTexture("depthImages", lightField.depthTextures);
			    	specularDebugRenderable.program().setUniform("occlusionEnabled", OCCLUSION_ENABLED);
			    	specularDebugRenderable.program().setUniform("occlusionBias", OCCLUSION_BIAS);
			    	specularDebugRenderable.program().setTexture("diffuse", diffuseFitFramebuffer.getColorAttachmentTexture(0));
			    	specularDebugRenderable.program().setTexture("normalMap", diffuseFitFramebuffer.getColorAttachmentTexture(1));
			    	specularDebugRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
			    	specularDebugRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
			    	specularDebugRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
			    	specularDebugRenderable.program().setUniform("gamma", GAMMA);
			    	specularDebugRenderable.program().setUniform("diffuseRemovalFactor", 1.0f);
			    	
			    	if (lightField.viewSet.getLightPositionBuffer() != null && lightField.viewSet.getLightIndexBuffer() != null)
		    		{
			    		specularDebugRenderable.program().setUniformBuffer("LightPositions", lightField.viewSet.getLightPositionBuffer());
			    		specularDebugRenderable.program().setUniformBuffer("LightIndices", lightField.viewSet.getLightIndexBuffer());
		    		}
			    	
			    	//new File(lightFieldDirectory, "output/debug/specular/rDotV").mkdirs();
			    	
			    	PrintStream specularInfo = new PrintStream(new File(lightFieldDirectory, "output/debug/specularInfo.txt"));
			    	
			    	for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
			    	{
			    		specularDebugRenderable.program().setUniform("viewIndex", i);
			    		
			    		specularDebugFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
			    		specularDebugFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
			    		specularDebugFBO.clearDepthBuffer();
			    		specularDebugRenderable.draw(PrimitiveMode.TRIANGLES, specularDebugFBO);
			    		
			    		//specularDebugFBO.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, String.format("output/debug/specular/%04d.png", i)));
			    		//specularDebugFBO.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, String.format("output/debug/specular/rDotV/%04d.png", i)));
			    		
			    		int[] colorData = specularDebugFBO.readColorBufferARGB(0, DEBUG_PIXEL_X, DEBUG_PIXEL_Y, 1, 1);
			    		int[] rDotVData = specularDebugFBO.readColorBufferARGB(1, DEBUG_PIXEL_X, DEBUG_PIXEL_Y, 1, 1);
			    		specularInfo.println(	(rDotVData[0] & 0x000000FF) + "\t" +
											((colorData[0] & 0xFF000000) >>> 24) + "\t" + 
			    							((colorData[0] & 0x00FF0000) >>> 16) + "\t" + 
			    							((colorData[0] & 0x0000FF00) >>> 8) + "\t" +
			    							(colorData[0] & 0x000000FF));
			    	}
			    	
			    	specularInfo.flush();
			    	specularInfo.close();
			    	
			    	specularDebugFBO.delete();
			    	for (OpenGLResource r : specularDebugResources)
			    	{
			    		r.delete();
			    	}
			    	
					System.out.println("Specular debug info completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	}
				
		        lightField.deleteOpenGLResources();
    		}
    		
    		diffuseDebugProgram.delete();
    		diffuseFitProgram.delete();
    		specularFitProgram.delete();
    		specularDebugProgram.delete();
    		holeFillProgram.delete();
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
        GLFWWindow.closeAllWindows();
        System.out.println("Process terminated with no errors.");
        System.exit(0);
	}
}
