package tetzlaff.gl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import tetzlaff.gl.builders.ColorCubemapBuilder;
import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.builders.DepthStencilTextureBuilder;
import tetzlaff.gl.builders.DepthTextureBuilder;
import tetzlaff.gl.builders.FramebufferObjectBuilder;
import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.StencilTextureBuilder;
import tetzlaff.gl.builders.TextureBuilder;
import tetzlaff.gl.nativelist.NativeByteVectorList;
import tetzlaff.gl.nativelist.NativeDoubleVectorList;
import tetzlaff.gl.nativelist.NativeFloatVectorList;
import tetzlaff.gl.nativelist.NativeIntVectorList;
import tetzlaff.gl.nativelist.NativeShortVectorList;

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
	void makeContextCurrent();

	void flush();
	void finish();
	void swapBuffers();

	FramebufferSize getFramebufferSize();
	
	void enableDepthTest();
	void disableDepthTest();
	
	void enableMultisampling();
	void disableMultisampling();
	
	void enableBackFaceCulling();
	void disableBackFaceCulling();

	void setAlphaBlendingFunction(AlphaBlendingFunction func);
	void disableAlphaBlending();
	
	int getMaxCombinedVertexUniformComponents();
	int getMaxCombinedFragmentUniformComponents();
	int getMaxUniformBlockSize();
	int getMaxVertexUniformComponents();
	int getMaxFragmentUniformComponents();
	int getMaxArrayTextureLayers();
	int getMaxCombinedTextureImageUnits();
	int getMaxCombinedUniformBlocks();
	
	Shader<ContextType> createShader(ShaderType type, String source);
	Shader<ContextType> createShader(ShaderType type, File file) throws FileNotFoundException;
	ProgramBuilder<ContextType> getShaderProgramBuilder();
	
	Framebuffer<ContextType> getDefaultFramebuffer();
	FramebufferObjectBuilder<ContextType> getFramebufferObjectBuilder(int width, int height);
	
	VertexBuffer<ContextType> createVertexBuffer();
	
	default VertexBuffer<ContextType> createRectangle()
	{
		return this.createVertexBuffer().setData(new NativeFloatVectorList(2, 4, new float[] { -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f }), false);
	}
	
	IndexBuffer<ContextType> createIndexBuffer();
	UniformBuffer<ContextType> createUniformBuffer();
	
	Drawable<ContextType> createDrawable(Program<ContextType> program);
	
	ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> 
		get2DColorTextureBuilder(InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;
	ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> 
		get2DColorTextureFromHDRBuilder(BufferedInputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;
	
	default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> 
		get2DColorTextureBuilder(InputStream imageStream, boolean flipVertical) throws IOException
	{
		return get2DColorTextureBuilder(imageStream, null, flipVertical);
	}
	
	default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> 
		get2DColorTextureBuilder(File imageFile, File maskFile, boolean flipVertical) throws IOException
	{
		if (imageFile.getName().endsWith(".hdr"))
		{
			return get2DColorTextureFromHDRBuilder(new BufferedInputStream(new FileInputStream(imageFile)), new FileInputStream(maskFile), flipVertical);
		}
		else
		{
			return get2DColorTextureBuilder(new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical);
		}
	}

	default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> 
		get2DColorTextureFromHDRBuilder(BufferedInputStream imageStream, boolean flipVertical) throws IOException
	{
		return get2DColorTextureFromHDRBuilder(imageStream, null, flipVertical);
	}
	
	default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> 
		get2DColorTextureBuilder(File imageFile, boolean flipVertical) throws IOException
	{
		if (imageFile.getName().endsWith(".hdr"))
		{
			return get2DColorTextureFromHDRBuilder(new BufferedInputStream(new FileInputStream(imageFile)), flipVertical);
		}
		else
		{
			return get2DColorTextureBuilder(new FileInputStream(imageFile), flipVertical);
		}
	}

	ColorTextureBuilder<ContextType, ? extends Texture1D<ContextType>> get1DColorTextureBuilder(NativeByteVectorList data);
	ColorTextureBuilder<ContextType, ? extends Texture1D<ContextType>> get1DColorTextureBuilder(NativeShortVectorList data);
	ColorTextureBuilder<ContextType, ? extends Texture1D<ContextType>> get1DColorTextureBuilder(NativeIntVectorList data);
	ColorTextureBuilder<ContextType, ? extends Texture1D<ContextType>> get1DColorTextureBuilder(NativeFloatVectorList data);
	ColorTextureBuilder<ContextType, ? extends Texture1D<ContextType>> get1DColorTextureBuilder(NativeDoubleVectorList data);
	
	ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(int width, int height, NativeByteVectorList data);
	ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(int width, int height, NativeShortVectorList data);
	ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(int width, int height, NativeIntVectorList data);
	ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(int width, int height, NativeFloatVectorList data);
	ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(int width, int height, NativeDoubleVectorList data);
	
	ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(int width, int height);
	DepthTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DDepthTextureBuilder(int width, int height);
	StencilTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DStencilTextureBuilder(int width, int height);
	DepthStencilTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DDepthStencilTextureBuilder(int width, int height);
	
	TextureBuilder<ContextType, ? extends Texture2D<ContextType>> getPerlinNoiseTextureBuilder();

	ColorTextureBuilder<ContextType, ? extends Texture3D<ContextType>> get2DColorTextureArrayBuilder(int width, int height, int length);
	DepthTextureBuilder<ContextType, ? extends Texture3D<ContextType>> get2DDepthTextureArrayBuilder(int width, int height, int length);
	StencilTextureBuilder<ContextType, ? extends Texture3D<ContextType>> get2DStencilTextureArrayBuilder(int width, int height, int length);
	DepthStencilTextureBuilder<ContextType, ? extends Texture3D<ContextType>> get2DDepthStencilTextureArrayBuilder(int width, int height, int length);

	ColorCubemapBuilder<ContextType, ? extends Cubemap<ContextType>> getColorCubemapBuilder(int faceSize) throws IOException;
	DepthTextureBuilder<ContextType, ? extends Cubemap<ContextType>> getDepthCubemapBuilder(int faceSize) throws IOException;
	StencilTextureBuilder<ContextType, ? extends Cubemap<ContextType>> getStencilCubemapBuilder(int faceSize) throws IOException;
	DepthStencilTextureBuilder<ContextType, ? extends Cubemap<ContextType>> getDepthStencilCubemapBuilder(int faceSize) throws IOException;
}
