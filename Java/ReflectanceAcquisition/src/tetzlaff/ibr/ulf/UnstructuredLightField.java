package tetzlaff.ibr.ulf;

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
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.ibr.IBRLoadOptions;
import tetzlaff.ibr.IBRSettings;
import tetzlaff.ibr.ViewSet;
import tetzlaff.ibr.ViewSetImageOptions;

public class UnstructuredLightField<ContextType extends Context<ContextType>>
{
	public final String id;
	public final ViewSet<ContextType> viewSet;
	public final VertexMesh proxy;
	public final VertexBuffer<ContextType> positionBuffer;
	public final VertexBuffer<ContextType> texCoordBuffer;
	public final VertexBuffer<ContextType> normalBuffer;
	public final VertexBuffer<ContextType> tangentBuffer;
	public final Texture3D<ContextType> depthTextures;
    public final IBRSettings settings;
	
	public UnstructuredLightField(String id, ViewSet<ContextType> viewSet, VertexMesh proxy, Texture3D<ContextType> depthTextures, 
			VertexBuffer<ContextType> positionBuffer, VertexBuffer<ContextType> texCoordBuffer, VertexBuffer<ContextType> normalBuffer, 
			VertexBuffer<ContextType> tangentBuffer, IBRSettings settings) 
	{
    	this.id = id;
		this.viewSet = viewSet;
		this.proxy = proxy;
		this.depthTextures = depthTextures;
		this.positionBuffer = positionBuffer;
		this.texCoordBuffer = texCoordBuffer;
		this.normalBuffer = normalBuffer;
		this.tangentBuffer = tangentBuffer;
		this.settings = settings;
	}
	
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
	
	public static <ContextType extends Context<ContextType>> 
		UnstructuredLightField<ContextType> loadFromAgisoftXMLFile(File xmlFile, File meshFile, ContextType context) throws IOException
	{
		return UnstructuredLightField.loadFromAgisoftXMLFile(xmlFile, meshFile, null);
	}

	public static <ContextType extends Context<ContextType>>
		UnstructuredLightField<ContextType> loadFromAgisoftXMLFile(File xmlFile, File meshFile, IBRLoadOptions loadOptions, ContextType context) throws IOException
	{
		ViewSet<ContextType> viewSet;
		VertexMesh proxy;
		Texture3D<ContextType> depthTextures = null;
		
		File directoryPath = xmlFile.getParentFile();
        proxy = new VertexMesh("OBJ", meshFile);
        viewSet = ViewSet.loadFromAgisoftXMLFile(xmlFile, loadOptions.getImageOptions(), context);
        VertexBuffer<ContextType> positionBuffer = context.createVertexBuffer().setData(proxy.getVertices());
        
        if (loadOptions.getImageOptions().getFilePath() != null)
        {
	        // Build depth textures for each view
	    	int width = viewSet.getTextures().getWidth();
	    	int height = viewSet.getTextures().getHeight();
	    	depthTextures = context.get2DDepthTextureArrayBuilder(width, height, viewSet.getCameraPoseCount()).createTexture();
	    	
	    	// Don't automatically generate any texture attachments for this framebuffer object
	    	FramebufferObject<ContextType> depthRenderingFBO = context.getFramebufferObjectBuilder(width, height).createFramebufferObject();
	    	
	    	// Load the program
	    	Program<ContextType> depthRenderingProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"))
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

	    	depthRenderingProgram.delete();
	    	depthRenderingFBO.delete();
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
        
        VertexBuffer<ContextType> tangentBuffer = null;
        if (proxy.hasTexCoords() && proxy.hasNormals())
        {
        	tangentBuffer = context.createVertexBuffer().setData(proxy.getTangents());
        }
        
    	return new UnstructuredLightField<ContextType>(directoryPath.toString(), viewSet, proxy, depthTextures, 
    			positionBuffer, texCoordBuffer, normalBuffer, tangentBuffer, new IBRSettings());
	}
	
	public static <ContextType extends Context<ContextType>>
		UnstructuredLightField<ContextType> loadFromVSETFile(File vsetFile, ContextType context) throws IOException
	{
		return UnstructuredLightField.loadFromVSETFile(vsetFile, new IBRLoadOptions(new ViewSetImageOptions(null, false, false, false), false, 0, 0), context);
	}

	public static <ContextType extends Context<ContextType>>
		UnstructuredLightField<ContextType> loadFromVSETFile(File vsetFile, IBRLoadOptions loadOptions, ContextType context) throws IOException
	{
		ViewSet<ContextType> viewSet;
		VertexMesh proxy;
		Texture3D<ContextType> depthTextures = null;
		
		File directoryPath = vsetFile.getParentFile();
        viewSet = ViewSet.loadFromVSETFile(vsetFile, loadOptions.getImageOptions(), context);
        proxy = new VertexMesh("OBJ", viewSet.getGeometryFile());
        VertexBuffer<ContextType> positionBuffer = context.createVertexBuffer().setData(proxy.getVertices());
        
        if (loadOptions.areDepthImagesRequested())
        {
	        // Build depth textures for each view
	    	//int width = viewSet.getTextures().getWidth();
	    	//int height = viewSet.getTextures().getHeight();
	    	depthTextures = 
    			context.get2DDepthTextureArrayBuilder(loadOptions.getDepthImageWidth(), loadOptions.getDepthImageHeight(), viewSet.getCameraPoseCount())
	    			.createTexture();
	    	
	    	// Don't automatically generate any texture attachments for this framebuffer object
	    	FramebufferObject<ContextType> depthRenderingFBO = 
    			context.getFramebufferObjectBuilder(loadOptions.getDepthImageWidth(), loadOptions.getDepthImageHeight())
	    			.createFramebufferObject();
	    	
	    	// Load the program
	    	Program<ContextType> depthRenderingProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"))
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

	    	depthRenderingProgram.delete();
	    	depthRenderingFBO.delete();
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
        
        VertexBuffer<ContextType> tangentBuffer = null;
        if (proxy.hasTexCoords() && proxy.hasNormals())
        {
        	tangentBuffer = context.createVertexBuffer().setData(proxy.getTangents());
        }
        
    	return new UnstructuredLightField<ContextType>(directoryPath.toString(), viewSet, proxy, depthTextures, 
    			positionBuffer, texCoordBuffer, normalBuffer, tangentBuffer, new IBRSettings());
	}
	
	@Override
	public String toString()
	{
		return this.id.length() > 32 ? "..." + this.id.substring(this.id.length()-31, this.id.length()) : this.id;
	}
}
