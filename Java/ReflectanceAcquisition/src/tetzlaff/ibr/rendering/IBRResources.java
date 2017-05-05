package tetzlaff.ibr.rendering;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.imageio.ImageIO;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
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
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.Material;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.ibr.IBRLoadOptions;
import tetzlaff.ibr.IBRLoadingMonitor;
import tetzlaff.ibr.ViewSet;

public class IBRResources<ContextType extends Context<ContextType>> implements AutoCloseable
{
	public final Context<ContextType> context;
	public final ViewSet viewSet;
	public final VertexMesh geometry;
	
	/**
	 * A GPU buffer containing the camera poses defining the transformation from object space to camera space for each view.
	 * These are necessary to perform projective texture mapping.
	 */
	public final UniformBuffer<ContextType> cameraPoseBuffer;
	
	/**
	 * A GPU buffer containing projection transformations defining the intrinsic properties of each camera.
	 */
	public final UniformBuffer<ContextType> cameraProjectionBuffer;
	
	/**
	 * A GPU buffer containing for every view an index designating the projection transformation that should be used for each view.
	 */
	public final UniformBuffer<ContextType> cameraProjectionIndexBuffer;
	
	/**
	 * A GPU buffer containing light source positions, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
	 * Assumed by convention to be in camera space.
	 */
	public final UniformBuffer<ContextType> lightPositionBuffer;
	
	/**
	 * A GPU buffer containing light source intensities, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
	 */
	public final UniformBuffer<ContextType> lightIntensityBuffer;
	
	/**
	 * A GPU buffer containing for every view an index designating the light source position and intensity that should be used for each view.
	 */
	public final UniformBuffer<ContextType> lightIndexBuffer;
	
	/**
	 * A texture array instantiated on the GPU containing the image corresponding to each view in this dataset.
	 */
	public final Texture3D<ContextType> colorTextures;
	
	/**
	 * A 1D texture defining how encoded RGB values should be converted to linear luminance.
	 */
	public final Texture1D<ContextType> luminanceMap;
	
	/**
	 * A 1D texture defining how encoded RGB values should be converted to linear luminance.
	 */
	public final Texture1D<ContextType> inverseLuminanceMap;
	
	public final VertexBuffer<ContextType> positionBuffer;
	public final VertexBuffer<ContextType> texCoordBuffer;
	public final VertexBuffer<ContextType> normalBuffer;
	public final VertexBuffer<ContextType> tangentBuffer;
	public final Texture3D<ContextType> depthTextures;
	public final Texture2D<ContextType> diffuseTexture;
	public final Texture2D<ContextType> normalTexture;
	public final Texture2D<ContextType> specularTexture;
	public final Texture2D<ContextType> roughnessTexture;
	public final Texture3D<ContextType> shadowTextures;
	public final UniformBuffer<ContextType> shadowMatrixBuffer;
	
	public static class Builder<ContextType extends Context<ContextType>>
	{
		private final ContextType context;
		private ViewSet viewSet;
		private VertexMesh geometry;
		private File imageDirectoryOverride;
		private IBRLoadOptions loadOptions;
		private IBRLoadingMonitor loadingMonitor;
		
		private float gamma;
		private double[] linearLuminanceValues;
		private byte[] encodedLuminanceValues;
		
		private Builder(ContextType context)
		{
			this.context = context;
		}
		
		public Builder<ContextType> setLoadOptions(IBRLoadOptions loadOptions)
		{
			this.loadOptions = loadOptions;
			return this;
		}
		
		public Builder<ContextType> setLoadingMonitor(IBRLoadingMonitor loadingMonitor)
		{
			this.loadingMonitor = loadingMonitor;
			return this;
		}
		
		public Builder<ContextType> setTonemapping(float gamma, double[] linearLuminanceValues, byte[] encodedLuminanceValues)
		{
			this.gamma = gamma;
			this.linearLuminanceValues = linearLuminanceValues;
			this.encodedLuminanceValues = encodedLuminanceValues;
			return this;
		}
		
		public Builder<ContextType> loadVSETFile(File vsetFile) throws IOException
		{
			this.viewSet = ViewSet.loadFromVSETFile(vsetFile);
			try
			{
				this.geometry = new VertexMesh("OBJ", this.viewSet.getGeometryFile());
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return this;
		}
		
		// undistorted images are defined in the load options
		public Builder<ContextType> loadAgisoftFiles(File cameraFile, File geometryFile, File undistortedImageDirectory) throws IOException
		{
			this.viewSet = ViewSet.loadFromAgisoftXMLFile(cameraFile);
			if (geometryFile != null)
			{
				this.geometry = new VertexMesh("OBJ", geometryFile);
			}
			if (undistortedImageDirectory != null)
			{
				this.imageDirectoryOverride = undistortedImageDirectory;
			}
			return this;
		}
		
		public Builder<ContextType> useExistingViewSet(ViewSet viewSet)
		{
			this.viewSet = viewSet;
			return this;
		}
		
		public Builder<ContextType> useExistingGeometry(VertexMesh geometry)
		{
			this.geometry = geometry;
			return this;
		}
		
		public Builder<ContextType> overrideImageDirectory(File imageDirectoryOverride)
		{
			this.imageDirectoryOverride = imageDirectoryOverride;
			return this;
		}
		
		public IBRResources<ContextType> create() throws IOException
		{
			if (linearLuminanceValues != null && encodedLuminanceValues != null)
			{
				viewSet.setTonemapping(gamma, linearLuminanceValues, encodedLuminanceValues);
			}

			if (imageDirectoryOverride != null)
			{
				viewSet.setRootDirectory(imageDirectoryOverride);
				viewSet.setRelativeImagePathName("");
			}
			
			return new IBRResources<ContextType>(context, viewSet, geometry, loadOptions, loadingMonitor);
		}
	}
	
	public static <ContextType extends Context<ContextType>> Builder<ContextType> getBuilderForContext(ContextType context)
	{
		return new IBRResources.Builder<ContextType>(context);
	}
	
	private IBRResources(ContextType context, ViewSet viewSet, VertexMesh geometry, IBRLoadOptions loadOptions, IBRLoadingMonitor loadingMonitor) throws IOException
	{
		this.context = context;
		this.viewSet = viewSet;
		this.geometry = geometry;
		
		// Store the poses in a uniform buffer
		if (viewSet.getCameraPoseData() != null)
		{
			// Create the uniform buffer
			cameraPoseBuffer = context.createUniformBuffer().setData(viewSet.getCameraPoseData());
		}
		else
		{
			cameraPoseBuffer = null;
		}
		
		// Store the camera projections in a uniform buffer
		if (viewSet.getCameraProjectionData() != null)
		{
			// Create the uniform buffer
			cameraProjectionBuffer = context.createUniformBuffer().setData(viewSet.getCameraProjectionData());
		}
		else
		{
			cameraProjectionBuffer = null;
		}
		
		// Store the camera projection indices in a uniform buffer
		if (viewSet.getCameraProjectionIndexData() != null)
		{
			cameraProjectionIndexBuffer = context.createUniformBuffer().setData(viewSet.getCameraProjectionIndexData());
		}
		else
		{
			cameraProjectionIndexBuffer = null;
		}
		
		// Store the light positions in a uniform buffer
		if (viewSet.getLightPositionData() != null)
		{
			// Create the uniform buffer
			lightPositionBuffer = context.createUniformBuffer().setData(viewSet.getLightPositionData());
		}
		else
		{
			lightPositionBuffer = null;
		}
		
		// Store the light positions in a uniform buffer
		if (viewSet.getLightIntensityData() != null)
		{
			// Create the uniform buffer
			lightIntensityBuffer = context.createUniformBuffer().setData(viewSet.getLightIntensityData());
		}
		else
		{
			lightIntensityBuffer = null;
		}
		
		// Store the light indices indices in a uniform buffer
		if (viewSet.getLightIndexData() != null)
		{
			lightIndexBuffer = context.createUniformBuffer().setData(viewSet.getLightIndexData());
		}
		else
		{
			lightIndexBuffer = null;
		}
		
		// Luminance map texture
		if (viewSet.getLuminanceEncoding() != null)
		{
			luminanceMap = viewSet.getLuminanceEncoding().createLuminanceMap(context);
			inverseLuminanceMap = viewSet.getLuminanceEncoding().createInverseLuminanceMap(context);
		}
		else
		{
			luminanceMap = null;
			inverseLuminanceMap = null;
		}
		
		// Read the images from a file
		if (loadOptions != null && loadOptions.areColorImagesRequested() && viewSet.getImageFilePath() != null && viewSet.getCameraPoseCount() > 0)
		{
			Date timestamp = new Date();
			File imageFile = viewSet.getImageFile(0);

			if (!imageFile.exists())
			{
				// Try some alternate file formats/extensions
				String[] altFormats = { "png", "PNG", "jpg", "JPG", "jpeg", "JPEG" };
				for(final String extension : altFormats)
				{
					String[] filenameParts = viewSet.getImageFileName(0).split("\\.");
			    	filenameParts[filenameParts.length - 1] = extension;
			    	String altFileName = String.join(".", filenameParts);
			    	File imageFileGuess = new File(imageFile.getParentFile(), altFileName);					
			    	
			    	System.out.printf("Trying '%s'\n", imageFileGuess.getAbsolutePath());
			    	if (imageFileGuess.exists())
			    	{
				    	System.out.printf("Found!!\n");
			    		imageFile = imageFileGuess;
			    		break;
			    	}
				}

				// Is it still not there?
				if (!imageFile.exists())
		    	{
		    		throw new FileNotFoundException(
		    				String.format("'%s' not found.", viewSet.getImageFileName(0)));
		    	}
			}
			
			// Read a single image to get the dimensions for the texture array
			InputStream input = new FileInputStream(imageFile); // myZip.retrieveFile(imageFile);
			BufferedImage img = ImageIO.read(input);
			if(img == null)
			{
				throw new IOException(String.format("Error: Unsupported image format '%s'.",
						viewSet.getImageFileName(0)));				
			}
			input.close();

			ColorTextureBuilder<ContextType, ? extends Texture3D<ContextType>> textureArrayBuilder = 
					context.get2DColorTextureArrayBuilder(img.getWidth(), img.getHeight(), viewSet.getCameraPoseCount());
			
			if (loadOptions.isCompressionRequested())
			{
				textureArrayBuilder.setInternalFormat(CompressionFormat.RGB_PUNCHTHROUGH_ALPHA1_4BPP);
			}
			else
			{
				textureArrayBuilder.setInternalFormat(ColorFormat.RGBA8);
			}
			
			if (loadOptions.areMipmapsRequested())
			{
				textureArrayBuilder.setMipmapsEnabled(true);
			}
			else
			{
				textureArrayBuilder.setMipmapsEnabled(false);
			}
			
			textureArrayBuilder.setLinearFilteringEnabled(true);
			textureArrayBuilder.setMaxAnisotropy(16.0f);
			colorTextures = textureArrayBuilder.createTexture();
			
			if(loadingMonitor != null) 
			{
				loadingMonitor.setMaximum(viewSet.getCameraPoseCount());
			}

			for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
			{
				imageFile = viewSet.getImageFile(i);
				if (!imageFile.exists())
				{
					// Try some alternate file formats/extensions
					String[] altFormats = { "png", "PNG", "jpg", "JPG", "jpeg", "JPEG" };
					for(final String extension : altFormats)
					{
						String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
				    	filenameParts[filenameParts.length - 1] = extension;
				    	String altFileName = String.join(".", filenameParts);
				    	File imageFileGuess = new File(imageFile.getParentFile(), altFileName);

				    	if (imageFileGuess.exists())
				    	{
				    		imageFile = imageFileGuess;
				    		break;
				    	}
					}

					// Is it still not there?
					if (!imageFile.exists())
			    	{
			    		throw new FileNotFoundException(
			    				String.format("'%s' not found.", viewSet.getImageFileName(i)));
			    	}
				}
				
				this.colorTextures.loadLayer(i, imageFile, true);

				if(loadingMonitor != null) 
				{
					loadingMonitor.setProgress(i+1);
				}
			}

			System.out.println("View Set textures loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		}
		else
		{
			this.colorTextures = null;
		}
		
		if (geometry != null)
		{
			this.positionBuffer = context.createVertexBuffer().setData(geometry.getVertices());
		}
		else
		{
			this.positionBuffer = null;
		}
		
		if (loadOptions.areDepthImagesRequested())
        {
	        // Build depth textures for each view
	    	this.depthTextures = 
    			context.get2DDepthTextureArrayBuilder(loadOptions.getDepthImageWidth(), loadOptions.getDepthImageHeight(), viewSet.getCameraPoseCount())
	    			.createTexture();
	    	
	    	try
	    	(
    			// Don't automatically generate any texture attachments for this framebuffer object
    			FramebufferObject<ContextType> depthRenderingFBO = 
    			context.getFramebufferObjectBuilder(loadOptions.getDepthImageWidth(), loadOptions.getDepthImageHeight())
	    			.createFramebufferObject(); 

    	    	// Load the program
    			Program<ContextType> depthRenderingProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"))
	    			.createProgram();
			)
	    	{
		    	Renderable<ContextType> depthRenderable = context.createRenderable(depthRenderingProgram);
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
		    	}
	    	}
        }
		else
		{
			this.depthTextures = null;
		}
		
		if (geometry != null && geometry.hasTexCoords())
        {
			this.texCoordBuffer = context.createVertexBuffer().setData(geometry.getTexCoords());
        }
		else
		{
			this.texCoordBuffer = null;
		}
        
        if (geometry != null && geometry.hasNormals())
        {
        	this.normalBuffer = context.createVertexBuffer().setData(geometry.getNormals());
        }
        else
        {
        	this.normalBuffer = null;
        }
        
        if (geometry != null && geometry.hasTexCoords() && geometry.hasNormals())
        {
        	this.tangentBuffer = context.createVertexBuffer().setData(geometry.getTangents());
        }
        else
        {
        	this.tangentBuffer = null;
        }
		
		String diffuseTextureName = null;
		String normalTextureName = null;
		String specularTextureName = null;
		String roughnessTextureName = null;
		
		// TODO Use more information from the material.  Currently just pulling texture names.
		if (this.geometry != null)
		{
			Material material = this.geometry.getMaterial();
			if (material != null)
			{
				if (material.getDiffuseMap() != null)
				{
					diffuseTextureName = material.getDiffuseMap().getMapName();
				}
	
				if (material.getNormalMap() != null)
				{
					normalTextureName = material.getNormalMap().getMapName();
				}
	
				if (material.getSpecularMap() != null)
				{
					specularTextureName = material.getSpecularMap().getMapName();
				}
	
				if (material.getRoughnessMap() != null)
				{
					roughnessTextureName = material.getRoughnessMap().getMapName();
				}
			}
		
			if (this.viewSet.getGeometryFileName() != null)
			{
				String prefix = this.viewSet.getGeometryFileName().split("\\.")[0];
				diffuseTextureName = diffuseTextureName != null ? diffuseTextureName : prefix + "_Kd.png";
				normalTextureName = normalTextureName != null ? normalTextureName : prefix + "_norm.png";
				specularTextureName = specularTextureName != null ? specularTextureName : prefix + "_Ks.png";
				roughnessTextureName = roughnessTextureName != null ? roughnessTextureName : prefix + "_Pr.png";
			}
			else
			{
				diffuseTextureName = diffuseTextureName != null ? diffuseTextureName : "diffuse.png";
				normalTextureName = normalTextureName != null ? normalTextureName : "normal.png";
				specularTextureName = specularTextureName != null ? specularTextureName : "specular.png";
				roughnessTextureName = roughnessTextureName != null ? roughnessTextureName : "roughness.png";
			}
			
			File diffuseFile = new File(this.geometry.getFilename().getParentFile(), diffuseTextureName);
			File normalFile = new File(this.geometry.getFilename().getParentFile(), normalTextureName);
			File specularFile = new File(this.geometry.getFilename().getParentFile(), specularTextureName);
			File roughnessFile = new File(this.geometry.getFilename().getParentFile(), roughnessTextureName);
			
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
		}
		else
		{
			diffuseTexture = null;
			normalTexture = null;
			specularTexture = null;
			roughnessTexture = null;
		}
		
		if (this.depthTextures != null)
        {
	    	shadowTextures = 
    			context.get2DDepthTextureArrayBuilder(this.depthTextures.getWidth(), this.depthTextures.getHeight(), this.viewSet.getCameraPoseCount())
	    			.createTexture();
	    	
	    	try
	    	(
    	    	// Don't automatically generate any texture attachments for this framebuffer object
		    	FramebufferObject<ContextType> depthRenderingFBO = 
	    			context.getFramebufferObjectBuilder(this.depthTextures.getWidth(), this.depthTextures.getHeight())
		    			.createFramebufferObject();
		    	
		    	// Load the program
		    	Program<ContextType> depthRenderingProgram = context.getShaderProgramBuilder()
		    			.addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
		    			.addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"))
		    			.createProgram();
			)
			{
		    	Renderable<ContextType> depthRenderable = context.createRenderable(depthRenderingProgram);
		    	depthRenderable.addVertexBuffer("position", this.positionBuffer);
	
		    	// Flatten the camera pose matrices into 16-component vectors and store them in the vertex list data structure.
		    	FloatVertexList flattenedShadowMatrices = new FloatVertexList(16, this.viewSet.getCameraPoseCount());
		    	
		    	// Render each depth texture
		    	for (int i = 0; i < this.viewSet.getCameraPoseCount(); i++)
		    	{
		    		depthRenderingFBO.setDepthAttachment(shadowTextures.getLayerAsFramebufferAttachment(i));
		        	depthRenderingFBO.clearDepthBuffer();
		        	
		        	depthRenderingProgram.setUniform("model_view", this.viewSet.getCameraPose(i));
		    		depthRenderingProgram.setUniform("projection", 
						this.viewSet.getCameraProjection(this.viewSet.getCameraProjectionIndex(i))
		    				.getProjectionMatrix(
	    						this.viewSet.getRecommendedNearPlane(), 
	    						this.viewSet.getRecommendedFarPlane()
							)
					);
		    		
		    		Matrix4 modelView = Matrix4.lookAt(new Vector3(this.viewSet.getCameraPoseInverse(i).times(new Vector4(this.viewSet.getLightPosition(0), 1.0f))), this.geometry.getCentroid(), new Vector3(0, 1, 0));
		        	depthRenderingProgram.setUniform("model_view", modelView);
		        	
		    		Matrix4 projection = this.viewSet.getCameraProjection(this.viewSet.getCameraProjectionIndex(i))
							.getProjectionMatrix(
								this.viewSet.getRecommendedNearPlane(), 
								this.viewSet.getRecommendedFarPlane() * 2 // double it for good measure
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
			}
        }
		else
		{
			shadowTextures = null;
			shadowMatrixBuffer = null;
		}
	}
	
	public void close()
	{
		if (this.cameraPoseBuffer != null)
		{
			this.cameraPoseBuffer.close();
		}
		
		if (this.cameraProjectionBuffer != null)
		{
			this.cameraProjectionBuffer.close();
		}
		
		if (this.cameraProjectionIndexBuffer != null)
		{
			this.cameraProjectionIndexBuffer.close();
		}
		
		if (this.lightPositionBuffer != null)
		{
			this.lightPositionBuffer.close();
		}
		
		if (this.lightIntensityBuffer != null)
		{
			this.lightIntensityBuffer.close();
		}
		
		if (this.lightIndexBuffer != null)
		{
			this.lightIndexBuffer.close();
		}
		
		if (this.positionBuffer != null)
		{
			this.positionBuffer.close();
		}
    	
    	if (this.texCoordBuffer != null)
		{
    		this.texCoordBuffer.close();
		}
    	
    	if (this.normalBuffer != null)
    	{
    		this.normalBuffer.close();
    	}
		
		if (this.colorTextures != null)
		{
			this.colorTextures.close();
		}
		
		if (depthTextures != null)
		{
			depthTextures.close();
		}
		
		if (diffuseTexture != null)
		{
			diffuseTexture.close();
		}
		
		if (normalTexture != null)
		{
			normalTexture.close();
		}
		
		if (specularTexture != null)
		{
			specularTexture.close();
		}
		
		if (roughnessTexture != null)
		{
			roughnessTexture.close();
		}
		
		if (shadowTextures != null)
		{
			shadowTextures.close();
		}
		
		if (shadowMatrixBuffer != null)
		{
			shadowMatrixBuffer.close();
		}
	}
}
