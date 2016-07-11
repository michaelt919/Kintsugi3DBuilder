/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package tetzlaff.lightfield;

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

/**
 * A class for organizing all of the GL resources that are necessary for light field rendering.
 * This includes a proxy geometry and depth textures for visibility testing, in addition to a view set.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the context that the GL resources for this light field are to be used with.
 */
public class LightField<ContextType extends Context<ContextType>>
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
    public final LFSettings settings;
    
    /**
     * The directory of the shader resources (to be wrapped in the JAR file).
     */
    public static final File SHADER_RESOURCE_DIRECTORY = new File("resources/shaders");
	
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
	public LightField(String id, ViewSet<ContextType> viewSet, VertexMesh proxy, Texture3D<ContextType> depthTextures, 
			VertexBuffer<ContextType> positionBuffer, VertexBuffer<ContextType> texCoordBuffer, VertexBuffer<ContextType> normalBuffer, LFSettings settings) 
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
	
	private static <ContextType extends Context<ContextType>> 
		Texture3D<ContextType> generateDepthTextures(ContextType context, LFLoadOptions loadOptions, ViewSet<ContextType> viewSet, VertexBuffer<ContextType> positionBuffer) throws IOException
	{
		Texture3D<ContextType> depthTextures;
		
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
	    			.addShader(ShaderType.VERTEX, new File(LightField.SHADER_RESOURCE_DIRECTORY, "depth.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File(LightField.SHADER_RESOURCE_DIRECTORY, "depth.frag"))
	    			.createProgram();
	    	Renderable<ContextType> depthRenderable = context.createRenderable(depthRenderingProgram);
	    	depthRenderable.addVertexBuffer("position", positionBuffer);
	    	
	    	depthRenderingFBO.setDepthAttachment(depthTextures.getLayerAsFramebufferAttachment(0));

	    	// Test framebuffer completeness.
	    	if (!depthRenderingFBO.isComplete())
    		{
    			// For some AMD cards - a color texture must always be bound to slot zero.
    			depthRenderingFBO.delete(); // Delete the old FBO
    			depthRenderingFBO = context.getFramebufferObjectBuilder(loadOptions.getDepthImageWidth(), loadOptions.getDepthImageHeight())
    					.addColorAttachment() // Default auto-created color attachment texture.
    	    			.createFramebufferObject();
    		}
	    	
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
    	finally
    	{
	    	if(depthRenderingProgram != null) 
	    	{ 
	    		depthRenderingProgram.delete(); 
    		}
	    	
	    	if(depthRenderingFBO != null) 
	    	{ 
	    		depthRenderingFBO.delete(); 
    		}
    	}
    	
    	return depthTextures;
	}

	/**
	 * Loads a camera definition file exported in XML format from Agisoft PhotoScan along with a specific geometric proxy 
	 * and initializes a corresponding LightField object with all associated GPU resources.
	 * @param xmlFile The Agisoft PhotoScan XML camera file to load.
	 * @param meshFile The mesh exported from Agisoft PhotoScan to be used as proxy geometry.
	 * @param loadOptions The requested options for loading the light field.
	 * @param context The GL context in which to create the resources.
	 * @param loadingCallback A callback for monitoring loading progress, particularly for images.
	 * @return The newly created LightField object.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	public static <ContextType extends Context<ContextType>>
		LightField<ContextType> loadFromAgisoftXMLFile(File xmlFile, File meshFile, LFLoadOptions loadOptions, ContextType context, LFLoadingMonitor loadingCallback) throws IOException
	{
		ViewSet<ContextType> viewSet;
		VertexMesh proxy;
		Texture3D<ContextType> depthTextures = null;
		
		File directoryPath = xmlFile.getParentFile();
        proxy = new VertexMesh("OBJ", meshFile);
        viewSet = ViewSet.loadFromAgisoftXMLFile(xmlFile, loadOptions.getImageOptions(), context, loadingCallback);
        VertexBuffer<ContextType> positionBuffer = context.createVertexBuffer().setData(proxy.getVertices());
        
        if (loadOptions.areDepthImagesRequested())
        {
        	depthTextures = generateDepthTextures(context, loadOptions, viewSet, positionBuffer);
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
        
    	return new LightField<ContextType>(directoryPath.getName(), viewSet, proxy, depthTextures, positionBuffer, texCoordBuffer, normalBuffer, new LFSettings());
	}
	
	/**
	 * Loads a VSET file and creates and initializes a corresponding LightField object with all associated GPU resources.
	 * @param vsetFile The VSET file to load.
	 * @param loadOptions The requested options for loading the light field.
	 * @param contextThe GL context in which to create the resources.
	 * @param loadingCallback A callback for monitoring loading progress, particularly for images.
	 * @return The newly created LightField object.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	public static <ContextType extends Context<ContextType>>
		LightField<ContextType> loadFromVSETFile(File vsetFile, LFLoadOptions loadOptions, ContextType context, LFLoadingMonitor loadingCallback) throws IOException
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
        	depthTextures = generateDepthTextures(context, loadOptions, viewSet, positionBuffer);
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
        
    	return new LightField<ContextType>(directoryPath.getName(), viewSet, proxy, depthTextures, positionBuffer, texCoordBuffer, normalBuffer, new LFSettings());
	}
	
	@Override
	public String toString()
	{
		return this.id;
	}
}
