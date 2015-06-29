package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.gl.opengl.OpenGLFramebufferObject;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLResource;
import tetzlaff.gl.opengl.OpenGLTextureArray;
import tetzlaff.gl.opengl.OpenGLVertexBuffer;

public class UnstructuredLightField 
{
	public final File directoryPath;
	public final String id;
	public final ViewSet viewSet;
	public final VertexMesh proxy;
	public final OpenGLVertexBuffer positionBuffer;
	public final OpenGLVertexBuffer texCoordBuffer;
	public final OpenGLVertexBuffer normalBuffer;
	public final OpenGLTextureArray depthTextures;
    public final ULFSettings settings;
	
	public UnstructuredLightField(File directoryPath, ViewSet viewSet, VertexMesh proxy, OpenGLTextureArray depthTextures, 
			OpenGLVertexBuffer positionBuffer, OpenGLVertexBuffer texCoordBuffer, OpenGLVertexBuffer normalBuffer, ULFSettings settings) 
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
	
	public static UnstructuredLightField loadFromAgisoftXMLFile(File xmlFile, File meshFile) throws IOException
	{
		return UnstructuredLightField.loadFromAgisoftXMLFile(xmlFile, meshFile, null);
	}

	public static UnstructuredLightField loadFromAgisoftXMLFile(File xmlFile, File meshFile, File imageDirectory) throws IOException
	{
		ViewSet viewSet;
		VertexMesh proxy;
		OpenGLTextureArray depthTextures = null;
		
		File directoryPath = xmlFile.getParentFile();
        proxy = new VertexMesh("OBJ", meshFile); // TODO don't have geometry filename hard-coded
        viewSet = ViewSet.loadFromAgisoftXMLFile(xmlFile, imageDirectory);
        OpenGLVertexBuffer positionBuffer = new OpenGLVertexBuffer(proxy.getVertices());
        
        if (imageDirectory != null)
        {
	        // Build depth textures for each view
	    	int width = viewSet.getTextures().getWidth();
	    	int height = viewSet.getTextures().getHeight();
	    	depthTextures = OpenGLTextureArray.createDepthTextureArray(width, height, viewSet.getCameraPoseCount());
	    	
	    	// Don't automatically generate any texture attachments for this framebuffer object
	    	OpenGLFramebufferObject depthRenderingFBO = new OpenGLFramebufferObject(width, height, 0, false, false);
	    	
	    	// Load the program
	    	OpenGLProgram depthRenderingProgram = new OpenGLProgram(new File("shaders/depth.vert"), new File("shaders/depth.frag"));
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
	    	}

	    	depthRenderingProgram.delete();
	    	depthRenderingFBO.delete();
        }
        
        OpenGLVertexBuffer texCoordBuffer = null;
        if (proxy.hasTexCoords())
        {
        	texCoordBuffer = new OpenGLVertexBuffer(proxy.getTexCoords());
        }
        
        OpenGLVertexBuffer normalBuffer = null;
        if (proxy.hasNormals())
        {
        	normalBuffer = new OpenGLVertexBuffer(proxy.getNormals());
        }
        
    	return new UnstructuredLightField(directoryPath, viewSet, proxy, depthTextures, positionBuffer, texCoordBuffer, normalBuffer, new ULFSettings());
	}
	
	public static UnstructuredLightField loadFromVSETFile(File vsetFile) throws IOException
	{
		return UnstructuredLightField.loadFromVSETFile(vsetFile, true);
	}

	public static UnstructuredLightField loadFromVSETFile(File vsetFile, boolean loadImages) throws IOException
	{
		ViewSet viewSet;
		VertexMesh proxy;
		OpenGLTextureArray depthTextures = null;
		
		File directoryPath = vsetFile.getParentFile();
        proxy = new VertexMesh("OBJ", new File(directoryPath, "manifold.obj")); // TODO don't have geometry filename hard-coded
        viewSet = ViewSet.loadFromVSETFile(vsetFile, loadImages);
        OpenGLVertexBuffer positionBuffer = new OpenGLVertexBuffer(proxy.getVertices());
        
        if (loadImages)
        {
	        // Build depth textures for each view
	    	int width = viewSet.getTextures().getWidth();
	    	int height = viewSet.getTextures().getHeight();
	    	depthTextures = OpenGLTextureArray.createDepthTextureArray(width, height, viewSet.getCameraPoseCount());
	    	
	    	// Don't automatically generate any texture attachments for this framebuffer object
	    	OpenGLFramebufferObject depthRenderingFBO = new OpenGLFramebufferObject(width, height, 0, false, false);
	    	
	    	// Load the program
	    	OpenGLProgram depthRenderingProgram = new OpenGLProgram(new File("shaders/depth.vert"), new File("shaders/depth.frag"));
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
	    	}

	    	depthRenderingProgram.delete();
	    	depthRenderingFBO.delete();
        }
        
        OpenGLVertexBuffer texCoordBuffer = null;
        if (proxy.hasTexCoords())
        {
        	texCoordBuffer = new OpenGLVertexBuffer(proxy.getTexCoords());
        }
        
        OpenGLVertexBuffer normalBuffer = null;
        if (proxy.hasNormals())
        {
        	normalBuffer = new OpenGLVertexBuffer(proxy.getNormals());
        }
        
    	return new UnstructuredLightField(directoryPath, viewSet, proxy, depthTextures, positionBuffer, texCoordBuffer, normalBuffer, new ULFSettings());
	}
	
	@Override
	public String toString()
	{
		return this.id;
	}
}
