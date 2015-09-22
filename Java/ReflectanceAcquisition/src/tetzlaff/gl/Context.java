package tetzlaff.gl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.builders.DepthStencilTextureBuilder;
import tetzlaff.gl.builders.FramebufferObjectBuilder;
import tetzlaff.gl.builders.DepthTextureBuilder;
import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.StencilTextureBuilder;
import tetzlaff.gl.builders.TextureBuilder;
import tetzlaff.gl.helpers.FloatVertexList;

/**
 * An interface for any OpenGL-like graphics context.
 * An implementation of this interface serves several purposes:
 * (1) it provides query functions for obtaining information about the GL state, and
 * (2) it serves as an access point for modifying global GL options like depth testing, multisampling, and back-face culling,
 * (3) it provides synchronization functions like flush(), finish(), and swapBuffers(),
 * (4) it acts as a factory for creating GL objects such as buffers, textures, and shaders.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> For concrete types, this type parameter should match the implementing class itself.
 * Abstract implementations should be parameterized so that subclasses can fulfill this constraint.
 * This type parameter ensures that all the objects created by this context are mutually compatible with each other and the context itself.
 */
public interface Context<ContextType extends Context<ContextType>>
{
	/**
	 * Whether or not the context has been destroyed.
	 * If the context has been destroyed, all other methods will have undefined results.
	 * @return true if the context has been destroyed, false otherwise.
	 */
	boolean isDestroyed();
	
	/**
	 * Designates this context as the "current" context that should be used for all subsequent GL operations.
	 * Currently, in a multi-context environment, it would be necessary to call this before using any objects created by this context.
	 * Future revisions may implement the automatic switching of contexts when necessary, in which case this method will be eliminated.
	 */
	void makeContextCurrent();

	/**
	 * Flushes all graphics commands for this context.
	 */
	void flush();
	
	/**
	 * Waits for this context to finish all pending graphics commands.
	 */
	void finish();
	
	/**
	 * Flushes all graphics commands for this context and swaps the back and front buffers in the default framebuffer.
	 */
	void swapBuffers();
	
	/**
	 * Destroys this context.
	 * This should free any GPU resources previously allocated to this context.
	 * After this method has been called, all other methods will have undefined results.
	 */
	void destroy();

	/**
	 * Gets the size of the default framebuffer for this context.
	 * @return The size of the default framebuffer.
	 */
	FramebufferSize getFramebufferSize();
	
	/**
	 * Enables depth testing for this context.
	 */
	void enableDepthTest();
	
	/**
	 * Disables depth testing for this context.
	 */
	void disableDepthTest();
	
	/**
	 * Enables multisampling for this context.
	 */
	void enableMultisampling();
	
	/**
	 * Disables multisampling for this context.
	 */
	void disableMultisampling();
	
	/**
	 * Enables back-face culling for this context.
	 */
	void enableBackFaceCulling();
	
	/**
	 * Disables back-face culling for this context.
	 */
	void disableBackFaceCulling();

	/**
	 * Enables alpha blending and sets the blending function to be used for this context.
	 * @param func The alpha blending function to be used.
	 */
	void setAlphaBlendingFunction(AlphaBlendingFunction func);
	
	/**
	 * Disables alpha blending for this context.
	 */
	void disableAlphaBlending();
	
	/**
	 * Gets the maximum number of words allowed across all vertex shader uniform blocks.
	 * @return The maximum number of words allowed across all vertex shader uniform blocks.
	 */
	int getMaxCombinedVertexUniformComponents();
	/**
	 * Gets the maximum number of words allowed across all fragment shader uniform blocks.
	 * @return The maximum number of words allowed across all fragment shader uniform blocks.
	 */
	int getMaxCombinedFragmentUniformComponents();
	
	/**
	 * Gets the maximum size of a uniform block.
	 * @return The maximum size of a uniform block.
	 */
	int getMaxUniformBlockSize();
	
	/**
	 * Gets the maximum number of uniform components allowed in a vertex shader.
	 * @return The maximum number of uniform components allowed in a vertex shader.
	 */
	int getMaxVertexUniformComponents();
	
	/**
	 * Gets the maximum number of uniform components allowed in a fragment shader.
	 * @return The maximum number of uniform components allowed in a fragment shader.
	 */
	int getMaxFragmentUniformComponents();
	
	/**
	 * Gets the maximum number of layers allowed in a texture array.
	 * @return The maximum number of layers allowed in a texture array.
	 */
	int getMaxArrayTextureLayers();
	
	/**
	 * Gets the maximum number of textures allowed.
	 * @return The maximum number of textures allowed.
	 */
	int getMaxCombinedTextureImageUnits();
	
	/**
	 * Gets the maximum number of uniform blocks allowed.
	 * @return Gets the maximum number of uniform blocks allowed.
	 */
	int getMaxCombinedUniformBlocks();
	
	/**
	 * Creates a new shader from a string containing the source code.
	 * @param type The type of shader to create.
	 * @param source The source code of the shader.
	 * @return The newly created shader.
	 */
	Shader<ContextType> createShader(ShaderType type, String source);
	
	/**
	 * Creates a new shader from a file containing the source code.
	 * @param type The type of shader to create.
	 * @param source The source code of the shader.
	 * @return The newly created shader.
	 */
	
	/**
	 * Creates a new shader from a file containing the source code.
	 * @param type The type of shader to create.
	 * @param file A file containing the source code of the shader.
	 * @return The newly created shader.
	 * @throws FileNotFoundException Upon an unrecoverable File I/O problem when reading the shader file.
	 */
	Shader<ContextType> createShader(ShaderType type, File file) throws FileNotFoundException;
	
	/**
	 * Gets a builder object for a shader program.
	 * @return A builder object for a shader program.
	 */
	ProgramBuilder<ContextType> getShaderProgramBuilder();
	
	/**
	 * Gets the default framebuffer.
	 * @return The default framebuffer.
	 */
	Framebuffer<ContextType> getDefaultFramebuffer();
	
	/**
	 * Gets a builder for a framebuffer object.
	 * @param width The width of the framebuffer.
	 * @param height The height of the framebuffer.
	 * @return The builder for a framebuffer object with the specified dimensions.
	 */
	FramebufferObjectBuilder<ContextType> getFramebufferObjectBuilder(int width, int height);
	
	/**
	 * Creates a new vertex buffer.
	 * @return The newly created vertex buffer.
	 */
	VertexBuffer<ContextType> createVertexBuffer();
	
	/**
	 * Creates a vertex buffer containing 4 vertices that can be rendered as a triangle fan to form a rectangle from [-1, -1] to [1, 1].
	 * @return The newly created vertex buffer for drawing a rectangle.
	 */
	default VertexBuffer<ContextType> createRectangle()
	{
		return this.createVertexBuffer().setData(new FloatVertexList(2, 4, new float[] { -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f }), false);
	}
	
	/**
	 * Creates a new index buffer.
	 * @return The newly created index buffer.
	 */
	IndexBuffer<ContextType> createIndexBuffer();
	
	/**
	 * Creates a new uniform buffer.
	 * @return The newly created uniform buffer.
	 */
	UniformBuffer<ContextType> createUniformBuffer();
	
	/**
	 * Creates a new renderable object.
	 * @param program The shader program to use with the renderable object.
	 * @return The newly created renderable object.
	 */
	Renderable<ContextType> createRenderable(Program<ContextType> program);
	
	/**
	 * Gets a builder object for a 2D color texture to be loaded from an arbitrary input stream along with a separate stream containing an alpha mask.
	 * @param imageStream An input stream containing the image in a format supported by Java's ImageIO library.
	 * @param maskStream An input stream containing the alpha mask in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
	 * @return The builder object for the texture.
	 * @throws IOException Upon an unrecoverable File I/O problem when reading the images.
	 */
	ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;

	default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(File imageFile, boolean flipVertical) throws IOException
	{
		return get2DColorTextureBuilder(new FileInputStream(imageFile), flipVertical);
	}
	
	/**
	 * Gets a builder object for a 2D color texture to be loaded from an arbitrary input stream.
	 * @param imageStream An input stream containing the image in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
	 * @return The builder object for the texture.
	 * @throws IOException Upon an unrecoverable File I/O problem when reading the images.
	 */
	default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(InputStream imageStream, boolean flipVertical) throws IOException
	{
		return get2DColorTextureBuilder(imageStream, null, flipVertical);
	}
	
	/**
	 * Gets a builder object for a 2D color texture to be loaded from a file along with an alpha mask in a separate file.
	 * @param imageFile A file containing the image in a format supported by Java's ImageIO library.
	 * @param maskFile A file containing the alpha mask in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
	 * @return The builder object for the texture.
	 * @throws IOException Upon an unrecoverable File I/O problem when reading the images.
	 */
	default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(File imageFile, File maskFile, boolean flipVertical) throws IOException
	{
		return get2DColorTextureBuilder(new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical);
	}
	
	/**
	 * Gets a builder object for a blank 2D color texture.
	 * @param width The width of the texture.
	 * @param height The height of the texture.
	 * @return The builder for the texture with the specified dimensions.
	 */
	ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(int width, int height);
	
	/**
	 * Gets a builder object for a blank 2D depth texture.
	 * @param width The width of the texture.
	 * @param height The height of the texture.
	 * @return The builder for the texture with the specified dimensions.
	 */
	DepthTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DDepthTextureBuilder(int width, int height);
	
	/**
	 * Gets a builder object for a blank 2D stencil texture.
	 * @param width The width of the texture.
	 * @param height The height of the texture.
	 * @return The builder for the texture with the specified dimensions.
	 */
	StencilTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DStencilTextureBuilder(int width, int height);
	
	/**
	 * Gets a builder object for a blank 2D depth+stencil texture.
	 * @param width The width of the texture.
	 * @param height The height of the texture.
	 * @return The builder for the texture with the specified dimensions.
	 */
	DepthStencilTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DDepthStencilTextureBuilder(int width, int height);
	
	/**
	 * Gets a builder object for a 2D texture to contain Perlin noise.
	 * @return The builder for the texture.
	 */
	TextureBuilder<ContextType, ? extends Texture2D<ContextType>> getPerlinNoiseTextureBuilder();

	/**
	 * Gets a builder object for a blank 2D color texture array.
	 * @param width The width of each texture in the array.
	 * @param height The height of each texture in the array.
	 * @param length The length of the texture array.
	 * @return The builder for the texture with the specified dimensions.
	 */
	ColorTextureBuilder<ContextType, ? extends Texture3D<ContextType>> get2DColorTextureArrayBuilder(int width, int height, int length);
	
	/**
	 * Gets a builder object for a blank 2D depth texture array.
	 * @param width The width of each texture in the array.
	 * @param height The height of each texture in the array.
	 * @param length The length of the texture array.
	 * @return The builder for the texture with the specified dimensions.
	 */
	DepthTextureBuilder<ContextType, ? extends Texture3D<ContextType>> get2DDepthTextureArrayBuilder(int width, int height, int length);
	
	/**
	 * Gets a builder object for a blank 2D stencil texture array.
	 * @param width The width of each texture in the array.
	 * @param height The height of each texture in the array.
	 * @param length The length of the texture array.
	 * @return The builder for the texture with the specified dimensions.
	 */
	StencilTextureBuilder<ContextType, ? extends Texture3D<ContextType>> get2DStencilTextureArrayBuilder(int width, int height, int length);
	
	/**
	 * Gets a builder object for a blank 2D depth+stencil texture array.
	 * @param width The width of each texture in the array.
	 * @param height The height of each texture in the array.
	 * @param length The length of the texture array.
	 * @return The builder for the texture with the specified dimensions.
	 */
	DepthStencilTextureBuilder<ContextType, ? extends Texture3D<ContextType>> get2DDepthStencilTextureArrayBuilder(int width, int height, int length);
}
