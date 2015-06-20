package tetzlaff.reflacq;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector2;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLFramebufferObject;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLTexture2D;
import tetzlaff.gl.opengl.OpenGLTextureArray;
import tetzlaff.ulf.UnstructuredLightField;

public class TexGenExecutor 
{
	// Debug parameters
	private static final boolean DEBUG = false;

	private OpenGLContext context;
	private File vsetFile;
	private TexGenParameters param;
	
	public TexGenExecutor(OpenGLContext context, File vsetFile, TexGenParameters param) 
	{
		this.context = context;
		this.vsetFile = vsetFile;
		this.param = param;
	}

	public void execute() throws IOException
	{
		final int DEBUG_PIXEL_X = 322;
		final int DEBUG_PIXEL_Y = param.getTextureSize() - 365;
		
		context.enableDepthTest();
    	context.enableBackFaceCulling();

    	System.out.println("Max vertex uniform components across all blocks:" + context.getMaxCombinedVertexUniformComponents());
    	System.out.println("Max fragment uniform components across all blocks:" + context.getMaxCombinedFragmentUniformComponents());
    	System.out.println("Max size of a uniform block in bytes:" + context.getMaxUniformBlockSize());
    	System.out.println("Max texture array layers:" + context.getMaxArrayTextureLayers());
    	
    	OpenGLProgram depthRenderingProgram = new OpenGLProgram(new File("shaders/depth.vert"), new File("shaders/depth.frag"));
    	
    	OpenGLProgram projTexProgram = new OpenGLProgram(
    			new File("shaders", "texspace.vert"), 
    			new File("shaders", "projtex_single.frag"));
    	
		OpenGLProgram diffuseFitProgram = new OpenGLProgram(
				new File("shaders", "texspace.vert"), 
				new File("shaders", param.isImagePreprojectionUseEnabled() || param.getTextureSubdivision() > 1 ? "diffusefit_texspace.frag" : "diffusefit_imgspace.frag"));
		
		OpenGLProgram specularFitProgram = new OpenGLProgram(
				new File("shaders", "texspace.vert"), 
				new File("shaders", param.isImagePreprojectionUseEnabled() || param.getTextureSubdivision() > 1 ? "specularfit_texspace.frag" : "specularfit_imgspace.frag"));
		
    	OpenGLProgram diffuseDebugProgram = new OpenGLProgram(
    			new File("shaders", "texspace.vert"), 
    			new File("shaders", "projtex_multi.frag"));
		
		OpenGLProgram specularDebugProgram = new OpenGLProgram(
				new File("shaders", "texspace.vert"), 
				new File("shaders", "speculardebug_imgspace.frag"));
		
		System.out.println("Loading view set...");
    	Date timestamp = new Date();
		
    	File lightFieldDirectory = vsetFile.getParentFile();
    	UnstructuredLightField lightField = UnstructuredLightField.loadFromVSETFile(vsetFile, !param.isImagePreprojectionUseEnabled() && param.getTextureSubdivision() == 1);
    	
    	File outputDirectory = new File(lightFieldDirectory, "output");
    	outputDirectory.mkdir();
    	if (DEBUG)
    	{
    		new File(outputDirectory, "debug").mkdir();
    	}
    	
    	System.out.println("Loading completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	
    	File tmpDir = new File(outputDirectory, "tmp");
    	
    	if ((param.isImagePreprojectionUseEnabled() || param.getTextureSubdivision() > 1) && param.isImagePreprojectionGenerationEnabled())
    	{
    		System.out.println("Pre-projecting images into texture space...");
	    	timestamp = new Date();
	    	
	    	OpenGLFramebufferObject projTexFBO = new OpenGLFramebufferObject(param.getTextureSize() / param.getTextureSubdivision(), param.getTextureSize() / param.getTextureSubdivision(), 2, true, false);
	    	OpenGLRenderable projTexRenderable = new OpenGLRenderable(projTexProgram);
	    	
	    	projTexRenderable.addVertexBuffer("position", lightField.positionBuffer);
	    	projTexRenderable.addVertexBuffer("texCoord", lightField.texCoordBuffer);
	    	projTexRenderable.addVertexBuffer("normal", lightField.normalBuffer);
	    	
	    	projTexRenderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
	    	projTexRenderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
	    	
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
	    	
		    	for (int row = 0; row < param.getTextureSubdivision(); row++)
		    	{
			    	for (int col = 0; col < param.getTextureSubdivision(); col++)
		    		{
			    		projTexRenderable.program().setUniform("minTexCoord", 
			    				new Vector2((float)col / (float)param.getTextureSubdivision(), (float)row / (float)param.getTextureSubdivision()));
			    		
			    		projTexRenderable.program().setUniform("maxTexCoord", 
			    				new Vector2((float)(col+1) / (float)param.getTextureSubdivision(), (float)(row+1) / (float)param.getTextureSubdivision()));
			    		
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
    	
    	if (param.isImagePreprojectionUseEnabled() || param.getTextureSubdivision() > 1)
    	{
    		System.out.println("Beginning model fitting (" + (param.getTextureSubdivision() * param.getTextureSubdivision()) + " blocks)...");
	    	timestamp = new Date();
    	}
    	
    	OpenGLFramebufferObject diffuseFitFramebuffer = new OpenGLFramebufferObject(param.getTextureSize(), param.getTextureSize(), 4, false, false);
    	OpenGLFramebufferObject specularFitFramebuffer = new OpenGLFramebufferObject(param.getTextureSize(), param.getTextureSize(), 4, false, false);
    	
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
    	diffuseFitRenderable.program().setUniform("gamma", param.getGamma());
    	diffuseFitRenderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
    	diffuseFitRenderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
    	
    	diffuseFitRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
    	
    	if (!param.isImagePreprojectionUseEnabled() && param.getTextureSubdivision() == 1)
    	{
	    	diffuseFitRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
	    	diffuseFitRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
    	}
    	
    	diffuseFitRenderable.program().setUniform("delta", param.getDiffuseDelta());
    	diffuseFitRenderable.program().setUniform("iterations", param.getDiffuseIterations());
    	diffuseFitRenderable.program().setUniform("fit1Weight", param.getDiffuseInputNormalWeight());
    	diffuseFitRenderable.program().setUniform("fit3Weight", param.getDiffuseComputedNormalWeight());
    	
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
    	
    	if (!param.isImagePreprojectionUseEnabled() && param.getTextureSubdivision() == 1)
    	{
	    	specularFitRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
	    	specularFitRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
    	}

    	specularFitRenderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
    	specularFitRenderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
    	specularFitRenderable.program().setUniform("gamma", param.getGamma());
    	
    	specularFitRenderable.program().setUniform("computeRoughness", param.isSpecularRoughnessComputationEnabled());
    	specularFitRenderable.program().setUniform("computeNormal", param.isSpecularNormalComputationEnabled());
    	specularFitRenderable.program().setUniform("trueBlinnPhong", param.isTrueBlinnPhongSpecularEnabled());

    	specularFitRenderable.program().setUniform("diffuseRemovalAmount", param.getSpecularSubtractDiffuseAmount());
    	specularFitRenderable.program().setUniform("specularInfluenceScale", param.getSpecularInfluenceScale());
    	specularFitRenderable.program().setUniform("determinantThreshold", param.getSpecularDeterminantThreshold());
    	specularFitRenderable.program().setUniform("fit1Weight", param.getSpecularInputNormalDefaultRoughnessWeight());
    	specularFitRenderable.program().setUniform("fit2Weight", param.getSpecularInputNormalComputedRoughnessWeight());
    	specularFitRenderable.program().setUniform("fit4Weight", param.getSpecularComputedNormalWeight());
    	specularFitRenderable.program().setUniform("defaultSpecularColor", new Vector3(0.0f, 0.0f, 0.0f));
    	specularFitRenderable.program().setUniform("defaultSpecularRoughness", param.getDefaultSpecularRoughness());
    	specularFitRenderable.program().setUniform("specularRoughnessScale", param.getSpecularRoughnessCap());

    	File diffuseTempDirectory = new File(tmpDir, "diffuse");
    	File normalTempDirectory = new File(tmpDir, "normal");
    	File specularTempDirectory = new File(tmpDir, "specular");
    	File roughnessTempDirectory = new File(tmpDir, "roughness");
    	File snormalTempDirectory = new File(tmpDir, "snormal");
    	
    	diffuseTempDirectory.mkdir();
    	normalTempDirectory.mkdir();
    	specularTempDirectory.mkdir();
    	roughnessTempDirectory.mkdir();
    	snormalTempDirectory.mkdir();
    	
    	int subdivSize = param.getTextureSize() / param.getTextureSubdivision();
    	
    	for (int row = 0; row < param.getTextureSubdivision(); row++)
    	{
	    	for (int col = 0; col < param.getTextureSubdivision(); col++)
    		{
		    	if (!param.isImagePreprojectionUseEnabled() && param.getTextureSubdivision() == 1)
	    		{
			    	System.out.println("Fitting diffuse...");
			    	timestamp = new Date();
	    		}
		    	
		    	OpenGLTextureArray preprojectedViews = null;
		    	
		    	if (param.isImagePreprojectionUseEnabled() || param.getTextureSubdivision() > 1)
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
	    				new Vector2((float)col / (float)param.getTextureSubdivision(), (float)row / (float)param.getTextureSubdivision()));
	    		
		    	diffuseFitRenderable.program().setUniform("maxTexCoord", 
	    				new Vector2((float)(col+1) / (float)param.getTextureSubdivision(), (float)(row+1) / (float)param.getTextureSubdivision()));
		    	
		        diffuseFitRenderable.draw(PrimitiveMode.TRIANGLES, diffuseFitFramebuffer, col * subdivSize, row * subdivSize, subdivSize, subdivSize);
		        context.finish();
		        
		        diffuseFitFramebuffer.saveColorBufferToFile(0, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
		        		"PNG", new File(diffuseTempDirectory, String.format("r%04dc%04d.png", row, col)));
		        
		        diffuseFitFramebuffer.saveColorBufferToFile(1, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
		        		"PNG", new File(normalTempDirectory, String.format("r%04dc%04d.png", row, col)));
	    		
		        if (!param.isImagePreprojectionUseEnabled() && param.getTextureSubdivision() == 1)
		        {
		        	System.out.println("Diffuse fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		        }
		    	
		    	if (!param.isImagePreprojectionUseEnabled() && param.getTextureSubdivision() == 1)
		        {
		        	System.out.println("Fitting specular...");
		        	timestamp = new Date();
		        }
		    	
		    	if (param.isImagePreprojectionUseEnabled() || param.getTextureSubdivision() > 1)
		    	{
		    		specularFitRenderable.program().setTexture("viewImages", preprojectedViews);
		    	}
		    	else
		    	{
			    	specularFitRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
			    	specularFitRenderable.program().setTexture("depthImages", lightField.depthTextures);
		    	}
		    	
		    	if (lightField.viewSet.getLightPositionBuffer() != null && lightField.viewSet.getLightIndexBuffer() != null)
	    		{
		    		specularFitRenderable.program().setUniformBuffer("LightPositions", lightField.viewSet.getLightPositionBuffer());
		    		specularFitRenderable.program().setUniformBuffer("LightIndices", lightField.viewSet.getLightIndexBuffer());
	    		}
		    	
		    	specularFitRenderable.program().setUniform("minTexCoord", 
	    				new Vector2((float)col / (float)param.getTextureSubdivision(), (float)row / (float)param.getTextureSubdivision()));
	    		
		    	specularFitRenderable.program().setUniform("maxTexCoord", 
	    				new Vector2((float)(col+1) / (float)param.getTextureSubdivision(), (float)(row+1) / (float)param.getTextureSubdivision()));

		    	specularFitRenderable.program().setTexture("diffuseEstimate", diffuseFitFramebuffer.getColorAttachmentTexture(0));
		    	specularFitRenderable.program().setTexture("normalEstimate", diffuseFitFramebuffer.getColorAttachmentTexture(1));
		        
	    		specularFitRenderable.draw(PrimitiveMode.TRIANGLES, specularFitFramebuffer, col * subdivSize, row * subdivSize, subdivSize, subdivSize);
	    		context.finish();

	    		specularFitFramebuffer.saveColorBufferToFile(0, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
		        		"PNG", new File(specularTempDirectory, String.format("r%04dc%04d.png", row, col)));
		        
	    		specularFitFramebuffer.saveColorBufferToFile(1, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
		        		"PNG", new File(roughnessTempDirectory, String.format("r%04dc%04d.png", row, col)));
		        
	    		specularFitFramebuffer.saveColorBufferToFile(2, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
		        		"PNG", new File(snormalTempDirectory, String.format("r%04dc%04d.png", row, col)));
		    	
		    	if (param.isImagePreprojectionUseEnabled() || param.getTextureSubdivision() > 1)
		    	{
		    		preprojectedViews.delete();
		    	}
	    		
		    	if (param.isImagePreprojectionUseEnabled() || param.getTextureSubdivision() > 1)
	    		{
	    			System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
	    		}
		    	else
		    	{
			    	System.out.println("Specular fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		        }
    		}
    	}
    	
    	if (param.isImagePreprojectionUseEnabled() || param.getTextureSubdivision() > 1)
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
    	
    	if (DEBUG && !param.isImagePreprojectionUseEnabled() && param.getTextureSubdivision() == 1)
    	{
    		System.out.println("Generating diffuse debug info...");
	    	timestamp = new Date();

	    	new File(outputDirectory, "debug").mkdirs();
    		
    		OpenGLFramebufferObject diffuseDebugFBO = new OpenGLFramebufferObject(param.getTextureSize(), param.getTextureSize(), 2, true, false);
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
	    	diffuseDebugRenderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
	    	diffuseDebugRenderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
	    	
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
    		
	    	OpenGLFramebufferObject specularDebugFBO = new OpenGLFramebufferObject(param.getTextureSize(), param.getTextureSize(), 2, true, false);
	    	OpenGLRenderable specularDebugRenderable = new OpenGLRenderable(specularDebugProgram);

	    	specularDebugRenderable.program().setUniform("minTexCoord", new Vector2(0.0f, 0.0f));
	    	specularDebugRenderable.program().setUniform("maxTexCoord", new Vector2(1.0f, 1.0f));
	    	
	    	specularDebugRenderable.addVertexBuffer("position", lightField.positionBuffer);
	    	specularDebugRenderable.addVertexBuffer("texCoord", lightField.texCoordBuffer);
	    	specularDebugRenderable.addVertexBuffer("normal", lightField.normalBuffer);

	    	specularDebugRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
	    	specularDebugRenderable.program().setTexture("depthImages", lightField.depthTextures);
	    	specularDebugRenderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
	    	specularDebugRenderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
	    	specularDebugRenderable.program().setTexture("diffuse", diffuseFitFramebuffer.getColorAttachmentTexture(0));
	    	specularDebugRenderable.program().setTexture("normalMap", diffuseFitFramebuffer.getColorAttachmentTexture(1));
	    	specularDebugRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
	    	specularDebugRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
	    	specularDebugRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
	    	specularDebugRenderable.program().setUniform("gamma", param.getGamma());
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

		projTexProgram.delete();
		diffuseFitProgram.delete();
		specularFitProgram.delete();
		diffuseDebugProgram.delete();
		specularDebugProgram.delete();
    	depthRenderingProgram.delete();
	}
}
