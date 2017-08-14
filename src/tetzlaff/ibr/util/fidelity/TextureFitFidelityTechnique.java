package tetzlaff.ibr.util.fidelity;

import java.io.File;
import java.io.IOException;
import java.util.List;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Drawable;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.ibr.ReadonlyIBRSettingsModel;
import tetzlaff.ibr.rendering.IBRResources;

public class TextureFitFidelityTechnique<ContextType extends Context<ContextType>> implements FidelityEvaluationTechnique<ContextType>
{
	private IBRResources<ContextType> resources;
    private boolean usePerceptuallyLinearError;

	private Program<ContextType> textureFitProgram;
	private Drawable<ContextType> textureFitDrawable;
	private FramebufferObject<ContextType> textureFitFramebuffer;

	private Program<ContextType> textureFitBaselineProgram;
	private Drawable<ContextType> textureFitBaselineDrawable;
	private FramebufferObject<ContextType> textureFitBaselineFramebuffer;
	
	private Texture2D<ContextType> maskTexture;

	private Program<ContextType> fidelityProgram;
	private Drawable<ContextType> fidelityDrawable; 
	private FramebufferObject<ContextType> fidelityFramebuffer;

	private NativeVectorBuffer viewIndexData;
	
	public TextureFitFidelityTechnique(boolean usePerceptuallyLinearError)
	{
		this.usePerceptuallyLinearError = usePerceptuallyLinearError;
	}
	
	@Override
	public boolean isGuaranteedInterpolating()
	{
		return false;
	}

	@Override
	public boolean isGuaranteedMonotonic() 
	{
		return false;
	}

	@Override
	public void initialize(IBRResources<ContextType> resources, ReadonlyIBRSettingsModel settings, int size) throws IOException
	{
		this.resources = resources;
		
		resources.context.getState().disableBackFaceCulling();
		
		fidelityProgram = resources.context.getShaderProgramBuilder()
			.addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
			.addShader(ShaderType.FRAGMENT, new File("shaders/texturefit/fidelity.frag"))
			.createProgram();
			
		fidelityFramebuffer = resources.context.buildFramebufferObject(size, size)
			.addColorAttachment(ColorFormat.RG32F)
			.createFramebufferObject();
		
		fidelityDrawable = resources.context.createDrawable(fidelityProgram);
		fidelityDrawable.addVertexBuffer("position", resources.positionBuffer);
		fidelityDrawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
		fidelityDrawable.addVertexBuffer("normal", resources.normalBuffer);
		fidelityDrawable.addVertexBuffer("tangent", resources.tangentBuffer);
		
		textureFitProgram = resources.context.getShaderProgramBuilder()
			.addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
			.addShader(ShaderType.FRAGMENT, new File("shaders/texturefit/specularfit2_imgspace_subset.frag"))
			.createProgram();
			
		textureFitFramebuffer = resources.context.buildFramebufferObject(size, size)
			.addColorAttachments(ColorFormat.RGBA8, 4)
			.createFramebufferObject();
		
		textureFitDrawable = resources.context.createDrawable(textureFitProgram);
		textureFitDrawable.addVertexBuffer("position", resources.positionBuffer);
		textureFitDrawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
		textureFitDrawable.addVertexBuffer("normal", resources.normalBuffer);
		textureFitDrawable.addVertexBuffer("tangent", resources.tangentBuffer);
		
		textureFitBaselineProgram = resources.context.getShaderProgramBuilder()
			.addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
			.addShader(ShaderType.FRAGMENT, new File("shaders/texturefit/specularfit2_imgspace.frag"))
			.createProgram();
			
		textureFitBaselineFramebuffer = resources.context.buildFramebufferObject(size, size)
			.addColorAttachments(ColorFormat.RGBA8, 4)
			.createFramebufferObject();
		
		textureFitBaselineDrawable = resources.context.createDrawable(textureFitBaselineProgram);
		textureFitBaselineDrawable.addVertexBuffer("position", resources.positionBuffer);
		textureFitBaselineDrawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
		textureFitBaselineDrawable.addVertexBuffer("normal", resources.normalBuffer);
		textureFitBaselineDrawable.addVertexBuffer("tangent", resources.tangentBuffer);
		
		// Baseline
		resources.setupShaderProgram(textureFitBaselineDrawable.program(), false);
		
		textureFitBaselineDrawable.program().setUniform("viewCount", resources.viewSet.getCameraPoseCount());
		
		if (this.usePerceptuallyLinearError)
		{
			textureFitBaselineDrawable.program().setUniform("fittingGamma", 2.2f);
		}
		else
		{
			textureFitBaselineDrawable.program().setUniform("fittingGamma", 1.0f);
		}
		
		textureFitBaselineDrawable.program().setUniform("standaloneMode", true);

		textureFitBaselineFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
		textureFitBaselineFramebuffer.clearColorBuffer(1, 0.5f, 0.5f, 1.0f, 1.0f);
		textureFitBaselineFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 1.0f);
		textureFitBaselineFramebuffer.clearColorBuffer(3, 1.0f, 1.0f, 1.0f, 1.0f);
		textureFitBaselineFramebuffer.clearDepthBuffer();
    	
    	textureFitBaselineDrawable.draw(PrimitiveMode.TRIANGLES, textureFitBaselineFramebuffer);
    	
    	viewIndexData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.INT, 1, resources.viewSet.getCameraPoseCount());
	}
	
	@Override
	public void setMask(File maskFile) throws IOException
	{
		if (this.maskTexture != null)
		{
			this.maskTexture.close();
			this.maskTexture = null;
		}
		
		if (maskFile != null)
		{
			this.maskTexture = resources.context.build2DColorTextureFromFile(maskFile, true)
					.setInternalFormat(ColorFormat.R8)
					.setLinearFilteringEnabled(false)
					.setMipmapsEnabled(false)
					.createTexture();
		}
	}

	@Override
	public void updateActiveViewIndexList(List<Integer> activeViewIndexList) 
	{
		resources.setupShaderProgram(textureFitDrawable.program(), false);
		
		for (int i = 0; i < activeViewIndexList.size(); i++)
		{
			viewIndexData.set(i, 0, activeViewIndexList.get(i));
		}
		
		try (UniformBuffer<ContextType> viewIndexBuffer = resources.context.createUniformBuffer().setData(viewIndexData))
		{
			textureFitDrawable.program().setUniformBuffer("ViewIndices", viewIndexBuffer);
			textureFitDrawable.program().setUniform("viewCount", activeViewIndexList.size());
			
			if (this.usePerceptuallyLinearError)
			{
				textureFitDrawable.program().setUniform("fittingGamma", 2.2f);
			}
			else
			{
				textureFitDrawable.program().setUniform("fittingGamma", 1.0f);
			}
			
			textureFitDrawable.program().setUniform("standaloneMode", true);
	    	
			textureFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
			textureFitFramebuffer.clearColorBuffer(1, 0.5f, 0.5f, 1.0f, 1.0f);
			textureFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 1.0f);
			textureFitFramebuffer.clearColorBuffer(3, 1.0f, 1.0f, 1.0f, 1.0f);
			textureFitFramebuffer.clearDepthBuffer();
	    	
	    	textureFitDrawable.draw(PrimitiveMode.TRIANGLES, textureFitFramebuffer);
		}
	}
	
	@Override
	public double evaluateBaselineError(int targetViewIndex, File debugFile)
	{
		resources.setupShaderProgram(fidelityDrawable.program(), false);
		
		fidelityDrawable.program().setUniform("model_view", resources.viewSet.getCameraPose(targetViewIndex));
		fidelityDrawable.program().setUniform("targetViewIndex", targetViewIndex);
		
		if (this.usePerceptuallyLinearError)
		{
			fidelityDrawable.program().setUniform("fittingGamma", 2.2f);
		}
		else
		{
			fidelityDrawable.program().setUniform("fittingGamma", 1.0f);
		}

		fidelityDrawable.program().setUniform("evaluateInXYZ", false);

		fidelityDrawable.program().setUniform("useMaskTexture", this.maskTexture != null);
		fidelityDrawable.program().setTexture("maskTexture", this.maskTexture);
		
		fidelityDrawable.program().setTexture("normalEstimate", textureFitBaselineFramebuffer.getColorAttachmentTexture(1));
		fidelityDrawable.program().setTexture("specularEstimate", textureFitBaselineFramebuffer.getColorAttachmentTexture(2));
		fidelityDrawable.program().setTexture("roughnessEstimate", textureFitBaselineFramebuffer.getColorAttachmentTexture(3));
		
		fidelityFramebuffer.clearColorBuffer(0, -1.0f, -1.0f, -1.0f, -1.0f);
		fidelityFramebuffer.clearDepthBuffer();
    	
		fidelityDrawable.draw(PrimitiveMode.TRIANGLES, fidelityFramebuffer);
		
		double baselineSumSqError = 0.0;
	    //double sumWeights = 0.0;
	    double baselineSumMask = 0.0;
	
		float[] baselineFidelityArray = fidelityFramebuffer.readFloatingPointColorBufferRGBA(0);
		for (int k = 0; 4 * k + 3 < baselineFidelityArray.length; k++)
		{
			if (baselineFidelityArray[4 * k + 1] >= 0.0f)
			{
				baselineSumSqError += baselineFidelityArray[4 * k];
				//sumWeights += baselineFidelityArray[4 * k + 1];
				baselineSumMask += 1.0;
			}
		}
	
		double baselineError = Math.sqrt(baselineSumSqError / baselineSumMask);
		
		if (debugFile != null)
        {
	        try
			{
	    	    fidelityFramebuffer.saveColorBufferToFile(0, "PNG", 
						new File(debugFile.getParentFile(), debugFile.getName()));
	    	    
				textureFitBaselineFramebuffer.saveColorBufferToFile(0, "PNG", new File(debugFile.getParentFile(), "baseline_diffuse.png"));
				textureFitBaselineFramebuffer.saveColorBufferToFile(1, "PNG", new File(debugFile.getParentFile(), "baseline_normal.png"));
				textureFitBaselineFramebuffer.saveColorBufferToFile(2, "PNG", new File(debugFile.getParentFile(), "baseline_specular.png"));
				textureFitBaselineFramebuffer.saveColorBufferToFile(3, "PNG", new File(debugFile.getParentFile(), "baseline_roughness.png"));
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
        }
		
		return baselineError;
	}

	@Override
	public double evaluateError(int targetViewIndex, File debugFile) 
	{
		resources.setupShaderProgram(fidelityDrawable.program(), false);
		
		fidelityDrawable.program().setUniform("model_view", resources.viewSet.getCameraPose(targetViewIndex));
		fidelityDrawable.program().setUniform("targetViewIndex", targetViewIndex);
		
		if (this.usePerceptuallyLinearError)
		{
			fidelityDrawable.program().setUniform("fittingGamma", 2.2f);
		}
		else
		{
			fidelityDrawable.program().setUniform("fittingGamma", 1.0f);
		}

		fidelityDrawable.program().setUniform("evaluateInXYZ", false);

		fidelityDrawable.program().setUniform("useMaskTexture", this.maskTexture != null);
		fidelityDrawable.program().setTexture("maskTexture", this.maskTexture);
		
		fidelityDrawable.program().setTexture("normalEstimate", textureFitFramebuffer.getColorAttachmentTexture(1));
		fidelityDrawable.program().setTexture("specularEstimate", textureFitFramebuffer.getColorAttachmentTexture(2));
		fidelityDrawable.program().setTexture("roughnessEstimate", textureFitFramebuffer.getColorAttachmentTexture(3));
		
		fidelityFramebuffer.clearColorBuffer(0, -1.0f, -1.0f, -1.0f, -1.0f);
		fidelityFramebuffer.clearDepthBuffer();
    	
		fidelityDrawable.draw(PrimitiveMode.TRIANGLES, fidelityFramebuffer);
		
		if (debugFile != null)
        {
	        try
			{
	    	    fidelityFramebuffer.saveColorBufferToFile(0, "PNG", debugFile);
		        
		        textureFitFramebuffer.saveColorBufferToFile(0, "PNG", 
						new File(debugFile.getParentFile(), "diffuse_" + debugFile.getName()));
				textureFitFramebuffer.saveColorBufferToFile(1, "PNG", 
						new File(debugFile.getParentFile(), "normal_" + debugFile.getName()));
				textureFitFramebuffer.saveColorBufferToFile(2, "PNG", 
						new File(debugFile.getParentFile(), "specular_" + debugFile.getName()));
				textureFitFramebuffer.saveColorBufferToFile(3, "PNG", 
						new File(debugFile.getParentFile(), "roughness_" + debugFile.getName()));
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
        }
		
		double sumSqError = 0.0;
	    //double sumWeights = 0.0;
	    double sumMask = 0.0;
	
		float[] fidelityArray = fidelityFramebuffer.readFloatingPointColorBufferRGBA(0);
		for (int k = 0; 4 * k + 3 < fidelityArray.length; k++)
		{
			if (fidelityArray[4 * k + 1] >= 0.0f)
			{
				sumSqError += fidelityArray[4 * k];
				//sumWeights += fidelityArray[4 * k + 1];
				sumMask += 1.0;
			}
		}
	
		double renderError = Math.sqrt(sumSqError / sumMask);
		return renderError;
	}

	@Override
	public void close() throws Exception 
	{
		if (textureFitProgram != null)
		{
			textureFitProgram.close();
		}
	
		if (textureFitFramebuffer != null)
		{
			textureFitFramebuffer.close();
		}
		
		if (fidelityProgram != null)
		{
			fidelityProgram.close();
		}
		
		if (fidelityFramebuffer != null)
		{
			fidelityFramebuffer.close();
		}
	}
}
