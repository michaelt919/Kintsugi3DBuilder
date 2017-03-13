package tetzlaff.texturefit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;

import javax.imageio.ImageIO;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.helpers.DoubleMatrix3;
import tetzlaff.gl.helpers.DoubleVector3;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector2;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.ulf.ViewSet;

public class TextureFitExecutor<ContextType extends Context<ContextType>>
{
	private static final int SHADOW_MAP_FAR_PLANE_CUSHION = 2; // TODO decide where this should be defined
	private static final int ROUGHNESS_TEXTURE_SIZE = 1; // TODO decide where this should be defined
	
	private static final double FITTING_GAMMA = 2.2; // TODO make this configurable from the interface
	private static final double FITTING_GAMMA_INV = 1.0 / FITTING_GAMMA;
	

	private ContextType context;
	private File vsetFile;
	private File objFile;
	private File imageDir;
	private File maskDir;
	private File rescaleDir;
	private File outputDir;
	private File tmpDir;
	private File auxDir;
	private TextureFitParameters param;
	
	private String materialFileName;
	private String materialName;
	
	private ViewSet<ContextType> viewSet;
	
	private Program<ContextType> depthRenderingProgram;
	private Program<ContextType> projTexProgram;
	private Program<ContextType> lightFitProgram;
	private Program<ContextType> diffuseFitProgram;
	private Program<ContextType> specularFitProgram;
	private Program<ContextType> specularFit2Program;
	private Program<ContextType> specularResidProgram;
	private Program<ContextType> adjustFitProgram;
	private Program<ContextType> errorCalcProgram;
	private Program<ContextType> diffuseDebugProgram;
	private Program<ContextType> specularDebugProgram;
	private Program<ContextType> textureRectProgram;
	private Program<ContextType> holeFillProgram;
	private Program<ContextType> finalizeProgram;
	
	private VertexBuffer<ContextType> positionBuffer;
	private VertexBuffer<ContextType> texCoordBuffer;
	private VertexBuffer<ContextType> normalBuffer;
	private VertexBuffer<ContextType> tangentBuffer;
	private Vector3 center;
	
	UniformBuffer<ContextType> lightPositionBuffer;
	UniformBuffer<ContextType> lightIntensityBuffer;
	UniformBuffer<ContextType> shadowMatrixBuffer;
	
	Texture3D<ContextType> viewTextures;
	Texture3D<ContextType> depthTextures;
	Texture3D<ContextType> shadowTextures;
	
	public TextureFitExecutor(ContextType context, File vsetFile, File objFile, File imageDir, File maskDir, File rescaleDir, File outputDir, TextureFitParameters param) 
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
	
	private void compileShaders() throws IOException
	{
    	System.out.println("Loading and compiling shader programs...");
    	Date timestamp = new Date();
    	
		depthRenderingProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"))
    			.createProgram();
    	
    	projTexProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", "reflectance/projtex_single.frag"))
    			.createProgram();
    	
    	lightFitProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", 
    					(param.isImagePreprojectionUseEnabled() ? "texturefit/lightfit_texspace.frag" : "texturefit/lightfit_imgspace.frag")))
    			.createProgram();
    	
    	diffuseFitProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", 
    					(param.isImagePreprojectionUseEnabled() ? "texturefit/diffusefit_texspace.frag" : "texturefit/diffusefit_imgspace.frag")))
    			.createProgram();
		
    	specularFit2Program = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", 
    					(param.isImagePreprojectionUseEnabled() ? "texturefit/specularfit2_texspace.frag" : "texturefit/specularfit2_imgspace.frag")))
    			.createProgram();
		
    	adjustFitProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders",
    					//"texturefit/adjustfit_debug.frag"))
    					(param.isImagePreprojectionUseEnabled() ? "texturefit/adjustfit_texspace.frag" : "texturefit/adjustfit_imgspace.frag")))
    			.createProgram();
		
    	errorCalcProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", 
    					//"texturefit/errorcalc_debug.frag"))
    					(param.isImagePreprojectionUseEnabled() ? "texturefit/errorcalc_texspace.frag" : "texturefit/errorcalc_imgspace.frag")))
    			.createProgram();
    	
    	specularResidProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", 
    					(param.isImagePreprojectionUseEnabled() ? "texturefit/specularresid_imgspace.frag" : "texturefit/specularresid_imgspace_multi.frag")))
    			.createProgram();
		
    	diffuseDebugProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", "reflectance/projtex_multi.frag"))
    			.createProgram();
		
    	specularDebugProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", "texturefit/specularresid_imgspace.frag"))
    			.createProgram();
		
    	textureRectProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texture.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", "common/texture.frag"))
    			.createProgram();
		
    	holeFillProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texture.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", "texturefit/holefill.frag"))
    			.createProgram();
		
    	finalizeProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texture.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", "texturefit/finalize.frag"))
    			.createProgram();
		
    	System.out.println("Shader compilation completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	}
	
	private interface TextureSpaceCallback<ContextType extends Context<ContextType>>
	{
		void execute(Framebuffer<ContextType> framebuffer, int subdivRow, int subdivCol);
	}
	
	private FramebufferObject<ContextType> projectIntoTextureSpace(
			ViewSet<ContextType> viewSet, Program<ContextType> program, int viewIndex, int textureSize, int textureSubdiv, boolean useExistingTextureArray,
			TextureSpaceCallback<ContextType> callback) throws IOException
	{
		FramebufferObject<ContextType> mainFBO = 
			context.getFramebufferObjectBuilder(textureSize / textureSubdiv, textureSize / textureSubdiv)
				.addColorAttachments(ColorFormat.RGBA32F, 3)
				.createFramebufferObject();
    	Renderable<ContextType> renderable = context.createRenderable(program);
    	
    	renderable.addVertexBuffer("position", positionBuffer);
    	renderable.addVertexBuffer("texCoord", texCoordBuffer);
    	renderable.addVertexBuffer("normal", normalBuffer);
    	renderable.addVertexBuffer("tangent", tangentBuffer);
    	
    	renderable.program().setUniform("gamma", param.getGamma());
    	
    	if (viewSet.getLuminanceMap() == null)
        {
    		renderable.program().setUniform("useLuminanceMap", false);
        }
        else
        {
        	renderable.program().setUniform("useLuminanceMap", true);
        	renderable.program().setTexture("luminanceMap", viewSet.getLuminanceMap());
        }
    	
		int width, height;
		Texture2D<ContextType> viewTexture = null;
    	FramebufferObject<ContextType> shadowFBO = null;
    	
		if (useExistingTextureArray && viewTextures != null)
		{
			if (!param.isCameraVisibilityTestEnabled() || depthTextures == null)
			{
				renderable.program().setUniform("occlusionEnabled", false);
				renderable.program().setUniform("shadowTestEnabled", false);
			}
			else
			{
				renderable.program().setTexture("depthImages", depthTextures);
				renderable.program().setUniform("occlusionEnabled", true);
				renderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
				
				if (shadowTextures == null || shadowMatrixBuffer == null)
				{
					renderable.program().setUniform("shadowTestEnabled", false);
				}
				else
				{
					renderable.program().setTexture("shadowImages", depthTextures);
					renderable.program().setUniformBuffer("ShadowMatrices", shadowMatrixBuffer);
					renderable.program().setUniform("shadowTestEnabled", true);
				}
			}
			
			renderable.program().setUniformBuffer("CameraPoses", viewSet.getCameraPoseBuffer());
			renderable.program().setUniformBuffer("CameraProjections", viewSet.getCameraProjectionBuffer());
			renderable.program().setUniformBuffer("CameraProjectionIndices", viewSet.getCameraProjectionIndexBuffer());
			renderable.program().setUniformBuffer("LightIndices", viewSet.getLightIndexBuffer());
			renderable.program().setUniformBuffer("LightPositions", this.lightPositionBuffer);
			renderable.program().setUniformBuffer("LightIntensities", this.lightIntensityBuffer);

			renderable.program().setTexture("viewImages", viewTextures);
			renderable.program().setUniform("viewIndex", viewIndex);
			renderable.program().setUniform("viewCount", viewSet.getCameraPoseCount());
			renderable.program().setUniform("infiniteLightSources", false);
			
			width = viewTextures.getWidth();
			height = viewTextures.getHeight();
		}
		else
		{
			renderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
	    	renderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
	    	
	    	renderable.program().setUniform("cameraPose", viewSet.getCameraPose(viewIndex));
	    	renderable.program().setUniform("cameraProjection", 
	    			viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex))
	    				.getProjectionMatrix(viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
			
	    	boolean enableShadowTest = param.isCameraVisibilityTestEnabled();

	    	Vector3 lightPosition = viewSet.getLightPosition(viewSet.getLightIndex(viewIndex));
	    	renderable.program().setUniform("lightIntensity", viewSet.getLightIntensity(viewSet.getLightIndex(viewIndex)));
	    	renderable.program().setUniform("lightPosition", lightPosition);
			renderable.program().setUniform("infiniteLightSource", false);
	    	
	    	File imageFile = new File(imageDir, viewSet.getImageFileName(viewIndex));
			if (!imageFile.exists())
			{
				String[] filenameParts = viewSet.getImageFileName(viewIndex).split("\\.");
		    	filenameParts[filenameParts.length - 1] = "png";
		    	String pngFileName = String.join(".", filenameParts);
		    	imageFile = new File(imageDir, pngFileName);
			}
			
			if (maskDir == null)
	    	{
	    		viewTexture = context.get2DColorTextureBuilder(imageFile, true)
	    						.setLinearFilteringEnabled(true)
	    						.setMipmapsEnabled(true)
	    						.createTexture();
	    	}
	    	else
	    	{
	    		File maskFile = new File(maskDir, viewSet.getImageFileName(viewIndex));
				if (!maskFile.exists())
				{
					String[] filenameParts = viewSet.getImageFileName(viewIndex).split("\\.");
			    	filenameParts[filenameParts.length - 1] = "png";
			    	String pngFileName = String.join(".", filenameParts);
			    	maskFile = new File(maskDir, pngFileName);
				}
				
	    		viewTexture = context.get2DColorTextureBuilder(imageFile, maskFile, true)
	    						.setLinearFilteringEnabled(true)
	    						.setMipmapsEnabled(true)
	    						.createTexture();
	    	}
	    	
	    	renderable.program().setTexture("viewImage", viewTexture);
	    	
	    	width = viewTexture.getWidth();
	    	height = viewTexture.getHeight();
	    	
	    	if (enableShadowTest)
	    	{
	        	Matrix4 shadowModelView = Matrix4.lookAt(new Vector3(viewSet.getCameraPoseInverse(viewIndex).times(new Vector4(lightPosition, 1.0f))), center, new Vector3(0, 1, 0));
	        	
	    		Matrix4 shadowProjection = viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex))
	    				.getProjectionMatrix(
	    					viewSet.getRecommendedNearPlane(), 
	    					viewSet.getRecommendedFarPlane() * SHADOW_MAP_FAR_PLANE_CUSHION // double it for good measure
	    				);
	    		
	    		shadowFBO = context.getFramebufferObjectBuilder(width, height)
		    					.addDepthAttachment()
		    					.createFramebufferObject();
	    		
	    		Renderable<ContextType> shadowRenderable = context.createRenderable(depthRenderingProgram);
	    		shadowRenderable.addVertexBuffer("position", positionBuffer);
	        	
	        	depthRenderingProgram.setUniform("model_view", shadowModelView);
	    		depthRenderingProgram.setUniform("projection", shadowProjection);
	        	
	    		shadowFBO.clearDepthBuffer();
	    		shadowRenderable.draw(PrimitiveMode.TRIANGLES, shadowFBO);
	    		
	    		renderable.program().setUniform("shadowTestEnabled", true);
	    		renderable.program().setUniform("shadowMatrix", shadowProjection.times(shadowModelView));
	        	renderable.program().setTexture("shadowImage", shadowFBO.getDepthAttachmentTexture());
	    	}
	    	else
	    	{
	    		renderable.program().setUniform("shadowTestEnabled", false);
	    	}
		}
    	
    	FramebufferObject<ContextType> depthFBO = 
			context.getFramebufferObjectBuilder(width, height)
				.addDepthAttachment()
				.createFramebufferObject();
    	
    	Renderable<ContextType> depthRenderable = context.createRenderable(depthRenderingProgram);
    	depthRenderable.addVertexBuffer("position", positionBuffer);
    	
    	depthRenderingProgram.setUniform("model_view", viewSet.getCameraPose(viewIndex));
		depthRenderingProgram.setUniform("projection", 
			viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex))
				.getProjectionMatrix(
					viewSet.getRecommendedNearPlane(), 
					viewSet.getRecommendedFarPlane()
				)
		);
    	
		depthFBO.clearDepthBuffer();
    	depthRenderable.draw(PrimitiveMode.TRIANGLES, depthFBO);
    	
    	if (!useExistingTextureArray || viewTextures == null)
    	{
    		renderable.program().setTexture("depthImage", depthFBO.getDepthAttachmentTexture());
    	}
    	
    	for (int row = 0; row < textureSubdiv; row++)
    	{
	    	for (int col = 0; col < textureSubdiv; col++)
    		{
	    		renderable.program().setUniform("minTexCoord", 
	    				new Vector2((float)col / (float)textureSubdiv, (float)row / (float)textureSubdiv));
	    		
	    		renderable.program().setUniform("maxTexCoord", 
	    				new Vector2((float)(col+1) / (float)textureSubdiv, (float)(row+1) / (float)textureSubdiv));
	    		
	    		mainFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
	    		mainFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
	    		mainFBO.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
	    		mainFBO.clearDepthBuffer();
	    		renderable.draw(PrimitiveMode.TRIANGLES, mainFBO);
	    		
	    		callback.execute(mainFBO, row, col);
	    	}
		}
    	
    	mainFBO.delete();
    	
    	if (viewTexture != null)
    	{
    		viewTexture.delete();
    	}
    	
    	if (shadowFBO != null)
		{
    		shadowFBO.delete();
		}
    	
    	return depthFBO;
	}
	
	private static class SpecularParams
	{
		public final double reflectivity;
		public final double roughness;
		public final double remainder;
		
		public SpecularParams(double reflectivity, double roughness, double remainder)
		{
			this.reflectivity = reflectivity;
			this.roughness = roughness;
			this.remainder = remainder;
		}
	}
	
	private static double computeSumSqError(double roughnessSq, double reflectivity, double remainder, int directionalRes, double nDotHStart, 
			IntFunction<Vector3> colorSumLookup, IntFunction<Vector3> colorSquareSumLookup, 
			IntToDoubleFunction diffuseAlphaLookup, IntToDoubleFunction specularAlphaLookup, IntToDoubleFunction contributionSumLookup)
	{
		double sumSqError = 0.0;
		
		// Evaluate error
		for (int j = 0; j < directionalRes; j++)
		{
			double specularAlpha = specularAlphaLookup.applyAsDouble(j);
			if (specularAlpha > 0.0)
			{
				double coord = (double)j / (double)(directionalRes - 1);
				double nDotH = coord + (1 - coord) * nDotHStart;
				double contributionSum = contributionSumLookup.applyAsDouble(j);
				
				if (nDotH > 0.0 && contributionSum > 0.0)
				{
					double nDotHSquared = nDotH * nDotH;
					
					// Scaled by pi
//					double mfdEst = Math.exp((nDotHSquared - 1) / (nDotHSquared * roughnessSq))
//							/ (roughnessSq * nDotHSquared * nDotHSquared);
					
					// Optimize for GGX distribution
					double q1 = roughnessSq + (1.0 - nDotHSquared) / nDotHSquared;
					double mfdEst = roughnessSq / (nDotHSquared * nDotHSquared * q1 * q1);
					
//					double error = colorSumLookup.apply(j).y - specularAlpha * (remainder + reflectivity * mfdEst);
//					sumSqError += error * error;
					
					double fit = Math.pow((diffuseAlphaLookup.applyAsDouble(j) * remainder + specularAlpha * reflectivity * mfdEst) / contributionSum, FITTING_GAMMA_INV);
					double luminanceSum = colorSumLookup.apply(j).y;
					double sqError = colorSquareSumLookup.apply(j).y - 2 * luminanceSum * fit + contributionSum * fit * fit;
					sumSqError += sqError;
				}
			}
		}
		
		return sumSqError;
	}
	
	private SpecularParams computeSpecularParams(int directionalRes, double nDotHStart, 
			IntFunction<Vector3> colorSumLookup, IntFunction<Vector3> colorSquareSumLookup, 
			IntToDoubleFunction diffuseWeightSumLookup, IntToDoubleFunction specularWeightSumLookup, IntToDoubleFunction contributionSumLookup,
			File mfdFile) throws IOException
	{
		PrintStream mfdStream = new PrintStream(mfdFile);
		
		// Integral of Specular reflectivity
		// 		times microfacet probability
		//		times microfacet slope squared
		// 		times d [n dot h]
		// as [n dot h] goes from [n dot h]_min to 1
		double specularWeightedSlopeSum = 0.0;
		
		// Integral to find Specular reflectivity (low res)
		double specularReflectivitySum = 0.0;
		
		double contributionSum;
		double specularWeightSum;
		double nDotH;
		
		final double SQRT_HALF = Math.sqrt(0.5);
		
		// In case the first step(s) are missing
		int j = 0;
		do
		{
			contributionSum = contributionSumLookup.applyAsDouble(j);
			specularWeightSum = specularWeightSumLookup.applyAsDouble(j);
			double coord = (double)j / (double)(directionalRes - 1);
			nDotH = coord + (1 - coord) * nDotHStart;
			
			if (specularWeightSum == 0.0)
			{
				mfdStream.println(nDotH + ",0.0,0.0,0.0,0.0");
			}
			else if (nDotH < SQRT_HALF)
			{
				mfdStream.print(nDotH + ",");
				mfdStream.print(Math.max(0.0, colorSumLookup.apply(j).x) / specularWeightSum + ",");
				mfdStream.print(Math.max(0.0, colorSumLookup.apply(j).y) / specularWeightSum + ",");
				mfdStream.print(Math.max(0.0, colorSumLookup.apply(j).z) / specularWeightSum + ",");
				mfdStream.println(contributionSum);
			}
			
			j++;
		}
		while (j < directionalRes-1 && (specularWeightSum == 0.0 || nDotH < SQRT_HALF));
		
		specularWeightSum = specularWeightSumLookup.applyAsDouble(j);
		double lastWeight = colorSumLookup.apply(j).y / specularWeightSum;
		
		double coord = (double)j / (double)(directionalRes - 1);
		double lastNDotH = coord + (1 - coord) * nDotHStart;
		
		mfdStream.print(lastNDotH + ",");

		mfdStream.print(Math.max(0.0, colorSumLookup.apply(j).x) / specularWeightSum + ",");
		mfdStream.print(Math.max(0.0, colorSumLookup.apply(j).y) / specularWeightSum + ",");
		mfdStream.print(Math.max(0.0, colorSumLookup.apply(j).z) / specularWeightSum + ",");
		mfdStream.println(contributionSum);
		
//		if (lastNDotH > 0.0)
//		{
//			specularWeightedSlopeSum += lastWeight * (1 / lastNDotH - lastNDotH) * (lastNDotH - nDotHStart);
//			specularReflectivitySum += lastWeight * lastNDotH * (lastNDotH - nDotHStart);
//		}
//		else
		{
			lastWeight = 0.0;
		}
		
		int intervalCount = 0;
		
		for (j++; j < directionalRes; j++)
		{
			specularWeightSum = specularWeightSumLookup.applyAsDouble(j);
			
			coord = (double)j / (double)(directionalRes - 1);
			nDotH = coord + (1 - coord) * nDotHStart;
			
			if (specularWeightSum <= 0.0)
			{
				mfdStream.println(nDotH + ",0.0,0.0,0.0,0.0");
			}
			else
			{
				double weight = Math.max(0.0, colorSumLookup.apply(j).y) / specularWeightSum;

				mfdStream.print(nDotH + ",");

				contributionSum = contributionSumLookup.applyAsDouble(j);
				mfdStream.print(Math.max(0.0, colorSumLookup.apply(j).x) / specularWeightSum + ",");
				mfdStream.print(Math.max(0.0, colorSumLookup.apply(j).y) / specularWeightSum + ",");
				mfdStream.print(Math.max(0.0, colorSumLookup.apply(j).z) / specularWeightSum + ",");
				mfdStream.println(contributionSum);
				
				// Trapezoidal rule for integration
				if (lastNDotH > 0.0)
				{
					specularWeightedSlopeSum += lastWeight * (1 / lastNDotH - lastNDotH) / 2 * (nDotH - lastNDotH);
				}
				
				if (nDotH > 0.0)
				{
					specularWeightedSlopeSum += weight * (1 / nDotH - nDotH) / 2 * (nDotH - lastNDotH);
				}
				else
				{
					weight = 0.0;
				}
				
				specularReflectivitySum += (weight * nDotH + lastWeight * lastNDotH) / 2 * (nDotH - lastNDotH);
				
				intervalCount++;
				
				lastWeight = weight;
				lastNDotH = nDotH;
			}
		}
		
		// In case the final step(s) were missing
		specularWeightedSlopeSum += lastWeight * (1 / lastNDotH - lastNDotH) * (1.0 - lastNDotH);
		specularReflectivitySum += lastWeight * lastNDotH * (1.0 - lastNDotH);
		
		mfdStream.close();
		
		if (intervalCount > 0 && specularReflectivitySum > 0.0 && specularWeightedSlopeSum > 0.0)
		{
			double roughnessSq = specularWeightedSlopeSum / specularReflectivitySum;
			
			if (roughnessSq > 0.5)
			{
				roughnessSq = 0.5;
			}
			
			// Assuming scaling by 1 / pi
			double reflectivity = 2 * specularReflectivitySum;
			
			if (reflectivity > 1.0)
			{
				reflectivity = 1.0;
			}
			
			if (!param.isLevenbergMarquardtOptimizationEnabled())
			{
				return new SpecularParams(reflectivity, Math.sqrt(roughnessSq), 0.0);
			}
			else
			{
				double remainder = 0.0;
				
				double sumSqError;
				double nextSumSqError = computeSumSqError(roughnessSq, reflectivity, remainder, directionalRes, nDotHStart, 
						colorSumLookup, colorSquareSumLookup, diffuseWeightSumLookup, specularWeightSumLookup, contributionSumLookup);
				
				int i = 0;
				double deltaRoughnessSq;
				double deltaReflectivity;
				double deltaRemainder;
				double shiftFraction = 1.0;
				
				do
				{
					// Solving for parameter vector: [ roughness^2, reflectivity, diffuse ]
					DoubleMatrix3 jacobianSquared = new DoubleMatrix3(0.0f);
					DoubleVector3 jacobianTimesResiduals = new DoubleVector3(0.0f);
					
					sumSqError = nextSumSqError;
					
					for (j = 0; j < directionalRes; j++)
					{
						specularWeightSum = specularWeightSumLookup.applyAsDouble(j);
						if (specularWeightSum > 0.0)
						{
							coord = (double)j / (double)(directionalRes - 1);
							nDotH = coord + (1 - coord) * nDotHStart;
							contributionSum = contributionSumLookup.applyAsDouble(j);
							
							if (nDotH > 0.0 && contributionSum > 0.0 )
							{
								double nDotHSquared = nDotH * nDotH;
								
								// Scaled by pi
	//							double mfdEst = Math.exp((nDotHSquared - 1) / (nDotHSquared * roughnessSq))
	//									/ (roughnessSq * nDotHSquared * nDotHSquared);
	//							
	//							double mfdDeriv = -mfdEst * (nDotHSquared * (roughnessSq + 1) - 1) 
	//									/ (roughnessSq * roughnessSq * nDotHSquared);
								
								// Optimize for GGX distribution
								double q1 = roughnessSq + (1.0 - nDotHSquared) / nDotHSquared;
								double mfdEst = roughnessSq / (nDotHSquared * nDotHSquared * q1 * q1);
								double q2 = 1.0 + (roughnessSq - 1.0) * nDotHSquared;
								double mfdDeriv = (1.0 - (roughnessSq + 1.0) * nDotHSquared) / (q2 * q2 * q2);
										
	//							double residualWeighted = (colorSumLookup.apply(j).y - specularWeightSum * (remainder + reflectivity * mfdEst));
	//							DoubleVector3 derivsWeighted = new DoubleVector3(specularWeightSum * reflectivity * mfdDeriv, specularWeightSum * mfdEst, specularWeightSum);
	
								double diffuseWeightSum = diffuseWeightSumLookup.applyAsDouble(j);
								double currentFit = (diffuseWeightSum * remainder + reflectivity * mfdEst * specularWeightSum) / contributionSum;
								double residualWeighted = colorSumLookup.apply(j).y // already gamma corrected
										- contributionSum * Math.pow(currentFit, FITTING_GAMMA_INV);
								
								double dw = FITTING_GAMMA_INV * Math.pow(currentFit, FITTING_GAMMA_INV - 1);
								DoubleVector3 derivsWeighted = new DoubleVector3(
										dw * specularWeightSum * reflectivity * mfdDeriv, 
										dw * specularWeightSum * mfdEst, 
										dw * diffuseWeightSum);
								
								DoubleVector3 derivsUnweighted = derivsWeighted.times(1.0 / contributionSum);
								
								jacobianSquared = jacobianSquared.plus(derivsUnweighted.outerProduct(derivsWeighted));
								jacobianTimesResiduals = jacobianTimesResiduals.plus(derivsUnweighted.times(residualWeighted));
							}
						}
					}
					
					DoubleMatrix3 jacobianSquaredInverse = jacobianSquared.inverse();
					DoubleMatrix3 identityTest = jacobianSquaredInverse.times(jacobianSquared);
					
					DoubleVector3 paramDelta = jacobianSquared.inverse().times(jacobianTimesResiduals);
					deltaRoughnessSq = shiftFraction * paramDelta.x;
					deltaReflectivity = shiftFraction * paramDelta.y;
					deltaRemainder = shiftFraction * paramDelta.z;
					double nextRoughnessSq = roughnessSq + deltaRoughnessSq;
					double nextReflectivity = reflectivity + deltaReflectivity;
					double nextRemainder = remainder + deltaRemainder; // TODO is "remainder" even necessary?
					
					while(nextRoughnessSq < 0.0 || nextReflectivity < 0.0)
					{
						shiftFraction /= 2;
						deltaRoughnessSq /= 2;
						deltaReflectivity /= 2;
						deltaRemainder /= 2;
	
						nextRoughnessSq = roughnessSq + deltaRoughnessSq;
						nextReflectivity = reflectivity + deltaReflectivity;
						nextRemainder = remainder + deltaRemainder;
					}
					
					nextSumSqError = computeSumSqError(nextRoughnessSq, nextReflectivity, nextRemainder, directionalRes, nDotHStart, 
							colorSumLookup, colorSquareSumLookup, diffuseWeightSumLookup, specularWeightSumLookup, contributionSumLookup);
					
					while(nextSumSqError > sumSqError)
					{
						shiftFraction /= 2;
						deltaRoughnessSq /= 2;
						deltaReflectivity /= 2;
						deltaRemainder /= 2;
	
						nextRoughnessSq = roughnessSq + deltaRoughnessSq;
						nextReflectivity = reflectivity + deltaReflectivity;
						nextRemainder = remainder + deltaRemainder;
						
						nextSumSqError = computeSumSqError(nextRoughnessSq, nextReflectivity, nextRemainder, directionalRes, nDotHStart, 
								colorSumLookup, colorSquareSumLookup, diffuseWeightSumLookup, specularWeightSumLookup, contributionSumLookup);
					}
					
					roughnessSq = nextRoughnessSq;
					reflectivity = nextReflectivity;
					remainder = nextRemainder;
				}
				while(++i < 100 && (sumSqError - nextSumSqError) / sumSqError > 0.001);
	
				double roughness = Math.sqrt(roughnessSq);
				return new SpecularParams(reflectivity, roughness, remainder);
			}
		}
		else
		{
			return new SpecularParams(0.0, 1.0, 0.0);
		}
	}
	
	private SpecularParams globalSpecularFit(ViewSet<ContextType> viewSet, Texture<ContextType> diffuseEstimate, Texture<ContextType> normalObjSpEstimate) throws IOException
	{
		int directionalRes = 4096;
		
		double[][] colorSums = new double[directionalRes][3];
		double[][] colorSquareSums = new double[directionalRes][3];
		double[] diffuseWeightSums = new double[directionalRes];
		double[] specularWeightSums = new double[directionalRes];
		double[] contributionSums = new double[directionalRes];
		
		System.out.println("Sampling views...");
		
		if (diffuseEstimate != null && normalObjSpEstimate != null)
		{
			System.out.println("Using diffuse estimate to compute specular residuals.");
			specularResidProgram.setTexture("diffuseEstimate", diffuseEstimate);
			specularResidProgram.setTexture("normalEstimate", normalObjSpEstimate);
			specularResidProgram.setUniform("useDiffuseEstimate", true);
		}
		else
		{
			specularResidProgram.setUniform("useDiffuseEstimate", false);
		}
		
		for (int k = 0; k < viewSet.getCameraPoseCount(); k++)
		{
			final int K = k;
			
			FramebufferObject<ContextType> depthFBO = projectIntoTextureSpace(viewSet, specularResidProgram, k, param.getTextureSize(), 1, !param.isImagePreprojectionUseEnabled(),
				(framebuffer, row, col) -> 
				{
					if (param.isDebugModeEnabled())
			    	{
						try
	    				{
	    					framebuffer.saveColorBufferToFile(0, "PNG", new File(new File(auxDir, "specularResidDebug"), String.format("colors%04d.png", K)));
	    					framebuffer.saveColorBufferToFile(1, "PNG", new File(new File(auxDir, "specularResidDebug"), String.format("geom%04d.png", K)));
	    				}
	    				catch (IOException e)
	    				{
	    					e.printStackTrace();
	    				}
			    	}
					
					int partitionSize = param.getTextureSize();
					
					float[] colorData = framebuffer.readFloatingPointColorBufferRGBA(0);
					float[] geomData = framebuffer.readFloatingPointColorBufferRGBA(1);
						
					for (int y = 0; y < partitionSize; y++)
					{
						for (int x = 0; x < partitionSize; x++)
						{
							float colorX = (float)Math.pow(Math.max(0.0, colorData[((y*partitionSize) + x) * 4 + 0]), FITTING_GAMMA_INV);
							float colorY = (float)Math.pow(Math.max(0.0, colorData[((y*partitionSize) + x) * 4 + 1]), FITTING_GAMMA_INV);
							float colorZ = (float)Math.pow(Math.max(0.0, colorData[((y*partitionSize) + x) * 4 + 2]), FITTING_GAMMA_INV);
							float xSquared = colorX * colorX;
							float ySquared = colorY * colorY;
							float zSquared = colorZ * colorZ;
							float nDotL = colorData[((y*partitionSize) + x) * 4 + 3];
							float nDotH =  geomData[((y*partitionSize) + x) * 4 + 2];
							float geomRatio = geomData[((y*partitionSize) + x) * 4 + 3];
							
							if (nDotL > 0.0f && nDotH > 0.0f && geomRatio > 0.0f)
							{
								double bin = (directionalRes-1) * nDotH; //(directionalRes-1) * 2 * (hDotN - 0.5)
			
								int binFloor = (int)Math.floor(bin);
								int binCeil = (int)Math.ceil(bin);
								
								double s, t;
			
								if (binFloor >= 0 && binCeil < directionalRes)
								{
									if (binFloor == binCeil)
									{
										t = 0.0;
									}
									else
									{
										t = (bin - binFloor);
									}
									
									s = 1.0 - t;
									
									colorSums[binFloor][0] += s * colorX;
									colorSums[binCeil][0] += t * colorX;
									
									colorSums[binFloor][1] += s * colorY;
									colorSums[binCeil][1] += t * colorY;
									
									colorSums[binFloor][2] += s * colorZ;
									colorSums[binCeil][2] += t * colorZ;
									
									colorSquareSums[binFloor][0] += s * xSquared;
									colorSquareSums[binCeil][0] += t * xSquared;
									
									colorSquareSums[binFloor][1] += s * ySquared;
									colorSquareSums[binCeil][1] += t * ySquared;
									
									colorSquareSums[binFloor][2] += s * zSquared;
									colorSquareSums[binCeil][2] += t * zSquared;
									
									diffuseWeightSums[binFloor] += s * nDotL;
									diffuseWeightSums[binCeil] += t * nDotL;
									
									specularWeightSums[binFloor] += s * nDotL * geomRatio;
									specularWeightSums[binCeil] += t * nDotL * geomRatio;
									
									contributionSums[binFloor] += s;
									contributionSums[binFloor] += t;
								}
							}
						}
					}
				});
			

	    	depthFBO.delete();
			
	    	System.out.println("Completed " + (k+1) + "/" + viewSet.getCameraPoseCount() + " views...");
		}
		
		return computeSpecularParams(directionalRes, 0.0, 
				(j) -> new Vector3((float)colorSums[j][0], (float)colorSums[j][1], (float)colorSums[j][2]), 
				(j) -> new Vector3((float)colorSquareSums[j][0], (float)colorSquareSums[j][1], (float)colorSquareSums[j][2]), 
				(j) -> diffuseWeightSums[j], 
				(j) -> specularWeightSums[j], 
				(j) -> contributionSums[j],
				new File(auxDir, "mfd.csv"));
	}
	
	private void loadMesh() throws IOException
	{
		VertexMesh mesh = new VertexMesh("OBJ", objFile);
    	positionBuffer = context.createVertexBuffer().setData(mesh.getVertices());
    	texCoordBuffer = context.createVertexBuffer().setData(mesh.getTexCoords());
    	normalBuffer = context.createVertexBuffer().setData(mesh.getNormals());
    	tangentBuffer = context.createVertexBuffer().setData(mesh.getTangents());
    	center = mesh.getCentroid();
    	materialFileName = mesh.getMaterialFileName();
    	if (materialFileName == null)
    	{
    		materialFileName = objFile.getName().split("\\.")[0] + ".mtl";
    	}
    	materialName = mesh.getMaterialName();
    	if (materialName == null)
    	{
    		materialName = materialFileName.split("\\.")[0];
    	}
    	mesh = null;
    	System.gc(); // Garbage collect the mesh object (hopefully)
	}
	
	private Renderable<ContextType> getLightFitRenderable()
	{
		Renderable<ContextType> renderable = context.createRenderable(lightFitProgram);
    	
        renderable.addVertexBuffer("position", positionBuffer);
        renderable.addVertexBuffer("texCoord", texCoordBuffer);
        renderable.addVertexBuffer("normal", normalBuffer);
    	
        renderable.program().setUniform("viewCount", viewSet.getCameraPoseCount());
        renderable.program().setUniform("gamma", param.getGamma());
        if (viewSet.getLuminanceMap() == null)
        {
        	renderable.program().setUniform("useLuminanceMap", false);
        }
        else
        {
        	renderable.program().setUniform("useLuminanceMap", true);
        	renderable.program().setTexture("luminanceMap", viewSet.getLuminanceMap());
        }
        renderable.program().setUniform("shadowTestEnabled", false);
        renderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
        renderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
        renderable.program().setUniform("infiniteLightSources", param.areLightSourcesInfinite());
    	
        renderable.program().setUniformBuffer("CameraPoses", viewSet.getCameraPoseBuffer());
    	
    	if (!param.isImagePreprojectionUseEnabled())
    	{
    		renderable.program().setUniformBuffer("CameraProjections", viewSet.getCameraProjectionBuffer());
    		renderable.program().setUniformBuffer("CameraProjectionIndices", viewSet.getCameraProjectionIndexBuffer());
    	}
    	
    	renderable.program().setUniformBuffer("LightIndices", viewSet.getLightIndexBuffer());
    	
    	renderable.program().setUniform("delta", param.getDiffuseDelta());
    	renderable.program().setUniform("iterations", 1/*param.getDiffuseIterations()*/); // TODO rework light fitting
    	
    	return renderable;
	}
	
	private /*static*/ abstract class LightFit<ContextType extends Context<ContextType>>
	{
		private Renderable<ContextType> renderable;
		final int framebufferSize;
		final int framebufferSubdiv;
		
		private Vector3 position;
		private Vector3 intensity;
		
		Vector3 getPosition()
		{
			return position;
		}
		
		Vector3 getIntensity()
		{
			return intensity;
		}
		
		protected abstract void fitTexture(Renderable<ContextType> renderable, Framebuffer<ContextType> framebuffer) throws IOException;
		
		LightFit(Renderable<ContextType> renderable, int framebufferSize, int framebufferSubdiv)
		{
	    	this.renderable = renderable;
	    	this.framebufferSize = framebufferSize;
	    	this.framebufferSubdiv = framebufferSubdiv;
		}
    	
    	void fit(int lightIndex) throws IOException
    	{
    		FramebufferObject<ContextType> framebuffer = 
				renderable.getContext().getFramebufferObjectBuilder(framebufferSize, framebufferSize)
					.addColorAttachments(new ColorAttachmentSpec(ColorFormat.RGBA32F), 2)
					.createFramebufferObject();

    		framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
	    	framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
        	
        	renderable.program().setUniform("lightIndex", lightIndex);
        	fitTexture(renderable, framebuffer);

    		framebuffer.saveColorBufferToFile(1, "PNG", new File(auxDir, "lightDebug.png"));
    	
	    	System.out.println("Aggregating light estimates...");
	    	
	    	float[] rawLightPositions = framebuffer.readFloatingPointColorBufferRGBA(0);
	        float[] rawLightIntensities = framebuffer.readFloatingPointColorBufferRGBA(1);
	        
	        framebuffer.delete(); // No need for this anymore
	        
	        Vector4 lightPositionSum = new Vector4(0, 0, 0, 0);
	        Vector4 lightIntensitySum = new Vector4(0, 0, 0, 0);
	        
	        for (int i = 0; i < framebuffer.getSize().height; i++)
	        {
	        	for (int j = 0; j < framebuffer.getSize().width; j++)
	        	{
	        		int indexStart = (i * framebuffer.getSize().width + j) * 4;
	        		lightPositionSum = lightPositionSum.plus(
	        				new Vector4(rawLightPositions[indexStart+0], rawLightPositions[indexStart+1], rawLightPositions[indexStart+2], 1.0f)
	        					.times(rawLightPositions[indexStart+3]));
	        		lightIntensitySum = lightIntensitySum.plus(
	        				new Vector4(rawLightIntensities[indexStart+0], rawLightIntensities[indexStart+1], rawLightIntensities[indexStart+2], 1.0f)
	        					.times(rawLightIntensities[indexStart+3]));
	        	}
	        }
	        
	        position = new Vector3(lightPositionSum.dividedBy(lightPositionSum.w));
	        intensity = new Vector3(lightIntensitySum.dividedBy(lightIntensitySum.w));
    	}
	}
	
	private /*static*/ class TexSpaceLightFit<ContextType extends Context<ContextType>> extends LightFit<ContextType>
	{
		private File preprojDir;
		private int preprojCount;
		
		TexSpaceLightFit(Renderable<ContextType> renderable, File preprojDir, int preprojCount, int framebufferSize, int framebufferSubdiv)
		{
			super(renderable, framebufferSize, framebufferSubdiv);
			this.preprojDir = preprojDir;
			this.preprojCount = preprojCount;
		}
		
		@Override
		protected void fitTexture(Renderable<ContextType> renderable, Framebuffer<ContextType> framebuffer) throws IOException
		{
			int subdivSize = framebufferSize / framebufferSubdiv;
    		
    		for (int row = 0; row < framebufferSubdiv; row++)
	    	{
		    	for (int col = 0; col < framebufferSubdiv; col++)
	    		{
		    		Texture3D<ContextType> preprojectedViews = null;
			    	
			    	preprojectedViews = renderable.getContext().get2DColorTextureArrayBuilder(subdivSize, subdivSize, preprojCount).createTexture();
				    	
					for (int i = 0; i < preprojCount; i++)
					{
						preprojectedViews.loadLayer(i, new File(new File(preprojDir, String.format("%04d", i)), String.format("r%04dc%04d.png", row, col)), true);
					}
		    		
		    		renderable.program().setTexture("viewImages", preprojectedViews);
			    	
			    	renderable.program().setUniform("minTexCoord", 
		    				new Vector2((float)col / (float)framebufferSubdiv, (float)row / (float)framebufferSubdiv));
		    		
			    	renderable.program().setUniform("maxTexCoord", 
		    				new Vector2((float)(col+1) / (float)framebufferSubdiv, (float)(row+1) / (float)framebufferSubdiv));
			    	
			    	renderable.draw(PrimitiveMode.TRIANGLES, framebuffer, col * subdivSize, row * subdivSize, subdivSize, subdivSize);
			        renderable.getContext().finish();
			        
			        if (framebufferSubdiv > 1)
		    		{
		    			System.out.println("Block " + (row*framebufferSubdiv + col + 1) + "/" + (framebufferSubdiv * framebufferSubdiv) + " completed.");
		    		}
	    		}
	    	}
		}
	}
	
	private /*static*/ class ImgSpaceLightFit<ContextType extends Context<ContextType>> extends LightFit<ContextType>
	{
		private Texture<ContextType> viewTextures;
		private Texture<ContextType> depthTextures;
		
		ImgSpaceLightFit(Renderable<ContextType> renderable, Texture<ContextType> viewTextures, Texture<ContextType> depthTextures, int framebufferSize, int framebufferSubdiv)
		{
			super(renderable, framebufferSize, framebufferSubdiv);
			
			this.viewTextures = viewTextures;
			this.depthTextures = depthTextures;
		}
		
		@Override
		protected void fitTexture(Renderable<ContextType> renderable, Framebuffer<ContextType> framebuffer)
		{
			int subdivSize = framebufferSize / framebufferSubdiv;
    		
    		for (int row = 0; row < framebufferSubdiv; row++)
	    	{
		    	for (int col = 0; col < framebufferSubdiv; col++)
	    		{
			    	renderable.program().setTexture("viewImages", viewTextures);
		    		renderable.program().setTexture("depthImages", depthTextures);
			    	
			    	renderable.program().setUniform("minTexCoord", 
		    				new Vector2((float)col / (float)framebufferSubdiv, (float)row / (float)framebufferSubdiv));
		    		
			    	renderable.program().setUniform("maxTexCoord", 
		    				new Vector2((float)(col+1) / (float)framebufferSubdiv, (float)(row+1) / (float)framebufferSubdiv));
			    	
			    	renderable.draw(PrimitiveMode.TRIANGLES, framebuffer, col * subdivSize, row * subdivSize, subdivSize, subdivSize);
			        renderable.getContext().finish();
			        
			        if (framebufferSubdiv > 1)
		    		{
		    			System.out.println("Block " + (row*framebufferSubdiv + col + 1) + "/" + (framebufferSubdiv * framebufferSubdiv) + " completed.");
		    		}
	    		}
	    	}
		}
	}
	
	private LightFit<ContextType> createTexSpaceLightFit(int framebufferSize, int framebufferSubdiv)
	{
		return new TexSpaceLightFit<ContextType>(getLightFitRenderable(), tmpDir, viewSet.getCameraPoseCount(), framebufferSize, framebufferSubdiv);
	}
	
	private LightFit<ContextType> createImgSpaceLightFit(Texture<ContextType> viewTextures, Texture<ContextType> depthTextures, int framebufferSize, int framebufferSubdiv)
	{
		return new ImgSpaceLightFit<ContextType>(getLightFitRenderable(), viewTextures, depthTextures, framebufferSize, framebufferSubdiv);
	}
	
	private void setupCommonShaderInputs(Renderable<ContextType> renderable)
	{
		renderable.addVertexBuffer("position", positionBuffer);
    	renderable.addVertexBuffer("texCoord", texCoordBuffer);
    	renderable.addVertexBuffer("normal", normalBuffer);
    	renderable.addVertexBuffer("tangent", tangentBuffer);

    	renderable.program().setUniform("viewCount", viewSet.getCameraPoseCount());
    	renderable.program().setUniformBuffer("CameraPoses", viewSet.getCameraPoseBuffer());
    	
    	if (!param.isImagePreprojectionUseEnabled())
    	{
	    	renderable.program().setUniformBuffer("CameraProjections", viewSet.getCameraProjectionBuffer());
	    	renderable.program().setUniformBuffer("CameraProjectionIndices", viewSet.getCameraProjectionIndexBuffer());
    	}

    	renderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
    	renderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
    	renderable.program().setUniform("gamma", param.getGamma());
    	renderable.program().setUniform("fittingGamma", (float)FITTING_GAMMA);
    	renderable.program().setUniform("infiniteLightSources", param.areLightSourcesInfinite());

    	if (viewSet.getLuminanceMap() == null)
        {
        	renderable.program().setUniform("useLuminanceMap", false);
        }
        else
        {
        	renderable.program().setUniform("useLuminanceMap", true);
        	renderable.program().setTexture("luminanceMap", viewSet.getLuminanceMap());
        }
    	
		renderable.program().setUniformBuffer("LightIndices", viewSet.getLightIndexBuffer());
    	
    	if (lightPositionBuffer != null)
    	{
    		renderable.program().setUniformBuffer("LightPositions", lightPositionBuffer);
    	}
    	else
    	{
    		renderable.program().setUniformBuffer("LightPositions", viewSet.getLightPositionBuffer());
    	}
    	
    	if (lightIntensityBuffer != null)
    	{
    		renderable.program().setUniformBuffer("LightIntensities", lightIntensityBuffer);
    	}
    	else
    	{
    		renderable.program().setUniformBuffer("LightIntensities", viewSet.getLightIntensityBuffer());
    	}
    	
    	if (shadowMatrixBuffer != null)
    	{
    		renderable.program().setUniform("shadowTestEnabled", param.isCameraVisibilityTestEnabled());
    		renderable.program().setUniformBuffer("ShadowMatrices", shadowMatrixBuffer);
    	}
    	else
    	{
    		renderable.program().setUniform("shadowTestEnabled", false);
    	}
	}
	
	private static interface SubdivisionRenderingCallback
	{
		void execute(int row, int col) throws IOException;
	}
	
	private static class ParameterizedFitBase<ContextType extends Context<ContextType>>
	{
		final Renderable<ContextType> renderable;
		final int viewCount;
		final int subdiv;
		
		ParameterizedFitBase(Renderable<ContextType> renderable, int viewCount, int subdiv)
		{
			this.subdiv = subdiv;
			this.viewCount = viewCount;
	    	this.renderable = renderable;
		}
		
		private void fitSubdiv(Framebuffer<ContextType> framebuffer, int row, int col, 
				Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages)
		{
			int subdivWidth = framebuffer.getSize().width / subdiv;
			int subdivHeight = framebuffer.getSize().height / subdiv;
			
			renderable.program().setTexture("viewImages", viewImages);
	    	renderable.program().setTexture("depthImages", depthImages);
	    	renderable.program().setTexture("shadowImages", shadowImages);
	    	
	    	renderable.program().setUniform("minTexCoord", 
    				new Vector2((float)col / (float)subdiv, (float)row / (float)subdiv));
    		
	    	renderable.program().setUniform("maxTexCoord", 
    				new Vector2((float)(col+1) / (float)subdiv, (float)(row+1) / (float)subdiv));
	    	
	        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer, col * subdivWidth, row * subdivHeight, subdivWidth, subdivHeight);
	        renderable.getContext().finish();
		}
		
		void fitImageSpace(Framebuffer<ContextType> framebuffer, Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages, 
				SubdivisionRenderingCallback callback) throws IOException
		{
			if (this.subdiv == 1)
        	{
        		this.fitSubdiv(framebuffer, 0, 0, viewImages, depthImages, shadowImages);
        	}
        	else
        	{
        		for (int row = 0; row < this.subdiv; row++)
    	    	{
    		    	for (int col = 0; col < this.subdiv; col++)
    	    		{
    	        		this.fitSubdiv(framebuffer, row, col, viewImages, depthImages, shadowImages);
    	        		callback.execute(row, col);
    	    		}
    	    	}
        	}
		}
		
		void fitTextureSpace(Framebuffer<ContextType> framebuffer, File preprocessDirectory, SubdivisionRenderingCallback callback) throws IOException
		{
			int subdivWidth = framebuffer.getSize().width / subdiv;
			int subdivHeight = framebuffer.getSize().height / subdiv;
			
			if (this.subdiv == 1)
        	{
        		Texture3D<ContextType> preprojectedViews = 
    				renderable.getContext().get2DColorTextureArrayBuilder(subdivWidth, subdivHeight, viewCount).createTexture();
			    	
				for (int i = 0; i < this.viewCount; i++)
				{
					preprojectedViews.loadLayer(i, new File(new File(preprocessDirectory, String.format("%04d", i)), String.format("r%04dc%04d.png", 0, 0)), true);
				}
	    	
		    	this.fitSubdiv(framebuffer, 0, 0, preprojectedViews, null, null);
		        	
	        	preprojectedViews.delete();
        	}
        	else
        	{
        		for (int row = 0; row < this.subdiv; row++)
    	    	{
    		    	for (int col = 0; col < this.subdiv; col++)
    	    		{
    	        		Texture3D<ContextType> preprojectedViews = 
	        				renderable.getContext().get2DColorTextureArrayBuilder(subdivWidth, subdivHeight, viewCount).createTexture();
    				    	
						for (int i = 0; i < this.viewCount; i++)
						{
							preprojectedViews.loadLayer(i, new File(new File(preprocessDirectory, String.format("%04d", i)), String.format("r%04dc%04d.png", row, col)), true);
						}

    	        		this.fitSubdiv(framebuffer, row, col, preprojectedViews, null, null);
    	        		callback.execute(row, col);

			    		preprojectedViews.delete();
    	    		}
    	    	}
        	}
		}
	}
	
	private static class DiffuseFit<ContextType extends Context<ContextType>>
	{
		private final ParameterizedFitBase<ContextType> base;
		private final Framebuffer<ContextType> framebuffer;
		
		DiffuseFit(Renderable<ContextType> renderable, Framebuffer<ContextType> framebuffer, int viewCount, int subdiv)
		{
			this.framebuffer = framebuffer;
			base = new ParameterizedFitBase<ContextType>(renderable, viewCount, subdiv);
		}
		
		void fitImageSpace(Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages, SubdivisionRenderingCallback callback)
			throws IOException
		{
	    	base.fitImageSpace(framebuffer, viewImages, depthImages, shadowImages, callback);
		}
		
		void fitTextureSpace(File preprocessDirectory,  SubdivisionRenderingCallback callback) throws IOException
		{
	    	base.fitTextureSpace(framebuffer, preprocessDirectory, callback);
		}
	}
	
	private DiffuseFit<ContextType> createDiffuseFit(Framebuffer<ContextType> framebuffer, int viewCount, int subdiv)
	{
		Renderable<ContextType> renderable = context.createRenderable(diffuseFitProgram);
    	setupCommonShaderInputs(renderable);
    	renderable.program().setUniform("delta", param.getDiffuseDelta());
    	renderable.program().setUniform("iterations", param.getDiffuseIterations());
    	renderable.program().setUniform("fit1Weight", param.getDiffuseInputNormalWeight());
    	renderable.program().setUniform("fit3Weight", param.getDiffuseComputedNormalWeight());
		return new DiffuseFit<ContextType>(renderable, framebuffer, viewCount, subdiv);
	}
	
	private static class SpecularFit<ContextType extends Context<ContextType>>
	{
		private final ParameterizedFitBase<ContextType> base;
		private final Framebuffer<ContextType> framebuffer;
		
		SpecularFit(Renderable<ContextType> renderable, Framebuffer<ContextType> framebuffer, int viewCount, int subdiv)
		{
			this.framebuffer = framebuffer;
			base = new ParameterizedFitBase<ContextType>(renderable, viewCount, subdiv);
		}
		
		void fitImageSpace(Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages, 
				Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, Texture<ContextType> roughnessEstimate, SubdivisionRenderingCallback callback) throws IOException
		{
			base.renderable.program().setTexture("diffuseEstimate", diffuseEstimate);
			base.renderable.program().setTexture("normalEstimate", normalEstimate);
			base.renderable.program().setTexture("roughnessEstimate", roughnessEstimate);
	    	base.fitImageSpace(framebuffer, viewImages, depthImages, shadowImages, callback);
		}
		
		void fitTextureSpace(File preprocessDirectory, Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, Texture<ContextType> roughnessEstimate,
				SubdivisionRenderingCallback callback)
			throws IOException
		{
			base.renderable.program().setTexture("diffuseEstimate", diffuseEstimate);
			base.renderable.program().setTexture("normalEstimate", normalEstimate);
			base.renderable.program().setTexture("roughnessEstimate", roughnessEstimate);
	    	base.fitTextureSpace(framebuffer, preprocessDirectory, callback);
		}
	}
	
	private SpecularFit<ContextType> createSpecularFit(Framebuffer<ContextType> framebuffer, int viewCount, int subdiv)
	{
		Renderable<ContextType> renderable = context.createRenderable(specularFit2Program);
    	setupCommonShaderInputs(renderable);
		return new SpecularFit<ContextType>(renderable, framebuffer, viewCount, subdiv);
	}
	
	private static class AdjustFit<ContextType extends Context<ContextType>>
	{
		private final ParameterizedFitBase<ContextType> base;
		
		AdjustFit(Renderable<ContextType> renderable, int viewCount, int subdiv)
		{
			base = new ParameterizedFitBase<ContextType>(renderable, viewCount, subdiv);
		}
		
		void fitImageSpace(Framebuffer<ContextType> framebuffer, Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages, 
				Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, Texture<ContextType> specularEstimate, Texture<ContextType> roughnessEstimate,
				Texture<ContextType> errorTexture, SubdivisionRenderingCallback callback) throws IOException
		{
			base.renderable.program().setTexture("diffuseEstimate", diffuseEstimate);
			base.renderable.program().setTexture("normalEstimate", normalEstimate);
			base.renderable.program().setTexture("specularEstimate", specularEstimate);
			base.renderable.program().setTexture("roughnessEstimate", roughnessEstimate);
			base.renderable.program().setTexture("errorTexture", errorTexture);
	    	base.fitImageSpace(framebuffer, viewImages, depthImages, shadowImages, callback);
		}
		
		void fitTextureSpace(Framebuffer<ContextType> framebuffer, File preprocessDirectory, 
				Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, Texture<ContextType> specularEstimate, Texture<ContextType> roughnessEstimate,
				Texture<ContextType> errorTexture, SubdivisionRenderingCallback callback) throws IOException
		{
			base.renderable.program().setTexture("diffuseEstimate", diffuseEstimate);
			base.renderable.program().setTexture("normalEstimate", normalEstimate);
			base.renderable.program().setTexture("specularEstimate", specularEstimate);
			base.renderable.program().setTexture("roughnessEstimate", roughnessEstimate);
			base.renderable.program().setTexture("errorTexture", errorTexture);
	    	base.fitTextureSpace(framebuffer, preprocessDirectory, callback);
		}
	}
	
	private AdjustFit<ContextType> createAdjustFit(int viewCount, int subdiv)
	{
		Renderable<ContextType> renderable = context.createRenderable(adjustFitProgram);
    	setupCommonShaderInputs(renderable);
		return new AdjustFit<ContextType>(renderable, viewCount, subdiv);
	}
	
	private static class ErrorCalc<ContextType extends Context<ContextType>>
	{
		private final ParameterizedFitBase<ContextType> base;
		
		ErrorCalc(Renderable<ContextType> renderable, int viewCount, int subdiv)
		{
			base = new ParameterizedFitBase<ContextType>(renderable, viewCount, subdiv);
		}
		
		void fitImageSpace(Framebuffer<ContextType> framebuffer, Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages, 
				Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, Texture<ContextType> specularEstimate, Texture<ContextType> roughnessEstimate,
				Texture<ContextType> errorTexture, SubdivisionRenderingCallback callback) throws IOException
		{
			base.renderable.program().setTexture("diffuseEstimate", diffuseEstimate);
			base.renderable.program().setTexture("normalEstimate", normalEstimate);
			base.renderable.program().setTexture("specularEstimate", specularEstimate);
			base.renderable.program().setTexture("roughnessEstimate", roughnessEstimate);
			base.renderable.program().setTexture("errorTexture", errorTexture);
	    	base.fitImageSpace(framebuffer, viewImages, depthImages, shadowImages, callback);
		}
		
		void fitTextureSpace(Framebuffer<ContextType> framebuffer, File preprocessDirectory, 
				Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, Texture<ContextType> specularEstimate, Texture<ContextType> roughnessEstimate,
				Texture<ContextType> errorTexture, SubdivisionRenderingCallback callback) throws IOException
		{
			base.renderable.program().setTexture("diffuseEstimate", diffuseEstimate);
			base.renderable.program().setTexture("normalEstimate", normalEstimate);
			base.renderable.program().setTexture("specularEstimate", specularEstimate);
			base.renderable.program().setTexture("roughnessEstimate", roughnessEstimate);
			base.renderable.program().setTexture("errorTexture", errorTexture);
	    	base.fitTextureSpace(framebuffer, preprocessDirectory, callback);
		}
	}
	
	private ErrorCalc<ContextType> createErrorCalc(int viewCount, int subdiv)
	{
		Renderable<ContextType> renderable = context.createRenderable(errorCalcProgram);
    	setupCommonShaderInputs(renderable);
		return new ErrorCalc<ContextType>(renderable, viewCount, subdiv);
	}
	
//	private void removeNormalMapBias(float[] normalData, int normalMapSize, VertexMesh mesh)
//	{
//		VertexBuffer<ContextType> selectorBuffer = context.createVertexBuffer();
//		FloatVertexList selectors = new FloatVertexList(1, mesh.getTexCoords().count);
//		
//		FloatVertexList biasedNormals = new FloatVertexList(3, mesh.getTexCoords().count);
//		
//		Renderable<ContextType> vertexWeightRenderable = context.createRenderable(vertexColorProgram);
//		vertexWeightRenderable.addVertexBuffer("texCoord", texCoordBuffer);
//		vertexWeightRenderable.addVertexBuffer("colors", selectorBuffer);
//		
//		FramebufferObject<ContextType> vertexWeightFramebuffer = 
//			context.getFramebufferObjectBuilder(normalMapSize, normalMapSize)
//				.addColorAttachment(ColorFormat.R8)
//				.createFramebufferObject();
//		
//		for (int i = 0; i < mesh.getUniqueTexCoords().count; i++)
//		{
//			int[] texCoordInstances = mesh.getUniqueTexCoords().get(i);
//			
//			for (int j = 0; j < texCoordInstances.length; j++)
//			{
//				// Select the current vertex
//				selectors.set(j, 0, 1.0f);
//			}
//			
//			// Define a vertex buffer with zeros at every vertex except the current one
//			selectorBuffer.setData(selectors);
//			
//			vertexWeightFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
//			vertexWeightRenderable.draw(PrimitiveMode.TRIANGLES, vertexWeightFramebuffer);
//			float[] vertexWeightData = vertexWeightFramebuffer.readFloatingPointColorBufferRGBA(0);
//			
//			Vector3 weightedNormalSum = new Vector3(0.0f, 0.0f, 0.0f);
//			for (int j = 0; j < vertexWeightData.length; j++)
//			{
//				weightedNormalSum = weightedNormalSum
//					.plus(new Vector3(normalData[4*j + 0] - 0.5f, normalData[4*j + 1] - 0.5f, normalData[4*j + 2] - 0.5f)
//						.times(vertexWeightData[j]));
//			}
//			
//			Vector3 biasedNormal = weightedNormalSum.normalized();
//			
//			for (int j = 0; j < texCoordInstances.length; j++)
//			{
//				// Set the biased normal in all instances
//				biasedNormals.set(j, 0, biasedNormal.x);
//				biasedNormals.set(j, 1, biasedNormal.y);
//				biasedNormals.set(j, 2, biasedNormal.z);
//				
//				// Deselect the current vertex
//				selectors.set(j, 0, 0.0f);
//			}
//		}
//		
//		vertexWeightFramebuffer.delete();
//		selectorBuffer.delete();
//		
//		VertexBuffer<ContextType> biasedNormalBuffer = context.createVertexBuffer().setData(biasedNormals);
//		
//		Renderable<ContextType> biasedNormalRenderable = context.createRenderable(vertexColorProgram);
//		biasedNormalRenderable.addVertexBuffer("texCoord", texCoordBuffer);
//		biasedNormalRenderable.addVertexBuffer("colors", biasedNormalBuffer);
//		
//		FramebufferObject<ContextType> biasedNormalFramebuffer = 
//			context.getFramebufferObjectBuilder(normalMapSize, normalMapSize)
//				.addColorAttachment(ColorFormat.RGB32F)
//				.createFramebufferObject();
//		
//		biasedNormalFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
//		biasedNormalRenderable.draw(PrimitiveMode.TRIANGLES, biasedNormalFramebuffer);
//		float[] biasedNormalData = vertexWeightFramebuffer.readFloatingPointColorBufferRGBA(0);
//		
//		for (int i = 0; i < normalData.length; i+=4)
//		{
//			Vector3 detailNormal = new Vector3(normalData[i + 0] - 0.5f, normalData[i + 1] - 0.5f, normalData[i + 2] - 0.5f).normalized();
//			Vector3 biasedNormal = new Vector3(biasedNormalData[i + 0], biasedNormalData[i + 1], biasedNormalData[i + 2]).normalized();
//			
//			Vector3 unbiasedNormal = new Vector3(detailNormal.x - biasedNormal.x, detailNormal.y - biasedNormal.y, detailNormal.z - biasedNormal.z + 1.0f).normalized();
//			
//			// Write into the input array
//			normalData[i + 0] = unbiasedNormal.x * 0.5f + 0.5f;
//			normalData[i + 1] = unbiasedNormal.y * 0.5f + 0.5f;
//			normalData[i + 2] = unbiasedNormal.z * 0.5f + 0.5f;
//		}
//
//		biasedNormalFramebuffer.delete();
//		biasedNormalBuffer.delete();
//	}
	
	private double getLinearDepth(double nonLinearDepth, double nearPlane, double farPlane)
	{
		return 2 * nearPlane * farPlane / (farPlane + nearPlane - nonLinearDepth * (farPlane - nearPlane));
	}
	
	private double loadTextures() throws IOException
	{
		if (param.isImagePreprojectionUseEnabled() && param.isImagePreprojectionGenerationEnabled())
    	{
    		System.out.println("Pre-projecting images into texture space...");
	    	Date timestamp = new Date();
	    	
	    	tmpDir.mkdir();
	    	
    		double minDepth = viewSet.getRecommendedFarPlane();
	    	
	    	for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
	    	{
	    		File viewDir = new File(tmpDir, String.format("%04d", i));
	        	viewDir.mkdir();
	        	
	    		FramebufferObject<ContextType> depthFBO = projectIntoTextureSpace(viewSet, projTexProgram, i, param.getTextureSize(), param.getTextureSubdivision(), false,
	    			(framebuffer, row, col) -> 
    				{
	    				try
	    				{
	    					framebuffer.saveColorBufferToFile(0, "PNG", new File(viewDir, String.format("r%04dc%04d.png", row, col)));
	    					if (param.isDebugModeEnabled())
	    					{
	    						framebuffer.saveColorBufferToFile(1, "PNG", new File(viewDir, String.format("geomInfo_r%04dc%04d.png", row, col)));
	    					}
	    				}
	    				catch (IOException e)
	    				{
	    					e.printStackTrace();
	    				}
					});
	    		
	    		if (i == viewSet.getPrimaryViewIndex())
    			{
		    		short[] depthBufferData = depthFBO.readDepthBuffer();
		        	for (int j = 0; j < depthBufferData.length; j++)
		        	{
		        		int nonlinearDepth = 0xFFFF & (int)depthBufferData[j];
		        		minDepth = Math.min(minDepth, getLinearDepth((double)nonlinearDepth / 0xFFFF, viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
		        	}
    			}

	        	depthFBO.delete();
		    	
		    	System.out.println("Completed " + (i+1) + "/" + viewSet.getCameraPoseCount());
	    	}
	    	
	    	System.out.println("Pre-projections completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	    	
	    	return minDepth;
    	}
    	else if (!param.isImagePreprojectionUseEnabled())
    	{
    		if (param.isImageRescalingEnabled())
    		{
    			System.out.println("Rescaling images...");
    			Date timestamp = new Date();
    			
				// Create an FBO for downsampling
		    	FramebufferObject<ContextType> downsamplingFBO = 
	    			context.getFramebufferObjectBuilder(param.getImageWidth(), param.getImageHeight())
	    				.addColorAttachment()
	    				.createFramebufferObject();
		    	
		    	Renderable<ContextType> downsampleRenderable = context.createRenderable(textureRectProgram);
		    	VertexBuffer<ContextType> rectBuffer = context.createRectangle();
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
		    		
		    		Texture2D<ContextType> fullSizeImage;
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
		        	
					System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " images rescaled.");
		    	}
	
		    	rectBuffer.delete();
		    	downsamplingFBO.delete();
		    	
	    		System.out.println("Rescaling completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	    		
	    		// Use rescale directory in the future
	    		imageDir = rescaleDir;
	    		rescaleDir = null;
	    		maskDir = null;
    		}
    		
    		System.out.println("Loading images...");
	    	Date timestamp = new Date();
	    	
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
							.setInternalFormat(CompressionFormat.RGB_PUNCHTHROUGH_ALPHA1_4BPP)
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
    		
    		System.out.println("Creating depth maps...");
	    	timestamp = new Date();
	    	
	    	// Build depth textures for each view
	    	int width = viewTextures.getWidth() / 2;
	    	int height = viewTextures.getHeight() / 2;
	    	depthTextures = context.get2DDepthTextureArrayBuilder(width, height, viewSet.getCameraPoseCount()).createTexture();
	    	
	    	// Don't automatically generate any texture attachments for this framebuffer object
	    	FramebufferObject<ContextType> depthRenderingFBO = context.getFramebufferObjectBuilder(width, height).createFramebufferObject();
	    	
	    	Renderable<ContextType> depthRenderable = context.createRenderable(depthRenderingProgram);
	    	depthRenderable.addVertexBuffer("position", positionBuffer);

    		double minDepth = viewSet.getRecommendedFarPlane();
	    	
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
	        	
	        	if (i == viewSet.getPrimaryViewIndex())
	        	{
		        	short[] depthBufferData = depthRenderingFBO.readDepthBuffer();
		        	for (int j = 0; j < depthBufferData.length; j++)
		        	{
		        		int nonlinearDepth = 0xFFFF & (int)depthBufferData[j];
		        		minDepth = Math.min(minDepth, getLinearDepth((double)nonlinearDepth / 0xFFFF, viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
		        	}
	        	}
	        	
				//System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " depth maps created.");
	    	}

	    	depthRenderingFBO.delete();
	    	
    		System.out.println("Depth maps created in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	    	
	    	return minDepth;
    	}
    	else
    	{
    		return -1.0;
    	}
	}
	
	public void fitLightSource(double avgDistance) throws IOException
	{
		if (!param.areLightSourcesInfinite())
		{
			if (param.isLightOffsetEstimationEnabled())
	    	{
		    	System.out.println("Beginning light fit...");
		    	
	    		Vector3 lightIntensity = new Vector3((float)(avgDistance * avgDistance));
		        System.out.println("Using light intensity: " + lightIntensity.x + " " + lightIntensity.y + " " + lightIntensity.z);
		        
		    	Date timestamp = new Date();
				
		    	FloatVertexList lightPositionList = new FloatVertexList(4, viewSet.getLightCount());
		        FloatVertexList lightIntensityList = new FloatVertexList(3, viewSet.getLightCount());
		        
	    		LightFit<ContextType> lightFit;
		    	
		    	if (param.isImagePreprojectionUseEnabled())
		    	{
		    		lightFit = createTexSpaceLightFit(param.getTextureSize(), param.getTextureSubdivision());
		    	}
		    	else
		    	{
		    		lightFit = createImgSpaceLightFit(viewTextures, depthTextures, param.getTextureSize(), param.getTextureSubdivision());
		    	}
		        
		    	for (int i = 0; i < viewSet.getLightCount(); i++)
		    	{
		    		System.out.println("Fitting light " + i + "...");
		
		    		lightFit.fit(i);
		    		
		    		Vector3 lightPosition = lightFit.getPosition();
		    		//lightIntensity = lightFit.getIntensity();
			        
			        System.out.println("Light position: " + lightPosition.x + " " + lightPosition.y + " " + lightPosition.z);
			        System.out.println("(Light intensity from fit: " + lightFit.getIntensity().x + " " + lightFit.getIntensity().y + " " + lightFit.getIntensity().z + ")");
		    		
			    	lightPositionList.set(i, 0, lightPosition.x);
			    	lightPositionList.set(i, 1, lightPosition.y);
			    	lightPositionList.set(i, 2, lightPosition.z);
			    	lightPositionList.set(i, 3, 1.0f);
			    	
			        lightIntensityList.set(i, 0, lightIntensity.x);
			        lightIntensityList.set(i, 1, lightIntensity.y);
			        lightIntensityList.set(i, 2, lightIntensity.z);
			        
			        viewSet.setLightPosition(i, lightPosition);
			        viewSet.setLightIntensity(i, lightIntensity);
		    	}
		        
		        lightPositionBuffer = context.createUniformBuffer().setData(lightPositionList);
		        lightIntensityBuffer = context.createUniformBuffer().setData(lightIntensityList);
		        
		        System.out.println("Light fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	    	}
	    	else if (param.isLightIntensityEstimationEnabled())
	    	{
		        FloatVertexList lightIntensityList = new FloatVertexList(3, viewSet.getLightCount());
	    		
	    		Vector3 lightIntensity = new Vector3((float)(avgDistance * avgDistance));
		        System.out.println("Using light intensity: " + lightIntensity.x + " " + lightIntensity.y + " " + lightIntensity.z);
		        
		        for (int i = 0; i < viewSet.getLightCount(); i++)
		    	{
			        lightIntensityList.set(i, 0, lightIntensity.x);
			        lightIntensityList.set(i, 1, lightIntensity.y);
			        lightIntensityList.set(i, 2, lightIntensity.z);

			        viewSet.setLightIntensity(i, lightIntensity);
		    	}
		        
		        lightPositionBuffer = viewSet.getLightPositionBuffer();
		        lightIntensityBuffer = context.createUniformBuffer().setData(lightIntensityList);
	    	}
	    	else
	    	{
	    		System.out.println("Skipping light fit.");
	    		
	    		lightPositionBuffer = viewSet.getLightPositionBuffer();
		        lightIntensityBuffer =  viewSet.getLightIntensityBuffer();
	    	}
	        
//	        if (!param.isImagePreprojectionUseEnabled())
//	        {
//		        System.out.println("Creating shadow maps...");
//		    	Date timestamp = new Date();
//		    	
//		    	// Build shadow maps for each view
//		    	int width = param.getImageWidth(); //viewTextures.getWidth();
//		    	int height = param.getImageHeight(); //viewTextures.getHeight();
//		    	shadowTextures = context.get2DDepthTextureArrayBuilder(width, height, viewSet.getCameraPoseCount()).createTexture();
//		    	
//		    	// Don't automatically generate any texture attachments for this framebuffer object
//		    	FramebufferObject<ContextType> shadowRenderingFBO = context.getFramebufferObjectBuilder(width, height).createFramebufferObject();
//		    	
//		    	Renderable<ContextType> shadowRenderable = context.createRenderable(depthRenderingProgram);
//		    	shadowRenderable.addVertexBuffer("position", positionBuffer);
//		    	
//		    	// Flatten the camera pose matrices into 16-component vectors and store them in the vertex list data structure.
//		    	FloatVertexList flattenedShadowMatrices = new FloatVertexList(16, viewSet.getCameraPoseCount());
//		    	
//		    	// Render each shadow map
//		    	for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
//		    	{
//		    		shadowRenderingFBO.setDepthAttachment(shadowTextures.getLayerAsFramebufferAttachment(i));
//		    		shadowRenderingFBO.clearDepthBuffer();
//		    		
//		    		Matrix4 modelView = Matrix4.lookAt(new Vector3(viewSet.getCameraPoseInverse(i).times(new Vector4(lightPosition, 1.0f))), center, new Vector3(0, 1, 0));
//		        	depthRenderingProgram.setUniform("model_view", modelView);
//		        	
//		    		Matrix4 projection = viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(i))
//							.getProjectionMatrix(
//								viewSet.getRecommendedNearPlane(), 
//								viewSet.getRecommendedFarPlane() * SHADOW_MAP_FAR_PLANE_CUSHION // double it for good measure
//							);
//		    		depthRenderingProgram.setUniform("projection", projection);
//		        	
//		    		shadowRenderable.draw(PrimitiveMode.TRIANGLES, shadowRenderingFBO);
//		    		
//		    		Matrix4 fullTransform = projection.times(modelView);
//		    		
//		    		int d = 0;
//					for (int col = 0; col < 4; col++) // column
//					{
//						for (int row = 0; row < 4; row++) // row
//						{
//							flattenedShadowMatrices.set(i, d, fullTransform.get(row, col));
//							d++;
//						}
//					}
//		    	}
//				
//				// Create the uniform buffer
//				shadowMatrixBuffer = context.createUniformBuffer().setData(flattenedShadowMatrices);
//		
//		    	shadowRenderingFBO.delete();
//		    	
//				System.out.println("Shadow maps created in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
//	        }
		}
	}
	
	private void writeMTLFile(SpecularParams specularParams) throws FileNotFoundException
	{
    	PrintStream materialStream = new PrintStream(new File(outputDir, materialFileName));
    	materialStream.println("newmtl " + materialName);

    	if (param.isDiffuseTextureEnabled())
    	{
    		materialStream.println("Kd 1.0 1.0 1.0");
	    	materialStream.println("map_Kd " + materialName + "_Kd.png");
    		materialStream.println("Ka 1.0 1.0 1.0");
	    	materialStream.println("map_Ka " + materialName + "_Kd.png");
    	}
    	else
    	{
    		materialStream.println("Kd 0.0 0.0 0.0");
	    	
	    	if (param.isSpecularTextureEnabled())
	    	{
	    		materialStream.println("Ka 1.0 1.0 1.0");
		    	materialStream.println("map_Ka " + materialName + "_Ks.png");
	    	}
	    	else
	    	{
	    		materialStream.println("Ka " + specularParams.reflectivity + " " + specularParams.reflectivity + " " + specularParams.reflectivity);
	    	}
    	}
    	
    	if (param.isSpecularTextureEnabled())
    	{
	    	materialStream.println("Ks 1.0 1.0 1.0");
	    	materialStream.println("map_Ks " + materialName + "_Ks.png");
	    	materialStream.println("Ns " + (2.0 / (specularParams.roughness * specularParams.roughness) - 2.0)); // Fallback for non-PBR
	    	
	    	if (param.isRoughnessTextureEnabled())
	    	{
		    	materialStream.println("Pr 1.0");
		    	materialStream.println("map_Pr " + materialName + "_Pr.png");
	    	}
	    	else
	    	{
		    	materialStream.println("Pr " + specularParams.roughness);
	    	}
    	}
    	else
    	{
    		materialStream.println("Ks " + specularParams.reflectivity + " " + specularParams.reflectivity + " " + specularParams.reflectivity);
	    	materialStream.println("Ns " + (2.0 / (specularParams.roughness * specularParams.roughness) - 2.0)); // Fallback for non-PBR
	    	materialStream.println("Pr " + specularParams.roughness);
    	}
    	
    	if (param.isNormalTextureEnabled())
    	{
    		materialStream.println("norm " + materialName + "_norm.png");
    	}
    	
    	materialStream.println("d 1.0");
    	materialStream.println("Tr 0.0");
    	materialStream.println("illum 5");
    	
    	materialStream.flush();
    	materialStream.close();
	}

	public void execute() throws IOException
	{	
//		final int DEBUG_PIXEL_X = 322;
//		final int DEBUG_PIXEL_Y = param.getTextureSize() - 365;

    	System.out.println("Max vertex uniform components across all blocks:" + context.getMaxCombinedVertexUniformComponents());
    	System.out.println("Max fragment uniform components across all blocks:" + context.getMaxCombinedFragmentUniformComponents());
    	System.out.println("Max size of a uniform block in bytes:" + context.getMaxUniformBlockSize());
    	System.out.println("Max texture array layers:" + context.getMaxArrayTextureLayers());
		
		System.out.println("Loading view set...");
    	Date timestamp = new Date();
		
    	String[] vsetFileNameParts = vsetFile.getName().split("\\.");
    	String fileExt = vsetFileNameParts[vsetFileNameParts.length-1];
    	if (fileExt.equalsIgnoreCase("vset"))
    	{
    		System.out.println("Loading from VSET file.");
    		viewSet = ViewSet.loadFromVSETFile(vsetFile, null, param.getGamma(), param.getLinearLuminanceValues(), param.getEncodedLuminanceValues(), context, null);
    	}
    	else if (fileExt.equalsIgnoreCase("xml"))
    	{
    		System.out.println("Loading from Agisoft Photoscan XML file.");
    		viewSet = ViewSet.loadFromAgisoftXMLFile(vsetFile, null, param.getGamma(), param.getLinearLuminanceValues(), param.getEncodedLuminanceValues(), context, null);
    	}
    	else
    	{
    		System.out.println("Unrecognized file type, aborting.");
    		return;
    	}
    	
    	auxDir = new File(outputDir, "_aux");
    	auxDir.mkdirs();
    	
    	outputDir.mkdir();
    	if (param.isDebugModeEnabled())
    	{
    		new File(auxDir, "specularResidDebug").mkdir();
    	}
    	
    	System.out.println("Loading view set completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	
    	if(viewSet.getLuminanceMap() == null)
    	{
    		System.out.println("WARNING: no luminance mapping found.  Reflectance values are not physically grounded.");
    	}
		
		context.enableDepthTest();
    	//context.enableBackFaceCulling();
    	
    	try
    	{
	    	compileShaders();
	    	
	    	System.out.println("Loading mesh...");
	    	timestamp = new Date();
	    	
	    	loadMesh();
	    	
	    	System.out.println("Loading mesh completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	    	
	    	tmpDir = new File(outputDir, "tmp");
	    	
	    	System.out.println("Primary view: " + param.getPrimaryViewName());
	    	viewSet.setPrimaryView(param.getPrimaryViewName());
	    	System.out.println("Primary view index: " + viewSet.getPrimaryViewIndex());
	    	
	    	// Load textures, generate visibility depth textures, fit light source and generate shadow depth textures
    		double avgDistance = loadTextures();
    		fitLightSource(avgDistance);
	    	
    		viewSet.setGeometryFileName(objFile.getName());
	    	Path relativePathToRescaledImages = outputDir.toPath().relativize(imageDir.toPath());
	    	viewSet.setRelativeImagePathName(relativePathToRescaledImages.toString());
	    	
	    	FileOutputStream outputStream = new FileOutputStream(new File(outputDir, vsetFile.getName().split("\\.")[0] + ".vset"));
	        viewSet.writeVSETFileToStream(outputStream);
	    	outputStream.flush();
	    	outputStream.close();
	    	
	    	File objFileCopy = new File(outputDir, objFile.getName());
	    	Files.copy(objFile.toPath(), objFileCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
	    	
			// Phong regression
	    	FramebufferObject<ContextType> diffuseFitFramebuffer = null;
	    	
	    	int subdivSize = param.getTextureSize() / param.getTextureSubdivision();
        
	    	if (!param.isDiffuseTextureEnabled())
	    	{
	    		System.out.println("Skipping diffuse fit.");
	    	}
	    	else
	    	{
	    		System.out.println("Beginning diffuse fit (" + (param.getTextureSubdivision() * param.getTextureSubdivision()) + " blocks)...");
		    	timestamp = new Date();
	    		
	    		diffuseFitFramebuffer = 
        			context.getFramebufferObjectBuilder(param.getTextureSize(), param.getTextureSize())
    					.addColorAttachments(4)
    					.createFramebufferObject();
	        	
	    		diffuseFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
	    		
	    		DiffuseFit<ContextType> diffuseFit = createDiffuseFit(diffuseFitFramebuffer, viewSet.getCameraPoseCount(), param.getTextureSubdivision());
	    		
	    		File diffuseTempDirectory = new File(tmpDir, "diffuse");
		    	File normalTempDirectory = new File(tmpDir, "normal");
		    	File normalTSTempDirectory = new File(tmpDir, "normalTS");
	    		
	    		diffuseTempDirectory.mkdir();
		    	normalTempDirectory.mkdir();
		    	normalTSTempDirectory.mkdir();
		    	
		    	if (param.isImagePreprojectionUseEnabled())
		    	{
					final FramebufferObject<ContextType> currentFramebuffer = diffuseFitFramebuffer;
		    		diffuseFit.fitTextureSpace(tmpDir,
	    				(row, col) -> 
	    				{
	    					currentFramebuffer.saveColorBufferToFile(0, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
    	    		        		"PNG", new File(diffuseTempDirectory, String.format("r%04dc%04d.png", row, col)));
    	    		        
	    					currentFramebuffer.saveColorBufferToFile(1, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
    	    		        		"PNG", new File(normalTempDirectory, String.format("r%04dc%04d.png", row, col)));
    	    		        
	    					currentFramebuffer.saveColorBufferToFile(3, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
    	    		        		"PNG", new File(normalTSTempDirectory, String.format("r%04dc%04d.png", row, col)));
    	    		        
    	    		        System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + 
    	    		        		(param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
	    				});
		    	}
		    	else
		    	{
		    		diffuseFit.fitImageSpace(viewTextures, depthTextures, shadowTextures, 
	    				(row, col) ->
    					{
    						System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + 
    	    		        		(param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
    					});
		    	}

		        System.out.println("Diffuse fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	
		    	System.out.println("Filling empty regions...");
		    	timestamp = new Date();
	    	}
	    	
	    	FramebufferObject<ContextType> backFramebuffer = 
				context.getFramebufferObjectBuilder(param.getTextureSize(), param.getTextureSize())
					.addColorAttachments(4)
					.createFramebufferObject();
	    	
	    	Renderable<ContextType> holeFillRenderable = context.createRenderable(holeFillProgram);
	    	VertexBuffer<ContextType> rectBuffer = context.createRectangle();
	    	holeFillRenderable.addVertexBuffer("position", rectBuffer);
	    	
	    	holeFillProgram.setUniform("minFillAlpha", 0.5f);
			
	    	if (param.isDiffuseTextureEnabled())
	    	{
				System.out.println("Diffuse fill...");
		    	
		    	// Diffuse
		    	FramebufferObject<ContextType> frontFramebuffer = diffuseFitFramebuffer;
		    	for (int i = 0; i < param.getTextureSize() / 2; i++)
		    	{
		    		backFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
		    		backFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    		backFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		    		backFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
		    		
		    		holeFillProgram.setTexture("input0", frontFramebuffer.getColorAttachmentTexture(0));
		    		holeFillProgram.setTexture("input1", frontFramebuffer.getColorAttachmentTexture(1));
		    		holeFillProgram.setTexture("input2", frontFramebuffer.getColorAttachmentTexture(2));
		    		holeFillProgram.setTexture("input3", frontFramebuffer.getColorAttachmentTexture(3));
		    		
		    		holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, backFramebuffer);
		    		context.finish();
		    		
		    		FramebufferObject<ContextType> tmp = frontFramebuffer;
		    		frontFramebuffer = backFramebuffer;
		    		backFramebuffer = tmp;
		    	}
		    	diffuseFitFramebuffer = frontFramebuffer;
		    	
				System.out.println("Empty regions filled in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	System.out.println("Saving textures...");
		    	timestamp = new Date();
		    	
		    	diffuseFitFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "diffuse-old.png"));
		    	diffuseFitFramebuffer.saveColorBufferToFile(1, "PNG", new File(auxDir, "normal.png"));
		    	//diffuseFitFramebuffer.saveColorBufferToFile(2, "PNG", new File(textureDirectory, "ambient.png"));
		    	diffuseFitFramebuffer.saveColorBufferToFile(3, "PNG", new File(auxDir, "normalts.png"));
		    	
		    	if (!param.isSpecularTextureEnabled())
		    	{
		    		frontFramebuffer.saveColorBufferToFile(0, "PNG", new File(outputDir, materialName + "_Kd.png"));
					
					if (param.isNormalTextureEnabled())
					{
						frontFramebuffer.saveColorBufferToFile(3, "PNG", new File(outputDir, materialName + "_norm.png"));
					}
		    	}
		
		    	System.out.println("Textures saved in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	    	}
	    	
	    	System.out.println("Fitting specular residual...");
	    	timestamp = new Date();
	    	
			// Estimate the global, average specular parameters.
	    	SpecularParams specularParams = globalSpecularFit(viewSet, null, null);
				
    		System.out.println("Reflectivity: " + specularParams.reflectivity);
    		System.out.println("Roughness: " + specularParams.roughness);
    		System.out.println("Remainder: " + specularParams.remainder);
			double[] roughnessValues = {specularParams.roughness};
    		//double[] roughnessValues = resample(viewSet, diffuseTexture, normalTexture);
			
			PrintStream specularInfoFile = new PrintStream(new File(auxDir, "specularInfo.txt"));
			specularInfoFile.println("reflectivity " + specularParams.reflectivity);
			specularInfoFile.println("roughness " + specularParams.roughness);
			specularInfoFile.println("remainder " + specularParams.remainder);
			specularInfoFile.close();
				
			if (param.isSpecularTextureEnabled())
	    	{
				System.out.println("Creating specular reflectivity texture...");
				
				FloatVertexList roughnessList = new FloatVertexList(1, roughnessValues.length);
				
				for (int i = 0; i < roughnessValues.length; i++)
				{
					roughnessList.set(i, 0, Math.max(0.125f, (float)roughnessValues[i]));
				}
				
				Texture2D<ContextType> roughnessTexture = 
						context.get2DColorTextureBuilder(ROUGHNESS_TEXTURE_SIZE, ROUGHNESS_TEXTURE_SIZE, roughnessList)
							.setInternalFormat(ColorFormat.R32F)
							.setLinearFilteringEnabled(true)
							.setMipmapsEnabled(false)
							.createTexture();


		    	FramebufferObject<ContextType> frontFramebuffer = 
	    			context.getFramebufferObjectBuilder(param.getTextureSize(), param.getTextureSize())
						.addColorAttachments(4)
						.createFramebufferObject();

				frontFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
				frontFramebuffer.clearColorBuffer(1, 0.5f, 0.5f, 1.0f, 1.0f); // normal map
				frontFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
				frontFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);

				backFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
				backFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
				backFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
				backFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
		    	
				SpecularFit<ContextType> specularFit = createSpecularFit(backFramebuffer, viewSet.getCameraPoseCount(), param.getTextureSubdivision());
				
	    		File diffuseTempDirectory = new File(tmpDir, "diffuse");
	    		diffuseTempDirectory.mkdir();
				
				File specularTempDirectory = new File(tmpDir, "specular");
		    	specularTempDirectory.mkdir();
		    	
		    	if (param.isImagePreprojectionUseEnabled())
		    	{
		    		final FramebufferObject<ContextType> currentFramebuffer = backFramebuffer;
		    		specularFit.fitTextureSpace(tmpDir, 
	    				!param.isDiffuseTextureEnabled() ? frontFramebuffer.getColorAttachmentTexture(0) : diffuseFitFramebuffer.getColorAttachmentTexture(0),
						!param.isDiffuseTextureEnabled() ? frontFramebuffer.getColorAttachmentTexture(1) : diffuseFitFramebuffer.getColorAttachmentTexture(3), 
	    				roughnessTexture,
	    				(row, col) ->
	    				{
	    					currentFramebuffer.saveColorBufferToFile(0, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
    	    		        		"PNG", new File(specularTempDirectory, String.format("alt_r%04dc%04d.png", row, col)));

	    					currentFramebuffer.saveColorBufferToFile(1, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
    	    		        		"PNG", new File(diffuseTempDirectory, String.format("alt_r%04dc%04d.png", row, col)));
    	    	    		
    	    	    		System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + 
    	    	    				(param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
	    				});
		    	}
		    	else
		    	{
		    		specularFit.fitImageSpace(viewTextures, depthTextures, shadowTextures, 
	    				!param.isDiffuseTextureEnabled() ? frontFramebuffer.getColorAttachmentTexture(0) : diffuseFitFramebuffer.getColorAttachmentTexture(0),
						!param.isDiffuseTextureEnabled() ? frontFramebuffer.getColorAttachmentTexture(1) : diffuseFitFramebuffer.getColorAttachmentTexture(3), 
	    				roughnessTexture, 
	    				(row, col) -> 
    					{
    						System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + 
    	    	    				(param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
    					});
		    	}
		    	
		    	FramebufferObject<ContextType> tmp = backFramebuffer;
		    	backFramebuffer = frontFramebuffer;
		    	frontFramebuffer = tmp;

		    	frontFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "diffuse-raw.png"));
		    	frontFramebuffer.saveColorBufferToFile(1, "PNG", new File(auxDir, "normal-raw.png"));
		    	frontFramebuffer.saveColorBufferToFile(2, "PNG", new File(auxDir, "specular-raw.png"));
		    	frontFramebuffer.saveColorBufferToFile(3, "PNG", new File(auxDir, "roughness-raw.png"));
		    	
		    	if (param.isRoughnessTextureEnabled())
		    	{
					// Non-linear adjustment
					AdjustFit<ContextType> adjustFit = createAdjustFit(viewSet.getCameraPoseCount(), param.getTextureSubdivision());
					ErrorCalc<ContextType> errorCalc = createErrorCalc(viewSet.getCameraPoseCount(), param.getTextureSubdivision());
					Renderable<ContextType> finalizeRenderable = context.createRenderable(finalizeProgram);
					finalizeRenderable.addVertexBuffer("position", rectBuffer);
					
					FramebufferObject<ContextType> frontErrorFramebuffer = 
						context.getFramebufferObjectBuilder(param.getTextureSize(), param.getTextureSize())
							.addColorAttachments(ColorFormat.RG32F, 1)
							.addColorAttachments(ColorFormat.R8, 1)
							.createFramebufferObject();

					FramebufferObject<ContextType> backErrorFramebuffer = 
						context.getFramebufferObjectBuilder(param.getTextureSize(), param.getTextureSize())
							.addColorAttachments(ColorFormat.RG32F, 1)
							.addColorAttachments(ColorFormat.R8, 1)
							.createFramebufferObject();
					
					frontErrorFramebuffer.clearColorBuffer(0, 128.0f, Float.MAX_VALUE, 0.0f, 0.0f);
		    		backErrorFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    		
		    		if (param.isImagePreprojectionUseEnabled())
			    	{
			    		errorCalc.fitTextureSpace(
		    				backErrorFramebuffer,
		    				tmpDir,
		    				frontFramebuffer.getColorAttachmentTexture(0),
		    				frontFramebuffer.getColorAttachmentTexture(1),
		    				frontFramebuffer.getColorAttachmentTexture(2),
		    				frontFramebuffer.getColorAttachmentTexture(3),
		    				frontErrorFramebuffer.getColorAttachmentTexture(0),
		    				(row, col) ->
		    				{
//		    	    	    		System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + 
//		    	    	    				(param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
		    				});
			    	}
			    	else
			    	{
			    		errorCalc.fitImageSpace(
		    				backErrorFramebuffer,
		    				viewTextures, depthTextures, shadowTextures, 
		    				frontFramebuffer.getColorAttachmentTexture(0),
		    				frontFramebuffer.getColorAttachmentTexture(1),
		    				frontFramebuffer.getColorAttachmentTexture(2),
		    				frontFramebuffer.getColorAttachmentTexture(3),
		    				frontErrorFramebuffer.getColorAttachmentTexture(0),
		    				(row, col) -> 
	    					{
//		    						System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + 
//		    	    	    				(param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
	    					});
			    	}
		    		
		    		context.finish();
		    		
//		    		backErrorFramebuffer.saveColorBufferToFile(0, "PNG", new File(textureDirectory, "error-mask-init.png"));
		    		
		    		tmp = frontErrorFramebuffer;
		    		frontErrorFramebuffer = backErrorFramebuffer;
		    		backErrorFramebuffer = tmp;
		    		
		    		double lastSumSqError = 0.0;
		    		float[] errorData = frontErrorFramebuffer.readFloatingPointColorBufferRGBA(0);
		    		for (int j = 0; j * 4 + 3 < errorData.length; j++)
		    		{
		    			lastSumSqError += errorData[j * 4 + 1]; // Green channel holds squared error
		    		}
		    		
		    		
		    		System.out.println("Sum squared error: " + lastSumSqError);
		    		
		    		System.out.println("Adjusting fit...");
		    		
		    		boolean saveDebugTextures = false;
		    		boolean useGlobalDampingFactor = false;
		    		float globalDampingFactor = 128.0f;
					
					for (int i = 0; globalDampingFactor <= 1048576; i++)
					{
			    		backFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
			    		backFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
			    		backFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
			    		backFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
			    		
			    		if(useGlobalDampingFactor)
			    		{
			    			// hack to override damping factor and never discard the result - TODO make this more elegant
			    			frontErrorFramebuffer.clearColorBuffer(0, globalDampingFactor, Float.MAX_VALUE, 0.0f, 0.0f);
			    		}
			    		
			    		if (param.isImagePreprojectionUseEnabled())
				    	{
				    		adjustFit.fitTextureSpace(
			    				backFramebuffer,
			    				tmpDir,
			    				frontFramebuffer.getColorAttachmentTexture(0),
			    				frontFramebuffer.getColorAttachmentTexture(1),
			    				frontFramebuffer.getColorAttachmentTexture(2),
			    				frontFramebuffer.getColorAttachmentTexture(3),
			    				frontErrorFramebuffer.getColorAttachmentTexture(0),
			    				(row, col) ->
			    				{
//			    					currentFramebuffer.saveColorBufferToFile(0, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
//		    	    		        		"PNG", new File(diffuseTempDirectory, String.format("alt_r%04dc%04d.png", row, col)));
//
//			    					currentFramebuffer.saveColorBufferToFile(2, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
//		    	    		        		"PNG", new File(specularTempDirectory, String.format("alt_r%04dc%04d.png", row, col)));
		    	    	    		
//			    	    	    		System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + 
//			    	    	    				(param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
			    				});
				    	}
				    	else
				    	{
				    		adjustFit.fitImageSpace(
			    				backFramebuffer,
			    				viewTextures, depthTextures, shadowTextures, 
			    				frontFramebuffer.getColorAttachmentTexture(0),
			    				frontFramebuffer.getColorAttachmentTexture(1),
			    				frontFramebuffer.getColorAttachmentTexture(2),
			    				frontFramebuffer.getColorAttachmentTexture(3),
			    				frontErrorFramebuffer.getColorAttachmentTexture(0),
			    				(row, col) -> 
		    					{
//			    						System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + 
//			    	    	    				(param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
		    					});
				    	}
			    		
			    		context.finish();
			    		
			    		if (saveDebugTextures)
				    	{
					    	backFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "diffuse-test1.png"));
				    		backFramebuffer.saveColorBufferToFile(1, "PNG", new File(auxDir, "normal-test1.png"));
				    		backFramebuffer.saveColorBufferToFile(2, "PNG", new File(auxDir, "specular-test1.png"));
				    		backFramebuffer.saveColorBufferToFile(3, "PNG", new File(auxDir, "roughness-test1.png"));
				    	}
			    		
			    		backErrorFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
			    		
			    		if (param.isImagePreprojectionUseEnabled())
				    	{
				    		errorCalc.fitTextureSpace(
			    				backErrorFramebuffer,
			    				tmpDir,
			    				backFramebuffer.getColorAttachmentTexture(0),
			    				backFramebuffer.getColorAttachmentTexture(1),
			    				backFramebuffer.getColorAttachmentTexture(2),
			    				backFramebuffer.getColorAttachmentTexture(3),
			    				frontErrorFramebuffer.getColorAttachmentTexture(0),
			    				(row, col) ->
			    				{
//			    	    	    		System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + 
//			    	    	    				(param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
			    				});
				    	}
				    	else
				    	{
				    		errorCalc.fitImageSpace(
			    				backErrorFramebuffer,
			    				viewTextures, depthTextures, shadowTextures, 
			    				backFramebuffer.getColorAttachmentTexture(0),
			    				backFramebuffer.getColorAttachmentTexture(1),
			    				backFramebuffer.getColorAttachmentTexture(2),
			    				backFramebuffer.getColorAttachmentTexture(3),
			    				frontErrorFramebuffer.getColorAttachmentTexture(0),
			    				(row, col) -> 
		    					{
//			    						System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + 
//			    	    	    				(param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
		    					});
				    	}
			    		
			    		context.finish();
			    		
			    		if (saveDebugTextures)
				    	{
					    	backErrorFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "error-mask-test.png"));
				    	}
			    		
			    		tmp = frontErrorFramebuffer;
			    		frontErrorFramebuffer = backErrorFramebuffer;
			    		backErrorFramebuffer = tmp;
			    		
			    		double sumSqError = 0.0;
			    		errorData = frontErrorFramebuffer.readFloatingPointColorBufferRGBA(0);
			    		for (int j = 0; j * 4 + 3 < errorData.length; j++)
			    		{
			    			sumSqError += errorData[j * 4 + 1]; // Green channel holds squared error
			    		}
			    		
			    		System.out.println("Sum squared error: " + sumSqError);
			    		
			    		if (sumSqError < lastSumSqError)
			    		{
			    			lastSumSqError = sumSqError;
			    			
			    			System.out.println("Saving iteration.");
			    			
			    			if (useGlobalDampingFactor)
			    			{
			    				globalDampingFactor /= 2;
				    			
				    			System.out.println("Next damping factor: " + globalDampingFactor);
				    			
				    			// Set the mask framebuffer to all 1 (hack - TODO make this more elegant)
				    			frontErrorFramebuffer.clearColorBuffer(1, 1.0f, 1.0f, 1.0f, 1.0f);
			    			}
			    			else
			    			{
			    				// If the damping factor isn't being used, set to the minimum, which will function as a countdown if an iteration is unproductive.
			    				globalDampingFactor = 0.0078125f;
			    			}
			    		
				    		finalizeProgram.setTexture("input0", backFramebuffer.getColorAttachmentTexture(0));
				    		finalizeProgram.setTexture("input1", backFramebuffer.getColorAttachmentTexture(1));
				    		finalizeProgram.setTexture("input2", backFramebuffer.getColorAttachmentTexture(2));
				    		finalizeProgram.setTexture("input3", backFramebuffer.getColorAttachmentTexture(3));
				    		finalizeProgram.setTexture("alphaMask", frontErrorFramebuffer.getColorAttachmentTexture(1));
				    		
				    		finalizeRenderable.draw(PrimitiveMode.TRIANGLE_FAN, frontFramebuffer);
				    		context.finish();
			    		}
			    		else
			    		{
			    			// If useGlobalDampingFactor == false, then this effectively serves as a countdown in the case of an unproductive iteration.
			    			// If enough unproductive iterations occur, then this variable will keep doubling until it exceeds the maximum value.
			    			globalDampingFactor *= 2;
			    			
			    			System.out.println("Discarding iteration.");
			    			System.out.println("Next damping factor: " + globalDampingFactor);
			    		}

			    		if (saveDebugTextures)
				    	{
				    		frontFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "diffuse-test2.png"));
				    		frontFramebuffer.saveColorBufferToFile(1, "PNG", new File(auxDir, "normal-test2.png"));
				    		frontFramebuffer.saveColorBufferToFile(2, "PNG", new File(auxDir, "specular-test2.png"));
				    		frontFramebuffer.saveColorBufferToFile(3, "PNG", new File(auxDir, "roughness-test2.png"));
				    	}
			    		
			    		System.out.println("Iteration " + (i+1) + " complete.");
		    			System.out.println();
					}
					
		    		frontErrorFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "error-mask-final.png"));
			    	
			    	frontErrorFramebuffer.delete();
			    	backErrorFramebuffer.delete();
		    	}
			
				// Fill holes
				for (int i = 0; i < param.getTextureSize() / 2; i++)
		    	{
		    		backFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    		backFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    		backFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		    		backFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
		    		
		    		holeFillProgram.setTexture("input0", frontFramebuffer.getColorAttachmentTexture(0));
		    		holeFillProgram.setTexture("input1", frontFramebuffer.getColorAttachmentTexture(1));
		    		holeFillProgram.setTexture("input2", frontFramebuffer.getColorAttachmentTexture(2));
		    		holeFillProgram.setTexture("input3", frontFramebuffer.getColorAttachmentTexture(3));
		    		
		    		holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, backFramebuffer);
		    		context.finish();
		    		
		    		tmp = frontFramebuffer;
		    		frontFramebuffer = backFramebuffer;
		    		backFramebuffer = tmp;
		    	}
				
				// Save a copy of all of the channels, even the ones that shouldn't be needed, in the _aux folder.
				frontFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "diffuse-final.png"));
	    		frontFramebuffer.saveColorBufferToFile(1, "PNG", new File(auxDir, "normal-final.png"));
	    		frontFramebuffer.saveColorBufferToFile(2, "PNG", new File(auxDir, "specular-final.png"));
	    		frontFramebuffer.saveColorBufferToFile(3, "PNG", new File(auxDir, "roughness-final.png"));

				if (param.isDiffuseTextureEnabled())
				{
					frontFramebuffer.saveColorBufferToFile(0, "PNG", new File(outputDir, materialName + "_Kd.png"));
					
					if (param.isNormalTextureEnabled())
					{
						frontFramebuffer.saveColorBufferToFile(1, "PNG", new File(outputDir, materialName + "_norm.png"));
					}
				}
				
				frontFramebuffer.saveColorBufferToFile(2, "PNG", new File(outputDir, materialName + "_Ks.png"));
				
				//if (param.isRoughnessTextureEnabled())
				{
					frontFramebuffer.saveColorBufferToFile(3, "PNG", new File(outputDir, materialName + "_Pr.png"));
				}
    	        
    	    	frontFramebuffer.delete();
			}

	    	backFramebuffer.delete();
	    	
	    	writeMTLFile(specularParams);
			
			if (viewTextures != null)
	        {
	        	viewTextures.delete();
	        }
	        
	        if (depthTextures != null)
	        {
	        	depthTextures.delete();
	        }
	        
	        if (shadowTextures != null)
	        {
	        	shadowTextures.delete();
	        }
	        
	        lightPositionBuffer.delete();
	        lightIntensityBuffer.delete();
	        
	        if (shadowMatrixBuffer != null)
	        {
	        	shadowMatrixBuffer.delete();
	        }
	        
	        if (rectBuffer != null)
	        {
	        	rectBuffer.delete();
	        }
	    	
	        if (param.isDiffuseTextureEnabled())
	        {
	        	diffuseFitFramebuffer.delete();
	        }
	
	    	//System.out.println("Resampling completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	}
    	finally
    	{
			if (projTexProgram != null) projTexProgram.delete();
			if (diffuseFitProgram != null) diffuseFitProgram.delete();
			if (specularFitProgram != null) specularFitProgram.delete();
			if (diffuseDebugProgram != null) diffuseDebugProgram.delete();
			if (specularDebugProgram != null) specularDebugProgram.delete();
			if (depthRenderingProgram != null) depthRenderingProgram.delete();
			
			if (viewSet != null) viewSet.deleteOpenGLResources();
			if (positionBuffer != null) positionBuffer.delete();
			if (normalBuffer != null) normalBuffer.delete();
			if (texCoordBuffer != null) texCoordBuffer.delete();
			if (tangentBuffer != null) tangentBuffer.delete();
    	}
	}
}
