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
import tetzlaff.gl.helpers.VertexMesh;

public class UnstructuredLightField<ContextType extends Context<ContextType>>
{
	public final File directoryPath;
	public final String id;
	public final ViewSet<ContextType> viewSet;
	public final VertexMesh proxy;
	public final VertexBuffer<ContextType> positionBuffer;
	public final VertexBuffer<ContextType> texCoordBuffer;
	public final VertexBuffer<ContextType> normalBuffer;
	public final Texture3D<ContextType> depthTextures;
    public final ULFSettings settings;
	
	public UnstructuredLightField(File directoryPath, ViewSet<ContextType> viewSet, VertexMesh proxy, Texture3D<ContextType> depthTextures, 
			VertexBuffer<ContextType> positionBuffer, VertexBuffer<ContextType> texCoordBuffer, VertexBuffer<ContextType> normalBuffer, ULFSettings settings) 
	{
    	this.id = directoryPath.getName();
		this.directoryPath = directoryPath;
		this.viewSet = viewSet;
		this.proxy = proxy;
		this.depthTextures = depthTextures;
		this.positionBuffer = positionBuffer;
		this.texCoordBuffer = texCoordBuffer;
		this.normalBuffer = normalBuffer;
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
		UnstructuredLightField<ContextType> loadFromAgisoftXMLFile(File xmlFile, File meshFile, File imageDirectory, ContextType context) throws IOException
	{
		ViewSet<ContextType> viewSet;
		VertexMesh proxy;
		Texture3D<ContextType> depthTextures = null;
		
		File directoryPath = xmlFile.getParentFile();
        proxy = new VertexMesh("OBJ", meshFile); // TODO don't have geometry filename hard-coded
        viewSet = ViewSet.loadFromAgisoftXMLFile(xmlFile, imageDirectory, context);
        VertexBuffer<ContextType> positionBuffer = context.createVertexBuffer().setData(proxy.getVertices());
        
        if (imageDirectory != null)
        {
	        // Build depth textures for each view
	    	int width = viewSet.getTextures().getWidth();
	    	int height = viewSet.getTextures().getHeight();
	    	depthTextures = context.get2DDepthTextureArrayBuilder(width, height, viewSet.getCameraPoseCount()).createTexture();
	    	
	    	// Don't automatically generate any texture attachments for this framebuffer object
	    	FramebufferObject<ContextType> depthRenderingFBO = context.getFramebufferObjectBuilder(width, height).createFramebufferObject();
	    	
	    	// Load the program
	    	Program<ContextType> depthRenderingProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.Vertex, new File("shaders/depth.vert"))
	    			.addShader(ShaderType.Fragment, new File("shaders/depth.frag"))
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
        
    	return new UnstructuredLightField<ContextType>(directoryPath, viewSet, proxy, depthTextures, positionBuffer, texCoordBuffer, normalBuffer, new ULFSettings());
	}
	
	public static <ContextType extends Context<ContextType>>
		UnstructuredLightField<ContextType> loadFromVSETFile(File vsetFile, ContextType context) throws IOException
	{
		return UnstructuredLightField.loadFromVSETFile(vsetFile, context, true);
	}

	public static <ContextType extends Context<ContextType>>
		UnstructuredLightField<ContextType> loadFromVSETFile(File vsetFile, ContextType context, boolean loadImages) throws IOException
	{
		ViewSet<ContextType> viewSet;
		VertexMesh proxy;
		Texture3D<ContextType> depthTextures = null;
		
		File directoryPath = vsetFile.getParentFile();
        proxy = new VertexMesh("OBJ", new File(directoryPath, "manifold.obj")); // TODO don't have geometry filename hard-coded
        viewSet = ViewSet.loadFromVSETFile(vsetFile, loadImages, context);
        VertexBuffer<ContextType> positionBuffer = context.createVertexBuffer().setData(proxy.getVertices());
        
        if (loadImages)
        {
	        // Build depth textures for each view
	    	int width = viewSet.getTextures().getWidth();
	    	int height = viewSet.getTextures().getHeight();
	    	depthTextures = context.get2DDepthTextureArrayBuilder(width, height, viewSet.getCameraPoseCount()).createTexture();
	    	
	    	// Don't automatically generate any texture attachments for this framebuffer object
	    	FramebufferObject<ContextType> depthRenderingFBO = context.getFramebufferObjectBuilder(width, height).createFramebufferObject();
	    	
	    	// Load the program
	    	Program<ContextType> depthRenderingProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.Vertex, new File("shaders/depth.vert"))
	    			.addShader(ShaderType.Fragment, new File("shaders/depth.frag"))
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
        
    	return new UnstructuredLightField<ContextType>(directoryPath, viewSet, proxy, depthTextures, positionBuffer, texCoordBuffer, normalBuffer, new ULFSettings());
	}
	
	@Override
	public String toString()
	{
		return this.id;
	}
}
