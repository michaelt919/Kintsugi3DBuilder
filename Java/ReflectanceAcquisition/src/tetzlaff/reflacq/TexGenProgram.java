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
import tetzlaff.gl.helpers.Vector2;
import tetzlaff.gl.helpers.Vector3;
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

public class TexGenProgram
{
	private static final boolean DEBUG = false;
	
	private static final float GAMMA = 2.2f;
	private static final boolean OCCLUSION_ENABLED = true;
	private static final float OCCLUSION_BIAS = 0.0025f;
	
	private static final int TEXTURE_SIZE = 512;
	private static final int TEXTURE_SUBDIV = 2;
	
	private static final float DIFFUSE_DELTA = 0.1f;
	private static final int DIFFUSE_ITERATIONS = 4;
	private static final float DIFFUSE_DETERMINANT_THRESHOLD = 1.0f;
	private static final float DIFFUSE_FIT3_WEIGHT = 1.0f;
	private static final float DIFFUSE_FIT1_WEIGHT = Float.MAX_VALUE;
	
	private static final boolean COMPUTE_ROUGHNESS = false;
	private static final boolean COMPUTE_SPECULAR_NORMAL = false;
	private static final boolean TRUE_BLINN_PHONG = true;
	private static final boolean PREPROJECT_IMAGES = true;
	
	private static final float DIFFUSE_REMOVAL_AMOUNT = 0.98f;
	private static final float SPECULAR_INFLUENCE_SCALE = 0.35f;
	private static final float SPECULAR_DETERMINANT_THRESHOLD = 0.002f;
	private static final float SPECULAR_FIT4_WEIGHT = 0.0f;
	private static final float SPECULAR_FIT2_WEIGHT = 0.0f;
	private static final float SPECULAR_FIT1_WEIGHT = 1.0f;
	private static final float SPECULAR_ROUGHNESS = 0.1f;
	private static final float SPECULAR_ROUGHNESS_SCALE = 0.5f;
	
	private static final int DEBUG_PIXEL_X = 322;
	private static final int DEBUG_PIXEL_Y = TEXTURE_SIZE - 365;

	@SuppressWarnings("unused")
	public static void main(String[] args)
    {
		OpenGLContext context = new GLFWWindow(800, 800, "Texture Generation");
    	context.enableDepthTest();
    	context.enableBackFaceCulling();

    	System.out.println("Max vertex uniform components across all blocks:" + context.getMaxCombinedVertexUniformComponents());
    	System.out.println("Max fragment uniform components across all blocks:" + context.getMaxCombinedFragmentUniformComponents());
    	System.out.println("Max size of a uniform block in bytes:" + context.getMaxUniformBlockSize());
    	System.out.println("Max texture array layers:" + context.getMaxArrayTextureLayers());
    	
        try
        {
	    	OpenGLProgram depthRenderingProgram = new OpenGLProgram(new File("shaders/depth.vert"), new File("shaders/depth.frag"));
        	
	    	OpenGLProgram projTexProgram = new OpenGLProgram(
	    			new File("shaders", "texspace.vert"), 
	    			new File("shaders", "projtex_single.frag"));
	    	
    		OpenGLProgram diffuseFitProgram = new OpenGLProgram(
    				new File("shaders", "texspace.vert"), 
    				new File("shaders", PREPROJECT_IMAGES || TEXTURE_SUBDIV > 1 ? "diffusefit_texspace.frag" : "diffusefit_imgspace.frag"));
    		
    		OpenGLProgram specularFitProgram = new OpenGLProgram(
    				new File("shaders", "texspace.vert"), 
    				new File("shaders", PREPROJECT_IMAGES || TEXTURE_SUBDIV > 1 ? "specularfit_texspace.frag" : "specularfit_imgspace.frag"));
    		
	    	OpenGLProgram diffuseDebugProgram = new OpenGLProgram(
	    			new File("shaders", "texspace.vert"), 
	    			new File("shaders", "projtex_multi.frag"));
    		
    		OpenGLProgram specularDebugProgram = new OpenGLProgram(
    				new File("shaders", "texspace.vert"), 
    				new File("shaders", "speculardebug_imgspace.frag"));
    		
    		JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
    		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
    		fileChooser.setFileFilter(new FileNameExtensionFilter("View Set files (.vset)", "vset"));
    		
    		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
    		{
    			System.out.println("Loading view set...");
    	    	Date timestamp = new Date();
    			
    	    	File vsetFile = fileChooser.getSelectedFile();
    	    	File lightFieldDirectory = vsetFile.getParentFile();
    	    	UnstructuredLightField lightField = UnstructuredLightField.loadFromVSETFile(vsetFile, !PREPROJECT_IMAGES && TEXTURE_SUBDIV == 1);
    	    	
    	    	File outputDirectory = new File(lightFieldDirectory, "output");
    	    	outputDirectory.mkdir();
    	    	if (DEBUG)
    	    	{
    	    		new File(outputDirectory, "debug").mkdir();
    	    	}
    	    	
		    	System.out.println("Loading completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	
		    	File tmpDir = new File(outputDirectory, "tmp");
		    	
		    	if (PREPROJECT_IMAGES || TEXTURE_SUBDIV > 1)
		    	{
		    		System.out.println("Pre-projecting images into texture space...");
			    	timestamp = new Date();
			    	
			    	OpenGLFramebufferObject projTexFBO = new OpenGLFramebufferObject(TEXTURE_SIZE / TEXTURE_SUBDIV, TEXTURE_SIZE / TEXTURE_SUBDIV, 2, true, false);
			    	OpenGLRenderable projTexRenderable = new OpenGLRenderable(projTexProgram);
			    	
			    	projTexRenderable.addVertexBuffer("position", lightField.positionBuffer);
			    	projTexRenderable.addVertexBuffer("texCoord", lightField.texCoordBuffer);
			    	projTexRenderable.addVertexBuffer("normal", lightField.normalBuffer);
			    	
			    	projTexRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
			    	projTexRenderable.program().setTexture("depthImages", lightField.depthTextures);
			    	projTexRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
			    	projTexRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
			    	projTexRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
			    	projTexRenderable.program().setUniform("occlusionEnabled", OCCLUSION_ENABLED);
			    	projTexRenderable.program().setUniform("occlusionBias", OCCLUSION_BIAS);
			    	
			    	PrintStream diffuseDebugInfo = null;
			    	
			    	tmpDir.mkdir();
			    	
			    	for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
			    	{
				    	File viewDir = new File(tmpDir, String.format("%04d", i));
				    	viewDir.mkdir();
				    	
				    	OpenGLTexture2D viewTexture = new OpenGLTexture2D(lightField.viewSet.getImageFile(i), true, true, true);
				    	
				    	OpenGLFramebufferObject depthFBO =  new OpenGLFramebufferObject(viewTexture.getWidth(), viewTexture.getHeight(), 0, false, true);
				    	OpenGLRenderable depthRenderable = new OpenGLRenderable(depthRenderingProgram);
				    	depthRenderable.addVertexBuffer("position", lightField.positionBuffer);
				    	
			        	depthRenderingProgram.setUniform("model_view", lightField.viewSet.getCameraPose(i));
			    		depthRenderingProgram.setUniform("projection", 
		    				lightField.viewSet.getCameraProjection(lightField.viewSet.getCameraProjectionIndex(i))
			    				.getProjectionMatrix(
		    						lightField.viewSet.getRecommendedNearPlane(), 
		    						lightField.viewSet.getRecommendedFarPlane()
								)
						);
			        	
			    		depthFBO.clearDepthBuffer();
			        	depthRenderable.draw(PrimitiveMode.TRIANGLES, depthFBO);
			        	
			        	projTexRenderable.program().setUniform("cameraPose", lightField.viewSet.getCameraPose(i));
			        	projTexRenderable.program().setUniform("cameraProjection", 
			        			lightField.viewSet.getCameraProjection(lightField.viewSet.getCameraProjectionIndex(i))
			        				.getProjectionMatrix(lightField.viewSet.getRecommendedNearPlane(), lightField.viewSet.getRecommendedFarPlane()));
				    	
				    	projTexRenderable.program().setTexture("viewImage", viewTexture);
				    	projTexRenderable.program().setTexture("depthImage", depthFBO.getDepthAttachmentTexture());
			    	
				    	for (int row = 0; row < TEXTURE_SUBDIV; row++)
				    	{
					    	for (int col = 0; col < TEXTURE_SUBDIV; col++)
				    		{
					    		projTexRenderable.program().setUniform("minTexCoord", 
					    				new Vector2((float)col / (float)TEXTURE_SUBDIV, (float)row / (float)TEXTURE_SUBDIV));
					    		
					    		projTexRenderable.program().setUniform("maxTexCoord", 
					    				new Vector2((float)(col+1) / (float)TEXTURE_SUBDIV, (float)(row+1) / (float)TEXTURE_SUBDIV));
					    		
					    		projTexFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
					    		projTexFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
					    		projTexFBO.clearDepthBuffer();
					    		projTexRenderable.draw(PrimitiveMode.TRIANGLES, projTexFBO);
					    		
					    		projTexFBO.saveColorBufferToFile(0, "PNG", new File(viewDir, String.format("r%04dc%04d.png", row, col)));
					    	}
			    		}
				    	
				    	viewTexture.delete();
			        	depthFBO.delete();
				    	
				    	System.out.println("Completed " + (i+1) + "/" + lightField.viewSet.getCameraPoseCount());
			    	}
			    	
			    	System.out.println("Pre-projections completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	}
		    	
		    	OpenGLFramebufferObject diffuseFitFramebuffer = new OpenGLFramebufferObject(TEXTURE_SIZE, TEXTURE_SIZE, 4, true, false);
		    	OpenGLFramebufferObject specularFitFramebuffer = new OpenGLFramebufferObject(TEXTURE_SIZE, TEXTURE_SIZE, 4, true, false);
		    	
		    	diffuseFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
		    	
		    	specularFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		        specularFitFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		        specularFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		        specularFitFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);

		    	OpenGLRenderable diffuseFitRenderable = new OpenGLRenderable(diffuseFitProgram);
		    	
		    	diffuseFitRenderable.addVertexBuffer("position", lightField.positionBuffer);
		    	diffuseFitRenderable.addVertexBuffer("texCoord", lightField.texCoordBuffer);
		    	diffuseFitRenderable.addVertexBuffer("normal", lightField.normalBuffer);
		    	
		    	diffuseFitRenderable.program().setUniform("viewCount", lightField.viewSet.getCameraPoseCount());
		    	diffuseFitRenderable.program().setUniform("gamma", GAMMA);
		    	diffuseFitRenderable.program().setUniform("occlusionEnabled", OCCLUSION_ENABLED);
		    	diffuseFitRenderable.program().setUniform("occlusionBias", OCCLUSION_BIAS);
		    	
		    	diffuseFitRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	
		    	if (!PREPROJECT_IMAGES && TEXTURE_SUBDIV == 1)
		    	{
			    	diffuseFitRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
			    	diffuseFitRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
		    	}
		    	
		    	diffuseFitRenderable.program().setUniform("delta", DIFFUSE_DELTA);
		    	diffuseFitRenderable.program().setUniform("iterations", DIFFUSE_ITERATIONS);
		    	diffuseFitRenderable.program().setUniform("determinantThreshold", DIFFUSE_DETERMINANT_THRESHOLD);
		    	diffuseFitRenderable.program().setUniform("fit1Weight", DIFFUSE_FIT1_WEIGHT);
		    	diffuseFitRenderable.program().setUniform("fit3Weight", DIFFUSE_FIT3_WEIGHT);
		    	
		    	if (lightField.viewSet.getLightPositionBuffer() != null && lightField.viewSet.getLightIndexBuffer() != null)
	    		{
		    		diffuseFitRenderable.program().setUniformBuffer("LightPositions", lightField.viewSet.getLightPositionBuffer());
		    		diffuseFitRenderable.program().setUniformBuffer("LightIndices", lightField.viewSet.getLightIndexBuffer());
	    		}
		    	
		    	OpenGLRenderable specularFitRenderable = new OpenGLRenderable(specularFitProgram);
		    	
		    	specularFitRenderable.addVertexBuffer("position", lightField.positionBuffer);
		    	specularFitRenderable.addVertexBuffer("texCoord", lightField.texCoordBuffer);
		    	specularFitRenderable.addVertexBuffer("normal", lightField.normalBuffer);

		    	specularFitRenderable.program().setUniform("viewCount", lightField.viewSet.getCameraPoseCount());
		    	specularFitRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	
		    	if (!PREPROJECT_IMAGES && TEXTURE_SUBDIV == 1)
		    	{
			    	specularFitRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
			    	specularFitRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
		    	}

		    	specularFitRenderable.program().setUniform("occlusionEnabled", OCCLUSION_ENABLED);
		    	specularFitRenderable.program().setUniform("occlusionBias", OCCLUSION_BIAS);
		    	specularFitRenderable.program().setUniform("gamma", GAMMA);
		    	
		    	specularFitRenderable.program().setUniform("computeRoughness", COMPUTE_ROUGHNESS);
		    	specularFitRenderable.program().setUniform("computeNormal", COMPUTE_SPECULAR_NORMAL);
		    	specularFitRenderable.program().setUniform("trueBlinnPhong", TRUE_BLINN_PHONG);

		    	specularFitRenderable.program().setUniform("diffuseRemovalAmount", DIFFUSE_REMOVAL_AMOUNT);
		    	specularFitRenderable.program().setUniform("specularInfluenceScale", SPECULAR_INFLUENCE_SCALE);
		    	specularFitRenderable.program().setUniform("determinantThreshold", SPECULAR_DETERMINANT_THRESHOLD);
		    	specularFitRenderable.program().setUniform("fit1Weight", SPECULAR_FIT1_WEIGHT);
		    	specularFitRenderable.program().setUniform("fit2Weight", SPECULAR_FIT2_WEIGHT);
		    	specularFitRenderable.program().setUniform("fit4Weight", SPECULAR_FIT4_WEIGHT);
		    	specularFitRenderable.program().setUniform("defaultSpecularColor", new Vector3(0.0f, 0.0f, 0.0f));
		    	specularFitRenderable.program().setUniform("specularRoughnessScale", SPECULAR_ROUGHNESS_SCALE);
		    	
		    	if (PREPROJECT_IMAGES || TEXTURE_SUBDIV > 1)
		    	{
		    		System.out.println("Beginning model fitting (" + (TEXTURE_SUBDIV * TEXTURE_SUBDIV) + " blocks)...");
			    	timestamp = new Date();
		    	}
		    	
		    	int subdivSize = TEXTURE_SIZE / TEXTURE_SUBDIV;
		    	
		    	for (int row = 0; row < TEXTURE_SUBDIV; row++)
		    	{
			    	for (int col = 0; col < TEXTURE_SUBDIV; col++)
		    		{
				    	if (!PREPROJECT_IMAGES && TEXTURE_SUBDIV == 1)
			    		{
					    	System.out.println("Fitting diffuse...");
					    	timestamp = new Date();
			    		}
				    	
				    	OpenGLTextureArray preprojectedViews = null;
				    	
				    	if (PREPROJECT_IMAGES || TEXTURE_SUBDIV > 1)
				    	{
				    		preprojectedViews = new OpenGLTextureArray(subdivSize, subdivSize, lightField.viewSet.getCameraPoseCount(), false, false, false);
					    	
							for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
							{
								preprojectedViews.loadLayer(i, new File(new File(tmpDir, String.format("%04d", i)), String.format("r%04dc%04d.png", row, col)), true);
							}
				    		
				    		diffuseFitRenderable.program().setTexture("viewImages", preprojectedViews);
				    	}
				    	else
				    	{
					    	diffuseFitRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
					    	diffuseFitRenderable.program().setTexture("depthImages", lightField.depthTextures);
				    	}
				    	
				    	diffuseFitRenderable.program().setUniform("minTexCoord", 
			    				new Vector2((float)col / (float)TEXTURE_SUBDIV, (float)row / (float)TEXTURE_SUBDIV));
			    		
				    	diffuseFitRenderable.program().setUniform("maxTexCoord", 
			    				new Vector2((float)(col+1) / (float)TEXTURE_SUBDIV, (float)(row+1) / (float)TEXTURE_SUBDIV));
				    	
				        diffuseFitRenderable.draw(PrimitiveMode.TRIANGLES, diffuseFitFramebuffer, col * subdivSize, row * subdivSize, subdivSize, subdivSize);
				        context.finish();
			    		
				        if (!PREPROJECT_IMAGES && TEXTURE_SUBDIV == 1)
				        {
				        	System.out.println("Diffuse fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
				        }
				    	
				    	if (!PREPROJECT_IMAGES && TEXTURE_SUBDIV == 1)
				        {
				        	System.out.println("Fitting specular...");
				        	timestamp = new Date();
				        }
				    	
				    	if (PREPROJECT_IMAGES || TEXTURE_SUBDIV > 1)
				    	{
				    		specularFitRenderable.program().setTexture("viewImages", preprojectedViews);
				    	}
				    	else
				    	{
					    	specularFitRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
					    	specularFitRenderable.program().setTexture("depthImages", lightField.depthTextures);
				    	}
				    	
				    	
				    	specularFitRenderable.program().setUniform("defaultSpecularRoughness", SPECULAR_ROUGHNESS);
				    	
				    	if (lightField.viewSet.getLightPositionBuffer() != null && lightField.viewSet.getLightIndexBuffer() != null)
			    		{
				    		specularFitRenderable.program().setUniformBuffer("LightPositions", lightField.viewSet.getLightPositionBuffer());
				    		specularFitRenderable.program().setUniformBuffer("LightIndices", lightField.viewSet.getLightIndexBuffer());
			    		}
				    	
				    	specularFitRenderable.program().setUniform("minTexCoord", 
			    				new Vector2((float)col / (float)TEXTURE_SUBDIV, (float)row / (float)TEXTURE_SUBDIV));
			    		
				    	specularFitRenderable.program().setUniform("maxTexCoord", 
			    				new Vector2((float)(col+1) / (float)TEXTURE_SUBDIV, (float)(row+1) / (float)TEXTURE_SUBDIV));
		
				    	specularFitRenderable.program().setTexture("diffuseEstimate", diffuseFitFramebuffer.getColorAttachmentTexture(0));
				    	specularFitRenderable.program().setTexture("normalEstimate", diffuseFitFramebuffer.getColorAttachmentTexture(1));
				        
			    		specularFitRenderable.draw(PrimitiveMode.TRIANGLES, specularFitFramebuffer, col * subdivSize, row * subdivSize, subdivSize, subdivSize);
			    		context.finish();
				    	
				    	if (PREPROJECT_IMAGES || TEXTURE_SUBDIV > 1)
				    	{
				    		preprojectedViews.delete();
				    	}
			    		
				    	if (PREPROJECT_IMAGES || TEXTURE_SUBDIV > 1)
			    		{
			    			System.out.println("Block " + (row*TEXTURE_SUBDIV + col + 1) + "/" + (TEXTURE_SUBDIV * TEXTURE_SUBDIV) + " completed.");
			    		}
				    	else
				    	{
					    	System.out.println("Specular fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
				        }
		    		}
		    	}
		    	
		    	if (PREPROJECT_IMAGES || TEXTURE_SUBDIV > 1)
		    	{
		    		System.out.println("Model fitting completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	}
		    	
		    	System.out.println("Saving textures...");
		    	timestamp = new Date();
		    	
		    	File textureDirectory = new File(new File(lightFieldDirectory, "output"), "textures");
		    	
		    	textureDirectory.mkdirs();
		        
		    	diffuseFitFramebuffer.saveColorBufferToFile(0, "PNG", new File(textureDirectory, "diffuse.png"));
		    	diffuseFitFramebuffer.saveColorBufferToFile(1, "PNG", new File(textureDirectory, "normal.png"));
		    	//diffuseFitFramebuffer.saveColorBufferToFile(2, "PNG", new File(textureDirectory, "ambient.png"));
		    	if (DEBUG)
		    	{
		    		diffuseFitFramebuffer.saveColorBufferToFile(3, "PNG", new File(textureDirectory, "ddebug.png"));
		    	}

		    	specularFitFramebuffer.saveColorBufferToFile(0, "PNG", new File(textureDirectory, "specular.png"));
		    	specularFitFramebuffer.saveColorBufferToFile(1, "PNG", new File(textureDirectory, "roughness.png"));
		    	specularFitFramebuffer.saveColorBufferToFile(2, "PNG", new File(textureDirectory, "snormal.png"));
		    	if (DEBUG)
		    	{
			    	specularFitFramebuffer.saveColorBufferToFile(3, "PNG", new File(textureDirectory, "textures/sdebug.png"));
		    	}
		    	
		    	diffuseFitFramebuffer.delete();
		    	specularFitFramebuffer.delete();

		    	System.out.println("Textures saved in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	
		    	if (DEBUG && !PREPROJECT_IMAGES && TEXTURE_SUBDIV == 1)
		    	{
		    		System.out.println("Generating diffuse debug info...");
			    	timestamp = new Date();
	
			    	new File(outputDirectory, "debug").mkdirs();
		    		
		    		OpenGLFramebufferObject diffuseDebugFBO = new OpenGLFramebufferObject(TEXTURE_SIZE, TEXTURE_SIZE, 2, true, false);
			    	OpenGLRenderable diffuseDebugRenderable = new OpenGLRenderable(diffuseDebugProgram);
			    	
			    	diffuseDebugRenderable.program().setUniform("minTexCoord", new Vector2(0.0f, 0.0f));
			    	diffuseDebugRenderable.program().setUniform("maxTexCoord", new Vector2(1.0f, 1.0f));
			    	
			    	diffuseDebugRenderable.addVertexBuffer("position", lightField.positionBuffer);
			    	diffuseDebugRenderable.addVertexBuffer("texCoord", lightField.texCoordBuffer);
			    	diffuseDebugRenderable.addVertexBuffer("normal", lightField.normalBuffer);

			    	diffuseDebugRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
			    	diffuseDebugRenderable.program().setTexture("depthImages", lightField.depthTextures);
			    	diffuseDebugRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
			    	diffuseDebugRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
			    	diffuseDebugRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
			    	diffuseDebugRenderable.program().setUniform("occlusionEnabled", OCCLUSION_ENABLED);
			    	diffuseDebugRenderable.program().setUniform("occlusionBias", OCCLUSION_BIAS);
			    	
			    	//new File(outputDirectory, "debug/diffuse/projpos").mkdirs();
			    	
			    	PrintStream diffuseInfo = new PrintStream(new File(outputDirectory, "debug/diffuseInfo.txt"));
			    	
			    	for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
			    	{
			    		diffuseDebugRenderable.program().setUniform("viewIndex", i);
			    		
			    		diffuseDebugFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
			    		diffuseDebugFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
			    		diffuseDebugFBO.clearDepthBuffer();
			    		diffuseDebugRenderable.draw(PrimitiveMode.TRIANGLES, diffuseDebugFBO);
			    		
			    		//diffuseDebugFBO.saveColorBufferToFile(0, "PNG", new File(outputDirectory, String.format("debug/diffuse/%04d.png", i)));
			    		//diffuseDebugFBO.saveColorBufferToFile(1, "PNG", new File(outputDirectory, String.format("debug/diffuse/projpos/%04d.png", i)));
			    		
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
			    	
					System.out.println("Diffuse debug info completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    		
			    	System.out.println("Generating specular debug info...");
			    	timestamp = new Date();
		    		
			    	OpenGLFramebufferObject specularDebugFBO = new OpenGLFramebufferObject(TEXTURE_SIZE, TEXTURE_SIZE, 2, true, false);
			    	OpenGLRenderable specularDebugRenderable = new OpenGLRenderable(specularDebugProgram);

			    	specularDebugRenderable.program().setUniform("minTexCoord", new Vector2(0.0f, 0.0f));
			    	specularDebugRenderable.program().setUniform("maxTexCoord", new Vector2(1.0f, 1.0f));
			    	
			    	specularDebugRenderable.addVertexBuffer("position", lightField.positionBuffer);
			    	specularDebugRenderable.addVertexBuffer("texCoord", lightField.texCoordBuffer);
			    	specularDebugRenderable.addVertexBuffer("normal", lightField.normalBuffer);

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
			    	
			    	//new File(outputDirectory, "debug/specular/rDotV").mkdirs();
			    	
			    	PrintStream specularInfo = new PrintStream(new File(outputDirectory, "debug/specularInfo.txt"));
			    	
			    	for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
			    	{
			    		specularDebugRenderable.program().setUniform("viewIndex", i);
			    		
			    		specularDebugFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
			    		specularDebugFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
			    		specularDebugFBO.clearDepthBuffer();
			    		specularDebugRenderable.draw(PrimitiveMode.TRIANGLES, specularDebugFBO);
			    		
			    		//specularDebugFBO.saveColorBufferToFile(0, "PNG", new File(outputDirectory, String.format("debug/specular/%04d.png", i)));
			    		//specularDebugFBO.saveColorBufferToFile(1, "PNG", new File(outputDirectory, String.format("debug/specular/rDotV/%04d.png", i)));
			    		
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
			    	
					System.out.println("Specular debug info completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	}
		    	
		        lightField.deleteOpenGLResources();
    		}
    		
    		projTexProgram.delete();
    		diffuseFitProgram.delete();
    		specularFitProgram.delete();
    		diffuseDebugProgram.delete();
    		specularDebugProgram.delete();
        	depthRenderingProgram.delete();
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
