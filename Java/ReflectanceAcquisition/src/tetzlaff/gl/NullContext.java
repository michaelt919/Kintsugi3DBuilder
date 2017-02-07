package tetzlaff.gl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import tetzlaff.gl.AlphaBlendingFunction;
import tetzlaff.gl.Context;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.IndexBuffer;
import tetzlaff.gl.Program;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.Shader;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture1D;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.builders.DepthStencilTextureBuilder;
import tetzlaff.gl.builders.DepthTextureBuilder;
import tetzlaff.gl.builders.FramebufferObjectBuilder;
import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.StencilTextureBuilder;
import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

public abstract class NullContext implements Context<NullContext>
{
	@Override
	public void flush()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void finish()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void enableDepthTest()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void disableDepthTest()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void enableMultisampling()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void disableMultisampling()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void enableBackFaceCulling()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void disableBackFaceCulling()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setAlphaBlendingFunction(AlphaBlendingFunction func)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void disableAlphaBlending()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int getMaxCombinedVertexUniformComponents()
	{
		return 0;
	}
	
	@Override
	public int getMaxCombinedFragmentUniformComponents()
	{
		return 0;
	}
	
	@Override
	public int getMaxUniformBlockSize()
	{
		return 0;
	}
	
	@Override
	public int getMaxVertexUniformComponents()
	{
		return 0;
	}
	
	@Override
	public int getMaxFragmentUniformComponents()
	{
		return 0;
	}
	
	@Override
	public int getMaxArrayTextureLayers()
	{
		return 0;
	}
	
	@Override
	public int getMaxCombinedTextureImageUnits()
	{
		return 0;
	}
	
	@Override
	public int getMaxCombinedUniformBlocks()
	{
		return 0;
	}
	
	@Override
	public Shader<NullContext> createShader(ShaderType type, String source) 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Shader<NullContext> createShader(ShaderType type, File file) throws FileNotFoundException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ProgramBuilder<NullContext> getShaderProgramBuilder() 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Framebuffer<NullContext> getDefaultFramebuffer() 
	{
		return null;
	}

	@Override
	public VertexBuffer<NullContext> createVertexBuffer() 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IndexBuffer<NullContext> createIndexBuffer() 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public UniformBuffer<NullContext> createUniformBuffer() 
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Renderable<NullContext> createRenderable(Program<NullContext> program)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public FramebufferObjectBuilder<NullContext> getFramebufferObjectBuilder(int width, int height)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture1D<NullContext>> get1DColorTextureBuilder(ByteVertexList data)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture1D<NullContext>> get1DColorTextureBuilder(ShortVertexList data)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture1D<NullContext>> get1DColorTextureBuilder(IntVertexList data)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture1D<NullContext>> get1DColorTextureBuilder(FloatVertexList data)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture1D<NullContext>> get1DColorTextureBuilder(DoubleVertexList data)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture2D<NullContext>> get2DColorTextureBuilder(int width, int height, ByteVertexList data)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture2D<NullContext>> get2DColorTextureBuilder(int width, int height, ShortVertexList data)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture2D<NullContext>> get2DColorTextureBuilder(int width, int height, IntVertexList data)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture2D<NullContext>> get2DColorTextureBuilder(int width, int height, FloatVertexList data)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture2D<NullContext>> get2DColorTextureBuilder(int width, int height, DoubleVertexList data)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture2D<NullContext>> get2DColorTextureBuilder(InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture2D<NullContext>> get2DColorTextureBuilder(int width, int height)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public DepthTextureBuilder<NullContext, ? extends Texture2D<NullContext>> get2DDepthTextureBuilder(int width, int height)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public StencilTextureBuilder<NullContext, ? extends Texture2D<NullContext>> get2DStencilTextureBuilder(int width, int height)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public DepthStencilTextureBuilder<NullContext, ? extends Texture2D<NullContext>> get2DDepthStencilTextureBuilder(int width, int height)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture2D<NullContext>> getPerlinNoiseTextureBuilder()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ColorTextureBuilder<NullContext, ? extends Texture3D<NullContext>> get2DColorTextureArrayBuilder(int width, int height, int length)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public DepthTextureBuilder<NullContext, ? extends Texture3D<NullContext>> get2DDepthTextureArrayBuilder(int width, int height, int length)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public StencilTextureBuilder<NullContext, ? extends Texture3D<NullContext>> get2DStencilTextureArrayBuilder(int width, int height, int length)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public DepthStencilTextureBuilder<NullContext, ? extends Texture3D<NullContext>> get2DDepthStencilTextureArrayBuilder(int width, int height, int length)
	{
		throw new UnsupportedOperationException();
	}
}
