package tetzlaff.imagebasedmicrofacet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture1D;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.ulf.UnstructuredLightField;

public class SampledMicrofacetField<ContextType extends Context<ContextType>>
{
	public final UnstructuredLightField<ContextType> ulf;
	public final Texture2D<ContextType> diffuseTexture;
	public final Texture2D<ContextType> normalTexture;
	public final Texture2D<ContextType> specularTexture;
	public final Texture2D<ContextType> roughnessTexture;
	public final Texture3D<ContextType> shadowTextures;
	public final Texture1D<ContextType> mfdTexture;
	public final UniformBuffer<ContextType> shadowMatrixBuffer;
	
	public SampledMicrofacetField(UnstructuredLightField<ContextType> ulf, File diffuseFile, File normalFile, File specularFile, File roughnessFile, File mfdFile, ContextType context) throws IOException
	{
		this.ulf = ulf;
		
		if (diffuseFile != null && diffuseFile.exists())
		{
			System.out.println("Diffuse texture found.");
			diffuseTexture = context.get2DColorTextureBuilder(diffuseFile, true)
					.setInternalFormat(ColorFormat.RGB8)
					.setMipmapsEnabled(true)
					.setLinearFilteringEnabled(true)
					.createTexture();
		}
		else
		{
			diffuseTexture = null;
		}
		
		if (normalFile != null && normalFile.exists())
		{
			System.out.println("Normal texture found.");
			normalTexture = context.get2DColorTextureBuilder(normalFile, true)
					.setInternalFormat(ColorFormat.RGB8)
					.setMipmapsEnabled(true)
					.setLinearFilteringEnabled(true)
					.createTexture();
		}
		else
		{
			normalTexture = null;
		}
		
		if (specularFile != null && specularFile.exists())
		{
			System.out.println("Specular texture found.");
			specularTexture = context.get2DColorTextureBuilder(specularFile, true)
					.setInternalFormat(ColorFormat.RGB8)
					.setMipmapsEnabled(true)
					.setLinearFilteringEnabled(true)
					.createTexture();
		}
		else
		{
			specularTexture = null;
		}
		
		if (roughnessFile != null && roughnessFile.exists())
		{
			System.out.println("Roughness texture found.");
			roughnessTexture = context.get2DColorTextureBuilder(roughnessFile, true)
					.setInternalFormat(ColorFormat.R8)
					.setMipmapsEnabled(true)
					.setLinearFilteringEnabled(true)
					.createTexture();
		}
		else
		{
			roughnessTexture = null;
		}
		
		if (mfdFile != null && mfdFile.exists())
		{
			System.out.println("Microfacet distribution file found.");
			
			FloatVertexList mfdList = null;
			
			try(Scanner mfdScanner = new Scanner(mfdFile))
			{
				ArrayList<Double> probabilities = new ArrayList<Double>();
				
				mfdScanner.useDelimiter(",");
				while(mfdScanner.hasNext())
				{
					mfdScanner.nextDouble(); // Skip the n dot h entry
					probabilities.add(mfdScanner.nextDouble());
					if (mfdScanner.hasNextLine())
					{
						mfdScanner.nextLine();
					}
				}
	
				mfdList = new FloatVertexList(1, probabilities.size());
				
				int i = 0;
				for (Double probability : probabilities)
				{
					mfdList.set(i, 0, (float)probability.doubleValue());
					i++;
				}
			}
			catch(NoSuchElementException e)
			{
				System.err.println("Invalid microfacet distribution file:");
				e.printStackTrace();
			}
			
			if (mfdList != null)
			{
				mfdTexture = context.get1DColorTextureBuilder(mfdList)
						.setInternalFormat(ColorFormat.R32F)
						.setLinearFilteringEnabled(true)
						.createTexture();
			}
			else
			{
				mfdTexture = null;
			}
		}
		else
		{
			mfdTexture = null;
		}
		
		if (ulf.depthTextures != null)
        {
	    	shadowTextures = 
    			context.get2DDepthTextureArrayBuilder(ulf.depthTextures.getWidth(), ulf.depthTextures.getHeight(), ulf.viewSet.getCameraPoseCount())
	    			.createTexture();
	    	
	    	// Don't automatically generate any texture attachments for this framebuffer object
	    	FramebufferObject<ContextType> depthRenderingFBO = 
    			context.getFramebufferObjectBuilder(ulf.depthTextures.getWidth(), ulf.depthTextures.getHeight())
	    			.createFramebufferObject();
	    	
	    	// Load the program
	    	Program<ContextType> depthRenderingProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"))
	    			.createProgram();
	    	Renderable<ContextType> depthRenderable = context.createRenderable(depthRenderingProgram);
	    	depthRenderable.addVertexBuffer("position", ulf.positionBuffer);

	    	// Flatten the camera pose matrices into 16-component vectors and store them in the vertex list data structure.
	    	FloatVertexList flattenedShadowMatrices = new FloatVertexList(16, ulf.viewSet.getCameraPoseCount());
	    	
	    	// Render each depth texture
	    	for (int i = 0; i < ulf.viewSet.getCameraPoseCount(); i++)
	    	{
	    		depthRenderingFBO.setDepthAttachment(shadowTextures.getLayerAsFramebufferAttachment(i));
	        	depthRenderingFBO.clearDepthBuffer();
	        	
	        	depthRenderingProgram.setUniform("model_view", ulf.viewSet.getCameraPose(i));
	    		depthRenderingProgram.setUniform("projection", 
					ulf.viewSet.getCameraProjection(ulf.viewSet.getCameraProjectionIndex(i))
	    				.getProjectionMatrix(
    						ulf.viewSet.getRecommendedNearPlane(), 
    						ulf.viewSet.getRecommendedFarPlane()
						)
				);
	    		
	    		Matrix4 modelView = Matrix4.lookAt(new Vector3(ulf.viewSet.getCameraPoseInverse(i).times(new Vector4(ulf.viewSet.getLightPosition(0), 1.0f))), ulf.proxy.getCentroid(), new Vector3(0, 1, 0));
	        	depthRenderingProgram.setUniform("model_view", modelView);
	        	
	    		Matrix4 projection = ulf.viewSet.getCameraProjection(ulf.viewSet.getCameraProjectionIndex(i))
						.getProjectionMatrix(
							ulf.viewSet.getRecommendedNearPlane(), 
							ulf.viewSet.getRecommendedFarPlane() * 2 // double it for good measure
						);
	    		depthRenderingProgram.setUniform("projection", projection);
	        	
	        	depthRenderable.draw(PrimitiveMode.TRIANGLES, depthRenderingFBO);
	        	
	        	Matrix4 fullTransform = projection.times(modelView);
	    		
	    		int d = 0;
				for (int col = 0; col < 4; col++) // column
				{
					for (int row = 0; row < 4; row++) // row
					{
						flattenedShadowMatrices.set(i, d, fullTransform.get(row, col));
						d++;
					}
				}
	    	}
			
			// Create the uniform buffer
			shadowMatrixBuffer = context.createUniformBuffer().setData(flattenedShadowMatrices);

	    	depthRenderingProgram.delete();
	    	depthRenderingFBO.delete();
        }
		else
		{
			shadowTextures = null;
			shadowMatrixBuffer = null;
		}
	}
	
	public void deleteOpenGLResources()
	{
		if (diffuseTexture != null)
		{
			diffuseTexture.delete();
		}
		
		if (normalTexture != null)
		{
			normalTexture.delete();
		}
		
		if (specularTexture != null)
		{
			specularTexture.delete();
		}
		
		if (roughnessTexture != null)
		{
			roughnessTexture.delete();
		}
		
		if (shadowTextures != null)
		{
			shadowTextures.delete();
		}
		
		if (shadowMatrixBuffer != null)
		{
			shadowMatrixBuffer.delete();
		}
	}
}
