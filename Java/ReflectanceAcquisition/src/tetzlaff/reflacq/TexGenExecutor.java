package tetzlaff.reflacq;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import javax.imageio.ImageIO;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector2;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLTexture2D;
import tetzlaff.gl.opengl.OpenGLVertexBuffer;
import tetzlaff.ulf.ViewSet;

public class TexGenExecutor 
{
	// Debug parameters
	private static final boolean DEBUG = false;

	private OpenGLContext context;
	private File vsetFile;
	private File objFile;
	private File imageDir;
	private File maskDir;
	private File rescaleDir;
	private File outputDir;
	private TexGenParameters param;
	
	public TexGenExecutor(OpenGLContext context, File vsetFile, File objFile, File imageDir, File maskDir, File rescaleDir, File outputDir, TexGenParameters param) 
	{
		this.context = context;
		this.vsetFile = vsetFile;
		this.objFile = objFile;
		this.imageDir = imageDir;
		this.maskDir = maskDir;
		this.rescaleDir = rescaleDir;
		this.outputDir = outputDir;
		this.param = param;
	}

	public void execute() throws IOException
	{
		final int DEBUG_PIXEL_X = 322;
		final int DEBUG_PIXEL_Y = param.getTextureSize() - 365;

    	System.out.println("Max vertex uniform components across all blocks:" + context.getMaxCombinedVertexUniformComponents());
    	System.out.println("Max fragment uniform components across all blocks:" + context.getMaxCombinedFragmentUniformComponents());
    	System.out.println("Max size of a uniform block in bytes:" + context.getMaxUniformBlockSize());
    	System.out.println("Max texture array layers:" + context.getMaxArrayTextureLayers());
		
		System.out.println("Loading view set...");
    	Date timestamp = new Date();
		
    	ViewSet viewSet = null;
    	String[] vsetFileNameParts = vsetFile.getName().split("\\.");
    	String fileExt = vsetFileNameParts[vsetFileNameParts.length-1];
    	if (fileExt.equalsIgnoreCase("vset"))
    	{
    		System.out.println("Loading from VSET file.");
    		viewSet = ViewSet.loadFromVSETFile(vsetFile, false, context);
    	}
    	else if (fileExt.equalsIgnoreCase("xml"))
    	{
    		System.out.println("Loading from Agisoft Photoscan XML file.");
    		viewSet = ViewSet.loadFromAgisoftXMLFile(vsetFile, null, context);
    	}
    	else
    	{
    		System.out.println("Unrecognized file type, aborting.");
    		return;
    	}
    	
    	outputDir.mkdir();
    	if (DEBUG)
    	{
    		new File(outputDir, "debug").mkdir();
    	}
    	
    	System.out.println("Loading view set completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	
    	System.out.println("Loading and compiling shader programs...");
    	timestamp = new Date();
		
		context.enableDepthTest();
    	context.enableBackFaceCulling();
    	
    	OpenGLProgram depthRenderingProgram = new OpenGLProgram(new File("shaders/depth.vert"), new File("shaders/depth.frag"));
    	
    	OpenGLProgram projTexProgram = new OpenGLProgram(
    			new File("shaders", "texspace.vert"), 
    			new File("shaders", "projtex_single.frag"));
    	
		OpenGLProgram diffuseFitProgram = new OpenGLProgram(
				new File("shaders", "texspace.vert"), 
				new File("shaders", param.isImagePreprojectionUseEnabled() ? "diffusefit_texspace.frag" : "diffusefit_imgspace.frag"));
		
		OpenGLProgram specularFitProgram = new OpenGLProgram(
				new File("shaders", "texspace.vert"), 
				new File("shaders", param.isImagePreprojectionUseEnabled() ? "specularfit_texspace.frag" : "specularfit_imgspace.frag"));
		
    	OpenGLProgram diffuseDebugProgram = new OpenGLProgram(
    			new File("shaders", "texspace.vert"), 
    			new File("shaders", "projtex_multi.frag"));
		
		OpenGLProgram specularDebugProgram = new OpenGLProgram(
				new File("shaders", "texspace.vert"), 
				new File("shaders", "speculardebug_imgspace.frag"));
		
		OpenGLProgram textureRectProgram = new OpenGLProgram(
				new File("shaders", "texturerect.vert"),
				new File("shaders", "simpletexture.frag"));
		
		OpenGLProgram holeFillProgram = new OpenGLProgram(
				new File("shaders", "texturerect.vert"),
				new File("shaders", "holefill.frag"));
		
    	System.out.println("Shader compilation completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	
    	System.out.println("Loading mesh...");
    	timestamp = new Date();
    	
    	VertexMesh mesh = new VertexMesh("OBJ", objFile);
    	OpenGLVertexBuffer positionBuffer = new OpenGLVertexBuffer(mesh.getVertices());
    	OpenGLVertexBuffer texCoordBuffer = new OpenGLVertexBuffer(mesh.getTexCoords());
    	OpenGLVertexBuffer normalBuffer = new OpenGLVertexBuffer(mesh.getNormals());
    	
    	System.out.println("Loading mesh completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	
    	File tmpDir = new File(outputDir, "tmp");
    	
    	Texture3D<OpenGLContext> viewTextures = null;
    	Texture3D<OpenGLContext> depthTextures = null;
    	
    	if (param.isImagePreprojectionUseEnabled() && param.isImagePreprojectionGenerationEnabled())
    	{
    		System.out.println("Pre-projecting images into texture space...");
	    	timestamp = new Date();
	    	
	    	FramebufferObject<OpenGLContext> projTexFBO = 
    			context.getFramebufferObjectBuilder(param.getTextureSize() / param.getTextureSubdivision(), param.getTextureSize() / param.getTextureSubdivision())
    				.addColorAttachments(ColorFormat.RGBA32F, 2)
    				.createFramebufferObject();
	    	OpenGLRenderable projTexRenderable = new OpenGLRenderable(projTexProgram);
	    	
	    	projTexRenderable.addVertexBuffer("position", positionBuffer);
	    	projTexRenderable.addVertexBuffer("texCoord", texCoordBuffer);
	    	projTexRenderable.addVertexBuffer("normal", normalBuffer);
	    	
	    	projTexRenderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
	    	projTexRenderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
	    	
	    	tmpDir.mkdir();
	    	
	    	for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
	    	{
		    	File viewDir = new File(tmpDir, String.format("%04d", i));
		    	viewDir.mkdir();
		    	
		    	File imageFile = new File(imageDir, viewSet.getImageFileName(i));
				if (!imageFile.exists())
				{
					String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
			    	filenameParts[filenameParts.length - 1] = "png";
			    	String pngFileName = String.join(".", filenameParts);
			    	imageFile = new File(imageDir, pngFileName);
				}
		    	
		    	Texture2D<OpenGLContext> viewTexture;
		    	if (maskDir == null)
		    	{
		    		viewTexture = context.get2DColorTextureBuilder(imageFile, true)
		    						.setLinearFilteringEnabled(true)
		    						.setMipmapsEnabled(true)
		    						.createTexture();
		    	}
		    	else
		    	{
		    		File maskFile = new File(maskDir, viewSet.getImageFileName(i));
					if (!maskFile.exists())
					{
						String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
				    	filenameParts[filenameParts.length - 1] = "png";
				    	String pngFileName = String.join(".", filenameParts);
				    	maskFile = new File(maskDir, pngFileName);
					}
					
		    		viewTexture = context.get2DColorTextureBuilder(imageFile, maskFile, true)
		    						.setLinearFilteringEnabled(true)
		    						.setMipmapsEnabled(true)
		    						.createTexture();
		    	}
		    	
		    	FramebufferObject<OpenGLContext> depthFBO = 
	    			context.getFramebufferObjectBuilder(viewTexture.getWidth(), viewTexture.getHeight())
	    				.addDepthAttachment()
	    				.createFramebufferObject();
		    	
		    	OpenGLRenderable depthRenderable = new OpenGLRenderable(depthRenderingProgram);
		    	depthRenderable.addVertexBuffer("position", positionBuffer);
		    	
	        	depthRenderingProgram.setUniform("model_view", viewSet.getCameraPose(i));
	    		depthRenderingProgram.setUniform("projection", 
    				viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(i))
	    				.getProjectionMatrix(
    						viewSet.getRecommendedNearPlane(), 
    						viewSet.getRecommendedFarPlane()
						)
				);
	        	
	    		depthFBO.clearDepthBuffer();
	        	depthRenderable.draw(PrimitiveMode.TRIANGLES, depthFBO);
	        	
	        	projTexRenderable.program().setUniform("cameraPose", viewSet.getCameraPose(i));
	        	projTexRenderable.program().setUniform("cameraProjection", 
	        			viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(i))
	        				.getProjectionMatrix(viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
		    	
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
		    	
		    	System.out.println("Completed " + (i+1) + "/" + viewSet.getCameraPoseCount());
	    	}
	    	
	    	System.out.println("Pre-projections completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	}
    	else
    	{
    		if (param.isImageRescalingEnabled())
    		{
    			System.out.println("Loading and rescaling images...");
    			timestamp = new Date();
    			
    			viewTextures = context.get2DColorTextureArrayBuilder(param.getImageWidth(), param.getImageHeight(), viewSet.getCameraPoseCount())
    							.setLinearFilteringEnabled(true)
    							.setMipmapsEnabled(true)
    							.createTexture();
    			
				// Create an FBO for downsampling
		    	FramebufferObject<OpenGLContext> downsamplingFBO = 
	    			context.getFramebufferObjectBuilder(param.getImageWidth(), param.getImageHeight())
	    				.addEmptyColorAttachment()
	    				.createFramebufferObject();
		    	
		    	OpenGLRenderable downsampleRenderable = new OpenGLRenderable(textureRectProgram);
		    	OpenGLVertexBuffer rectBuffer = OpenGLVertexBuffer.createRectangle();
		    	downsampleRenderable.addVertexBuffer("position", rectBuffer);
		    	
		    	// Downsample and store each image
		    	for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
		    	{
		    		File imageFile = new File(imageDir, viewSet.getImageFileName(i));
					if (!imageFile.exists())
					{
						String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
				    	filenameParts[filenameParts.length - 1] = "png";
				    	String pngFileName = String.join(".", filenameParts);
				    	imageFile = new File(imageDir, pngFileName);
					}
		    		
		    		Texture2D<OpenGLContext> fullSizeImage;
		    		if (maskDir == null)
	    			{
		    			fullSizeImage = context.get2DColorTextureBuilder(imageFile, true)
		    								.setLinearFilteringEnabled(true)
		    								.setMipmapsEnabled(true)
		    								.createTexture();
	    			}
		    		else
		    		{
		    			File maskFile = new File(maskDir, viewSet.getImageFileName(i));
						if (!maskFile.exists())
						{
							String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
					    	filenameParts[filenameParts.length - 1] = "png";
					    	String pngFileName = String.join(".", filenameParts);
					    	maskFile = new File(maskDir, pngFileName);
						}
						
		    			fullSizeImage = context.get2DColorTextureBuilder(imageFile, maskFile, true)
											.setLinearFilteringEnabled(true)
											.setMipmapsEnabled(true)
											.createTexture();
		    		}
		    		
		    		downsamplingFBO.setColorAttachment(0, viewTextures.getLayerAsFramebufferAttachment(i));
		    		downsamplingFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		        	
		    		textureRectProgram.setTexture("tex", fullSizeImage);
		        	
		        	downsampleRenderable.draw(PrimitiveMode.TRIANGLE_FAN, downsamplingFBO);
		        	context.finish();
		        	
		        	if (rescaleDir != null)
		        	{
				    	String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
				    	filenameParts[filenameParts.length - 1] = "png";
				    	String pngFileName = String.join(".", filenameParts);
		        		downsamplingFBO.saveColorBufferToFile(0, "PNG", new File(rescaleDir, pngFileName));
		        	}
		        	
		        	fullSizeImage.delete();
		        	
					System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " images loaded and rescaled.");
		    	}
	
		    	rectBuffer.delete();
		    	downsamplingFBO.delete();
		    	
		    	// TODO why don't mipmaps work?
		    	//viewTextures.generateMipmaps();
		    	//context.finish();
		    	
	    		System.out.println("Image loading and rescaling completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    		}
    		else
    		{
	    		System.out.println("Loading images...");
		    	timestamp = new Date();
		    	
		    	// Read a single image to get the dimensions for the texture array
		    	File imageFile = new File(imageDir, viewSet.getImageFileName(0));
				if (!imageFile.exists())
				{
					String[] filenameParts = viewSet.getImageFileName(0).split("\\.");
			    	filenameParts[filenameParts.length - 1] = "png";
			    	String pngFileName = String.join(".", filenameParts);
			    	imageFile = new File(imageDir, pngFileName);
				}
				BufferedImage img = ImageIO.read(new FileInputStream(imageFile));
				viewTextures = context.get2DColorTextureArrayBuilder(img.getWidth(), img.getHeight(), viewSet.getCameraPoseCount())
								.setLinearFilteringEnabled(true)
								.setMipmapsEnabled(true)
								.createTexture();
				
				for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
				{
					imageFile = new File(imageDir, viewSet.getImageFileName(i));
					if (!imageFile.exists())
					{
						String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
				    	filenameParts[filenameParts.length - 1] = "png";
				    	String pngFileName = String.join(".", filenameParts);
				    	imageFile = new File(imageDir, pngFileName);
					}
					
					if (maskDir == null)
					{
						viewTextures.loadLayer(i, imageFile, true);
					}
					else
					{
						File maskFile = new File(maskDir, viewSet.getImageFileName(i));
						if (!maskFile.exists())
						{
							String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
					    	filenameParts[filenameParts.length - 1] = "png";
					    	String pngFileName = String.join(".", filenameParts);
					    	maskFile = new File(maskDir, pngFileName);
						}
						
						viewTextures.loadLayer(i, imageFile, maskFile, true);
					}
					
					System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " images loaded.");
				}
		    	
	    		System.out.println("Image loading completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    		}
    		
    		System.out.println("Creating depth maps...");
	    	timestamp = new Date();
	    	
	    	// Build depth textures for each view
	    	int width = viewTextures.getWidth();
	    	int height = viewTextures.getHeight();
	    	depthTextures = context.get2DDepthTextureArrayBuilder(width, height, viewSet.getCameraPoseCount()).createTexture();
	    	
	    	// Don't automatically generate any texture attachments for this framebuffer object
	    	FramebufferObject<OpenGLContext> depthRenderingFBO = context.getFramebufferObjectBuilder(width, height).createFramebufferObject();
	    	
	    	OpenGLRenderable depthRenderable = new OpenGLRenderable(depthRenderingProgram);
	    	depthRenderable.addVertexBuffer("position", positionBuffer);
	    	
	    	// Render each depth texture
	    	for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
	    	{
	    		depthRenderingFBO.setDepthAttachment(depthTextures.getLayerAsFramebufferAttachment(i));
	        	depthRenderingFBO.clearDepthBuffer();
	        	
	        	depthRenderingProgram.setUniform("model_view", viewSet.getCameraPose(i));
	    		depthRenderingProgram.setUniform("projection", 
					viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(i))
	    				.getProjectionMatrix(
							viewSet.getRecommendedNearPlane(), 
							viewSet.getRecommendedFarPlane()
						)
				);
	        	
	        	depthRenderable.draw(PrimitiveMode.TRIANGLES, depthRenderingFBO);
				System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " depth maps created.");
	    	}

	    	depthRenderingFBO.delete();
	    	
    		System.out.println("Depth maps created in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	}
    	
    	if (param.getTextureSubdivision() > 1)
    	{
	    	System.out.println("Beginning model fitting (" + (param.getTextureSubdivision() * param.getTextureSubdivision()) + " blocks)...");
    	}
    	else
    	{
	    	System.out.println("Setting up model fitting...");
    	}
    	timestamp = new Date();
    	
    	FramebufferObject<OpenGLContext> diffuseFitFramebuffer = 
			context.getFramebufferObjectBuilder(param.getTextureSize(), param.getTextureSize())
				.addColorAttachments(4)
				.createFramebufferObject();
    	
    	FramebufferObject<OpenGLContext> specularFitFramebuffer = 
			context.getFramebufferObjectBuilder(param.getTextureSize(), param.getTextureSize())
				.addColorAttachments(4)
				.createFramebufferObject();
    	
    	diffuseFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
    	diffuseFitFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
    	diffuseFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
    	diffuseFitFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
    	
    	specularFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
        specularFitFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
        specularFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
        specularFitFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);

    	OpenGLRenderable diffuseFitRenderable = new OpenGLRenderable(diffuseFitProgram);
    	
    	diffuseFitRenderable.addVertexBuffer("position", positionBuffer);
    	diffuseFitRenderable.addVertexBuffer("texCoord", texCoordBuffer);
    	diffuseFitRenderable.addVertexBuffer("normal", normalBuffer);
    	
    	diffuseFitRenderable.program().setUniform("viewCount", viewSet.getCameraPoseCount());
    	diffuseFitRenderable.program().setUniform("gamma", param.getGamma());
    	diffuseFitRenderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
    	diffuseFitRenderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
    	
    	diffuseFitRenderable.program().setUniformBuffer("CameraPoses", viewSet.getCameraPoseBuffer());
    	
    	if (!param.isImagePreprojectionUseEnabled())
    	{
	    	diffuseFitRenderable.program().setUniformBuffer("CameraProjections", viewSet.getCameraProjectionBuffer());
	    	diffuseFitRenderable.program().setUniformBuffer("CameraProjectionIndices", viewSet.getCameraProjectionIndexBuffer());
    	}
    	
    	diffuseFitRenderable.program().setUniform("delta", param.getDiffuseDelta());
    	diffuseFitRenderable.program().setUniform("iterations", param.getDiffuseIterations());
    	diffuseFitRenderable.program().setUniform("fit1Weight", param.getDiffuseInputNormalWeight());
    	diffuseFitRenderable.program().setUniform("fit3Weight", param.getDiffuseComputedNormalWeight());
    	
    	if (viewSet.getLightPositionBuffer() != null && viewSet.getLightIndexBuffer() != null)
		{
    		diffuseFitRenderable.program().setUniformBuffer("LightPositions", viewSet.getLightPositionBuffer());
    		diffuseFitRenderable.program().setUniformBuffer("LightIndices", viewSet.getLightIndexBuffer());
		}
    	
    	OpenGLRenderable specularFitRenderable = new OpenGLRenderable(specularFitProgram);
    	
    	specularFitRenderable.addVertexBuffer("position", positionBuffer);
    	specularFitRenderable.addVertexBuffer("texCoord", texCoordBuffer);
    	specularFitRenderable.addVertexBuffer("normal", normalBuffer);

    	specularFitRenderable.program().setUniform("viewCount", viewSet.getCameraPoseCount());
    	specularFitRenderable.program().setUniformBuffer("CameraPoses", viewSet.getCameraPoseBuffer());
    	
    	if (!param.isImagePreprojectionUseEnabled())
    	{
	    	specularFitRenderable.program().setUniformBuffer("CameraProjections", viewSet.getCameraProjectionBuffer());
	    	specularFitRenderable.program().setUniformBuffer("CameraProjectionIndices", viewSet.getCameraProjectionIndexBuffer());
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
    	
    	if (param.getTextureSubdivision() == 1)
		{
	    	System.out.println("Setup finished in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		}
    	
    	for (int row = 0; row < param.getTextureSubdivision(); row++)
    	{
	    	for (int col = 0; col < param.getTextureSubdivision(); col++)
    		{
		    	if (param.getTextureSubdivision() == 1)
	    		{
			    	System.out.println("Fitting diffuse...");
			    	timestamp = new Date();
	    		}
		    	
		    	Texture3D<OpenGLContext> preprojectedViews = null;
		    	
		    	if (param.isImagePreprojectionUseEnabled())
		    	{
		    		preprojectedViews = context.get2DColorTextureArrayBuilder(subdivSize, subdivSize, viewSet.getCameraPoseCount()).createTexture();
			    	
					for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
					{
						preprojectedViews.loadLayer(i, new File(new File(tmpDir, String.format("%04d", i)), String.format("r%04dc%04d.png", row, col)), true);
					}
		    		
		    		diffuseFitRenderable.program().setTexture("viewImages", preprojectedViews);
		    	}
		    	else
		    	{
			    	diffuseFitRenderable.program().setTexture("viewImages", viewTextures);
			    	diffuseFitRenderable.program().setTexture("depthImages", depthTextures);
		    	}
		    	
		    	diffuseFitRenderable.program().setUniform("minTexCoord", 
	    				new Vector2((float)col / (float)param.getTextureSubdivision(), (float)row / (float)param.getTextureSubdivision()));
	    		
		    	diffuseFitRenderable.program().setUniform("maxTexCoord", 
	    				new Vector2((float)(col+1) / (float)param.getTextureSubdivision(), (float)(row+1) / (float)param.getTextureSubdivision()));
		    	
		        diffuseFitRenderable.draw(PrimitiveMode.TRIANGLES, diffuseFitFramebuffer, col * subdivSize, row * subdivSize, subdivSize, subdivSize);
		        context.finish();
		        
		        if (param.isImagePreprojectionUseEnabled())
		        {
			        diffuseFitFramebuffer.saveColorBufferToFile(0, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
			        		"PNG", new File(diffuseTempDirectory, String.format("r%04dc%04d.png", row, col)));
			        
			        diffuseFitFramebuffer.saveColorBufferToFile(1, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
			        		"PNG", new File(normalTempDirectory, String.format("r%04dc%04d.png", row, col)));
		        }
	    		
		        if (param.getTextureSubdivision() == 1)
		        {
		        	System.out.println("Diffuse fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		        	
		        	System.out.println("Fitting specular...");
		        	timestamp = new Date();
		        }
		    	
		    	if (param.isImagePreprojectionUseEnabled())
		    	{
		    		specularFitRenderable.program().setTexture("viewImages", preprojectedViews);
		    	}
		    	else
		    	{
			    	specularFitRenderable.program().setTexture("viewImages", viewTextures);
			    	specularFitRenderable.program().setTexture("depthImages", depthTextures);
		    	}
		    	
		    	if (viewSet.getLightPositionBuffer() != null && viewSet.getLightIndexBuffer() != null)
	    		{
		    		specularFitRenderable.program().setUniformBuffer("LightPositions", viewSet.getLightPositionBuffer());
		    		specularFitRenderable.program().setUniformBuffer("LightIndices", viewSet.getLightIndexBuffer());
	    		}
		    	
		    	specularFitRenderable.program().setUniform("minTexCoord", 
	    				new Vector2((float)col / (float)param.getTextureSubdivision(), (float)row / (float)param.getTextureSubdivision()));
	    		
		    	specularFitRenderable.program().setUniform("maxTexCoord", 
	    				new Vector2((float)(col+1) / (float)param.getTextureSubdivision(), (float)(row+1) / (float)param.getTextureSubdivision()));

		    	specularFitRenderable.program().setTexture("diffuseEstimate", diffuseFitFramebuffer.getColorAttachmentTexture(0));
		    	specularFitRenderable.program().setTexture("normalEstimate", diffuseFitFramebuffer.getColorAttachmentTexture(1));
		        
	    		specularFitRenderable.draw(PrimitiveMode.TRIANGLES, specularFitFramebuffer, col * subdivSize, row * subdivSize, subdivSize, subdivSize);
	    		context.finish();

	    		if (param.isImagePreprojectionUseEnabled())
	    		{
		    		specularFitFramebuffer.saveColorBufferToFile(0, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
			        		"PNG", new File(specularTempDirectory, String.format("r%04dc%04d.png", row, col)));
			        
		    		specularFitFramebuffer.saveColorBufferToFile(1, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
			        		"PNG", new File(roughnessTempDirectory, String.format("r%04dc%04d.png", row, col)));
			        
		    		specularFitFramebuffer.saveColorBufferToFile(2, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
			        		"PNG", new File(snormalTempDirectory, String.format("r%04dc%04d.png", row, col)));
		    		
		    		preprojectedViews.delete();
		    	}
	    		
		    	if (param.getTextureSubdivision() > 1)
	    		{
	    			System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
	    		}
		    	else
		    	{
			    	System.out.println("Specular fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		        }
    		}
    	}
    	
    	if (!DEBUG || param.isImagePreprojectionUseEnabled())
    	{
	        viewSet.deleteOpenGLResources();
	        positionBuffer.delete();
	        normalBuffer.delete();
	        texCoordBuffer.delete();
	        
	        if (viewTextures != null)
	        {
	        	viewTextures.delete();
	        }
	        
	        if (depthTextures != null)
	        {
	        	depthTextures.delete();
	        }
    	}
    	
    	if (param.getTextureSubdivision() > 1)
    	{
    		System.out.println("Model fitting completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	}
    	
    	System.out.println("Filling empty regions...");
    	timestamp = new Date();
    	
    	FramebufferObject<OpenGLContext> holeFillBackFBO = 
			context.getFramebufferObjectBuilder(param.getTextureSize(), param.getTextureSize())
				.addColorAttachments(4)
				.createFramebufferObject();
    	
    	OpenGLRenderable holeFillRenderable = new OpenGLRenderable(holeFillProgram);
    	OpenGLVertexBuffer rectBuffer = OpenGLVertexBuffer.createRectangle();
    	holeFillRenderable.addVertexBuffer("position", rectBuffer);
    	
    	holeFillProgram.setUniform("minFillAlpha", 0.5f);
    	
    	// Diffuse
    	FramebufferObject<OpenGLContext> holeFillFrontFBO = diffuseFitFramebuffer;
    	for (int i = 0; i < param.getTextureSize() / 2; i++)
    	{
    		holeFillBackFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
    		holeFillBackFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
    		holeFillBackFBO.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
    		holeFillBackFBO.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
    		
    		holeFillProgram.setTexture("input0", holeFillFrontFBO.getColorAttachmentTexture(0));
    		holeFillProgram.setTexture("input1", holeFillFrontFBO.getColorAttachmentTexture(1));
    		holeFillProgram.setTexture("input2", holeFillFrontFBO.getColorAttachmentTexture(2));
    		holeFillProgram.setTexture("input3", holeFillFrontFBO.getColorAttachmentTexture(3));
    		
    		holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, holeFillBackFBO);
    		context.finish();
    		
    		FramebufferObject<OpenGLContext> tmp = holeFillFrontFBO;
    		holeFillFrontFBO = holeFillBackFBO;
    		holeFillBackFBO = tmp;
    		
    		System.out.println("Diffuse fill iteration " + i + "/" + (param.getTextureSize() / 2) + " completed.");
    	}
    	diffuseFitFramebuffer = holeFillFrontFBO;
    	
    	// Specular
    	holeFillFrontFBO = specularFitFramebuffer;
    	for (int i = 0; i < param.getTextureSize() / 2; i++)
    	{
    		holeFillBackFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
    		holeFillBackFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
    		holeFillBackFBO.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
    		holeFillBackFBO.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
    		
    		holeFillProgram.setTexture("input0", holeFillFrontFBO.getColorAttachmentTexture(0));
    		holeFillProgram.setTexture("input1", holeFillFrontFBO.getColorAttachmentTexture(1));
    		holeFillProgram.setTexture("input2", holeFillFrontFBO.getColorAttachmentTexture(2));
    		holeFillProgram.setTexture("input3", holeFillFrontFBO.getColorAttachmentTexture(3));
    		
    		holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, holeFillBackFBO);
    		context.finish();
    		
    		FramebufferObject<OpenGLContext> tmp = holeFillFrontFBO;
    		holeFillFrontFBO = holeFillBackFBO;
    		holeFillBackFBO = tmp;
    		
    		System.out.println("Specular fill iteration " + i + "/" + (param.getTextureSize() / 2) + " completed.");
    	}
    	specularFitFramebuffer = holeFillFrontFBO;

		System.out.println("Empty regions filled in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	
    	System.out.println("Saving textures...");
    	timestamp = new Date();
    	
    	File textureDirectory = new File(outputDir, "textures");
    	
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
    	if (param.isSpecularNormalComputationEnabled())
    	{
    		specularFitFramebuffer.saveColorBufferToFile(2, "PNG", new File(textureDirectory, "snormal.png"));
    	}
    	if (DEBUG)
    	{
	    	specularFitFramebuffer.saveColorBufferToFile(3, "PNG", new File(textureDirectory, "textures/sdebug.png"));
    	}
    	
    	diffuseFitFramebuffer.delete();
    	specularFitFramebuffer.delete();

    	System.out.println("Textures saved in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	
    	if (DEBUG && !param.isImagePreprojectionUseEnabled())
    	{
    		System.out.println("Generating diffuse debug info...");
	    	timestamp = new Date();

	    	new File(outputDir, "debug").mkdirs();
    		
	    	FramebufferObject<OpenGLContext> diffuseDebugFBO = 
    			context.getFramebufferObjectBuilder(param.getTextureSize(), param.getTextureSize())
    				.addColorAttachments(ColorFormat.RGBA32F, 2)
    				.createFramebufferObject();
	    	
	    	OpenGLRenderable diffuseDebugRenderable = new OpenGLRenderable(diffuseDebugProgram);
	    	
	    	diffuseDebugRenderable.program().setUniform("minTexCoord", new Vector2(0.0f, 0.0f));
	    	diffuseDebugRenderable.program().setUniform("maxTexCoord", new Vector2(1.0f, 1.0f));
	    	
	    	diffuseDebugRenderable.addVertexBuffer("position", positionBuffer);
	    	diffuseDebugRenderable.addVertexBuffer("texCoord", texCoordBuffer);
	    	diffuseDebugRenderable.addVertexBuffer("normal", normalBuffer);

	    	diffuseDebugRenderable.program().setTexture("viewImages", viewTextures);
	    	diffuseDebugRenderable.program().setTexture("depthImages", depthTextures);
	    	diffuseDebugRenderable.program().setUniformBuffer("CameraPoses", viewSet.getCameraPoseBuffer());
	    	diffuseDebugRenderable.program().setUniformBuffer("CameraProjections", viewSet.getCameraProjectionBuffer());
	    	diffuseDebugRenderable.program().setUniformBuffer("CameraProjectionIndices", viewSet.getCameraProjectionIndexBuffer());
	    	diffuseDebugRenderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
	    	diffuseDebugRenderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
	    	
	    	//new File(outputDirectory, "debug/diffuse/projpos").mkdirs();
	    	
	    	PrintStream diffuseInfo = new PrintStream(new File(outputDir, "debug/diffuseInfo.txt"));
	    	
	    	for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
	    	{
	    		diffuseDebugRenderable.program().setUniform("viewIndex", i);
	    		
	    		diffuseDebugFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
	    		diffuseDebugFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
	    		diffuseDebugFBO.clearDepthBuffer();
	    		diffuseDebugRenderable.draw(PrimitiveMode.TRIANGLES, diffuseDebugFBO);
	    		
	    		//diffuseDebugFBO.saveColorBufferToFile(0, "PNG", new File(outputDirectory, String.format("debug/diffuse/%04d.png", i)));
	    		//diffuseDebugFBO.saveColorBufferToFile(1, "PNG", new File(outputDirectory, String.format("debug/diffuse/projpos/%04d.png", i)));
	    		
	    		Matrix4 cameraPose = viewSet.getCameraPose(i);
	    		Vector3 lightPosition = new Matrix3(cameraPose).transpose().times(
    				viewSet.getLightPosition(viewSet.getLightPositionIndex(i))
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
    		
	    	FramebufferObject<OpenGLContext> specularDebugFBO = 
    			context.getFramebufferObjectBuilder(param.getTextureSize(), param.getTextureSize())
    				.addColorAttachments(ColorFormat.RGBA32F, 2)
    				.createFramebufferObject();
	    	OpenGLRenderable specularDebugRenderable = new OpenGLRenderable(specularDebugProgram);

	    	specularDebugRenderable.program().setUniform("minTexCoord", new Vector2(0.0f, 0.0f));
	    	specularDebugRenderable.program().setUniform("maxTexCoord", new Vector2(1.0f, 1.0f));
	    	
	    	specularDebugRenderable.addVertexBuffer("position", positionBuffer);
	    	specularDebugRenderable.addVertexBuffer("texCoord", texCoordBuffer);
	    	specularDebugRenderable.addVertexBuffer("normal", normalBuffer);

	    	specularDebugRenderable.program().setTexture("viewImages", viewTextures);
	    	specularDebugRenderable.program().setTexture("depthImages", depthTextures);
	    	specularDebugRenderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
	    	specularDebugRenderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
	    	specularDebugRenderable.program().setTexture("diffuse", diffuseFitFramebuffer.getColorAttachmentTexture(0));
	    	specularDebugRenderable.program().setTexture("normalMap", diffuseFitFramebuffer.getColorAttachmentTexture(1));
	    	specularDebugRenderable.program().setUniformBuffer("CameraPoses", viewSet.getCameraPoseBuffer());
	    	specularDebugRenderable.program().setUniformBuffer("CameraProjections", viewSet.getCameraProjectionBuffer());
	    	specularDebugRenderable.program().setUniformBuffer("CameraProjectionIndices", viewSet.getCameraProjectionIndexBuffer());
	    	specularDebugRenderable.program().setUniform("gamma", param.getGamma());
	    	specularDebugRenderable.program().setUniform("diffuseRemovalFactor", 1.0f);
	    	
	    	if (viewSet.getLightPositionBuffer() != null && viewSet.getLightIndexBuffer() != null)
    		{
	    		specularDebugRenderable.program().setUniformBuffer("LightPositions", viewSet.getLightPositionBuffer());
	    		specularDebugRenderable.program().setUniformBuffer("LightIndices", viewSet.getLightIndexBuffer());
    		}
	    	
	    	//new File(outputDirectory, "debug/specular/rDotV").mkdirs();
	    	
	    	PrintStream specularInfo = new PrintStream(new File(outputDir, "debug/specularInfo.txt"));
	    	
	    	for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
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
	    	
	        viewSet.deleteOpenGLResources();
	        positionBuffer.delete();
	        normalBuffer.delete();
	        texCoordBuffer.delete();
	        
	        if (viewTextures != null)
	        {
	        	viewTextures.delete();
	        }
	        
	        if (depthTextures != null)
	        {
	        	depthTextures.delete();
	        }
	    	
			System.out.println("Specular debug info completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	}

		projTexProgram.delete();
		diffuseFitProgram.delete();
		specularFitProgram.delete();
		diffuseDebugProgram.delete();
		specularDebugProgram.delete();
    	depthRenderingProgram.delete();
	}
}
