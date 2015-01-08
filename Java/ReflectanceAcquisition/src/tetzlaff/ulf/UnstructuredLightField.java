package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.gl.opengl.OpenGLFramebufferObject;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLTextureArray;

public class UnstructuredLightField 
{
	public final String id;
	public final ViewSet viewSet;
	public final VertexMesh proxy;
	public final OpenGLTextureArray depthTextures;
    public final ULFSettings settings;
	
	public UnstructuredLightField(String id, ViewSet viewSet, VertexMesh proxy, OpenGLTextureArray depthTextures, ULFSettings settings) 
	{
		this.id = id;
		this.viewSet = viewSet;
		this.proxy = proxy;
		this.depthTextures = depthTextures;
		this.settings = settings;
	}
	
	public void deleteOpenGLResources()
	{
		viewSet.deleteOpenGLResources();
		depthTextures.delete();
	}

	public static UnstructuredLightField loadFromDirectory(String directoryPath) throws IOException
	{
		ViewSet viewSet;
		VertexMesh proxy;
		OpenGLTextureArray depthTextures;
		
        proxy = new VertexMesh("OBJ", directoryPath + "/manifold.obj");
        viewSet = ViewSet.loadFromVSETFile(directoryPath + "/default.vset");
        
        // Build depth textures for each view
    	int width = viewSet.getTextures().getWidth();
    	int height = viewSet.getTextures().getHeight();
    	depthTextures = OpenGLTextureArray.createDepthTextureArray(width, height, viewSet.getCameraPoseCount());
    	
    	// Don't automatically generate any texture attachments for this framebuffer object
    	OpenGLFramebufferObject depthRenderingFBO = new OpenGLFramebufferObject(width, height, 0, false);
    	
    	// Load the program
    	OpenGLProgram depthRenderingProgram = new OpenGLProgram(new File("shaders/depth.vert"), new File("shaders/depth.frag"));
    	OpenGLRenderable depthRenderable = new OpenGLRenderable(depthRenderingProgram);
    	depthRenderable.addVertexMesh("position", null, null, proxy);
    	
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
    	
    	String[] pathParts = directoryPath.split("[\\\\\\/]");
    	String id = pathParts[pathParts.length - 1];
    	return new UnstructuredLightField(id, viewSet, proxy, depthTextures, new ULFSettings());
	}
	
	@Override
	public String toString()
	{
		return this.id;
	}
}
