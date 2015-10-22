package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.exceptions.GLException;
import tetzlaff.gl.helpers.VertexMesh;

/**
 * A class for organizing all of the GL resources that are necessary for light field rendering.
 * This includes a proxy geometry and depth textures for visibility testing, in addition to a view set.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the context that the GL resources for this unstructured light field are to be used with.
 */
public class UnstructuredLightField<ContextType extends Context<ContextType>>
{
	/**
	 * An arbitrary user-friendly identifier.
	 */
	public final String id;
	
	/**
	 * The view set containing the light field images and their associated camera poses.
	 */
	public final ViewSet<ContextType> viewSet;
	
	/**
	 * The light field proxy geometry.
	 */
	public final VertexMesh proxy;
	
	/**
	 * A vertex buffer containing the positions of the vertices in the proxy geometry as 3D vectors.
	 */
	public final VertexBuffer<ContextType> positionBuffer;
	
	/**
	 * A vertex buffer containing the texture coordinates of the vertices in the proxy geometry as 2D vectors.
	 */
	public final VertexBuffer<ContextType> texCoordBuffer;
	
	/**
	 * A vertex buffer containing the surface normals of the vertices in the proxy geometry as 3D vectors.
	 */
	public final VertexBuffer<ContextType> normalBuffer;
	
	/**
	 * A texture array containing a depth image for each view.
	 */
	public final Texture3D<ContextType> depthTextures;
	
	/**
	 * The current rendering settings.
	 */
    public final ULFSettings settings;
	
    /**
     * Creates a new unstructured light field.
     * @param id An arbitrary user-friendly identifier.
     * @param viewSet The view set containing the light field images and their associated camera poses.
     * @param proxy The light field proxy geometry.
     * @param depthTextures A texture array containing a depth image for each view.
     * @param positionBuffer A vertex buffer containing the positions of the vertices in the proxy geometry as 3D vectors.
     * @param texCoordBuffer A vertex buffer containing the texture coordinates of the vertices in the proxy geometry as 2D vectors.
     * @param normalBuffer A vertex buffer containing the surface normals of the vertices in the proxy geometry as 3D vectors.
     * @param settings The current rendering settings.
     */
	public UnstructuredLightField(String id, ViewSet<ContextType> viewSet, VertexMesh proxy, Texture3D<ContextType> depthTextures, 
			VertexBuffer<ContextType> positionBuffer, VertexBuffer<ContextType> texCoordBuffer, VertexBuffer<ContextType> normalBuffer, ULFSettings settings) 
	{
    	this.id = id;
		this.viewSet = viewSet;
		this.proxy = proxy;
		this.depthTextures = depthTextures;
		this.positionBuffer = positionBuffer;
		this.texCoordBuffer = texCoordBuffer;
		this.normalBuffer = normalBuffer;
		this.settings = settings;
	}
	
	/**
	 * Deletes all GL resources associated with this light field (including resources associated with the underlying view set).
	 * Attempting to use these resources after calling this method will have undefined results.
	 */
	public void deleteOpenGLResources()
	{
    	this.positionBuffer.delete();
    	
    	if (this.texCoordBuffer != null)
		{
    		this.texCoordBuffer.delete();
		}
    	
    	if (this.normalBuffer != null)
    	{
    		this.normalBuffer.delete();
    	}
    	
		viewSet.deleteOpenGLResources();
		
		if (depthTextures != null)
		{
			depthTextures.delete();
		}
	}

	/**
	 * Loads a camera definition file exported in XML format from Agisoft PhotoScan along with a specific geometric proxy 
	 * and initializes a corresponding UnstructuredLightField object with all associated GPU resources.
	 * @param xmlFile The Agisoft PhotoScan XML camera file to load.
	 * @param meshFile The mesh exported from Agisoft PhotoScan to be used as proxy geometry.
	 * @param loadOptions The requested options for loading the unstructured light field.
	 * @param context The GL context in which to create the resources.
	 * @param loadingCallback A callback for monitoring loading progress, particularly for images.
	 * @return The newly created UnstructuredLightField object.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	public static <ContextType extends Context<ContextType>>
		UnstructuredLightField<ContextType> loadFromAgisoftXMLFile(File xmlFile, File meshFile, ULFLoadOptions loadOptions, ContextType context, ULFLoadingMonitor loadingCallback) throws IOException
	{
		ViewSet<ContextType> viewSet;
		VertexMesh proxy;
		Texture3D<ContextType> depthTextures = null;
		
		File directoryPath = xmlFile.getParentFile();
        proxy = new VertexMesh("OBJ", meshFile);
        viewSet = ViewSet.loadFromAgisoftXMLFile(xmlFile, loadOptions.getImageOptions(), context, loadingCallback);
        VertexBuffer<ContextType> positionBuffer = context.createVertexBuffer().setData(proxy.getVertices());
        
        if (loadOptions.getImageOptions().getFilePath() != null)
        {
        	FramebufferObject<ContextType> depthRenderingFBO = null;
        	Program<ContextType> depthRenderingProgram = null;
        	
	    	try
	    	{
		        // Build depth textures for each view
		    	int width = viewSet.getTextures().getWidth();
		    	int height = viewSet.getTextures().getHeight();
		    	depthTextures = context.get2DDepthTextureArrayBuilder(width, height, viewSet.getCameraPoseCount()).createTexture();
		    	
		    	// Don't automatically generate any texture attachments for this framebuffer object
		    	depthRenderingFBO = context.getFramebufferObjectBuilder(width, height).createFramebufferObject();
		    	
		    	// Load the program
		    	depthRenderingProgram = context.getShaderProgramBuilder()
		    			.addShader(ShaderType.VERTEX, new File("shaders/depth.vert"))
		    			.addShader(ShaderType.FRAGMENT, new File("shaders/depth.frag"))
		    			.createProgram();
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
	    	catch(GLException gle)
	    	{
	    		gle.printStackTrace();
	    		throw new IOException("OpenGL Error: Unable to render due to incomplete framebuffer (possibly out of video memory).", gle);
	    	}
	    	finally
	    	{
		    	if(depthRenderingProgram != null) { depthRenderingProgram.delete(); }
		    	if(depthRenderingFBO != null) { depthRenderingFBO.delete(); }
	    	}
        }
        
        VertexBuffer<ContextType> texCoordBuffer = null;
        if (proxy.hasTexCoords())
        {
        	texCoordBuffer = context.createVertexBuffer().setData(proxy.getTexCoords());
        }
        
        VertexBuffer<ContextType> normalBuffer = null;
        if (proxy.hasNormals())
        {
        	normalBuffer = context.createVertexBuffer().setData(proxy.getNormals());
        }
        
    	return new UnstructuredLightField<ContextType>(directoryPath.getName(), viewSet, proxy, depthTextures, positionBuffer, texCoordBuffer, normalBuffer, new ULFSettings());
	}
	
	/**
	 * Loads a VSET file and creates and initializes a corresponding UnstructuredLightField object with all associated GPU resources.
	 * @param vsetFile The VSET file to load.
	 * @param loadOptions The requested options for loading the unstructured light field.
	 * @param contextThe GL context in which to create the resources.
	 * @param loadingCallback A callback for monitoring loading progress, particularly for images.
	 * @return The newly created UnstructuredLightField object.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	public static <ContextType extends Context<ContextType>>
		UnstructuredLightField<ContextType> loadFromVSETFile(File vsetFile, ULFLoadOptions loadOptions, ContextType context, ULFLoadingMonitor loadingCallback) throws IOException
	{
		ViewSet<ContextType> viewSet;
		VertexMesh proxy;
		Texture3D<ContextType> depthTextures = null;
		
		File directoryPath = vsetFile.getParentFile();
        proxy = new VertexMesh("OBJ", new File(directoryPath, "manifold.obj")); // TODO don't have geometry filename hard-coded
        viewSet = ViewSet.loadFromVSETFile(vsetFile, loadOptions.getImageOptions(), context, loadingCallback);
        VertexBuffer<ContextType> positionBuffer = context.createVertexBuffer().setData(proxy.getVertices());
        
        if (loadOptions.areDepthImagesRequested())
        {
        	FramebufferObject<ContextType> depthRenderingFBO = null;
        	Program<ContextType> depthRenderingProgram = null;
        	
	    	try
	    	{
		    	depthTextures = 
	    			context.get2DDepthTextureArrayBuilder(loadOptions.getDepthImageWidth(), loadOptions.getDepthImageHeight(), viewSet.getCameraPoseCount())
		    			.createTexture();
		    	
		    	// Don't automatically generate any texture attachments for this framebuffer object
		    	depthRenderingFBO = 
	    			context.getFramebufferObjectBuilder(loadOptions.getDepthImageWidth(), loadOptions.getDepthImageHeight())
		    			.createFramebufferObject();
		    	
		    	// Load the program
		    	depthRenderingProgram = context.getShaderProgramBuilder()
		    			.addShader(ShaderType.VERTEX, new File("shaders/depth.vert"))
		    			.addShader(ShaderType.FRAGMENT, new File("shaders/depth.frag"))
		    			.createProgram();
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
	    	catch(GLException gle)
	    	{
	    		gle.printStackTrace();
	    		throw new IOException("OpenGL Error: Unable to render due to incomplete framebuffer (possibly out of video memory).", gle);	    		
	    	}
	    	finally
	    	{
		    	if(depthRenderingProgram != null) { depthRenderingProgram.delete(); }
		    	if(depthRenderingFBO != null) { depthRenderingFBO.delete(); }
	    	}
        }
        
        VertexBuffer<ContextType> texCoordBuffer = null;
        if (proxy.hasTexCoords())
        {
        	texCoordBuffer = context.createVertexBuffer().setData(proxy.getTexCoords());
        }
        
        VertexBuffer<ContextType> normalBuffer = null;
        if (proxy.hasNormals())
        {
        	normalBuffer = context.createVertexBuffer().setData(proxy.getNormals());
        }
        
    	return new UnstructuredLightField<ContextType>(directoryPath.getName(), viewSet, proxy, depthTextures, positionBuffer, texCoordBuffer, normalBuffer, new ULFSettings());
	}
	
	@Override
	public String toString()
	{
		return this.id;
	}
}
