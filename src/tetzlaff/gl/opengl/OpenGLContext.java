package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.EXTTextureCompressionS3TC.*;
import static org.lwjgl.opengl.EXTTextureSRGB.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.GL43.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import tetzlaff.gl.AlphaBlendingFunction;
import tetzlaff.gl.BufferAccessFrequency;
import tetzlaff.gl.BufferAccessType;
import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Cubemap;
import tetzlaff.gl.Drawable;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.IndexBuffer;
import tetzlaff.gl.Program;
import tetzlaff.gl.Shader;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture1D;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.builders.ColorCubemapBuilder;
import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.builders.DepthStencilTextureBuilder;
import tetzlaff.gl.builders.DepthTextureBuilder;
import tetzlaff.gl.builders.FramebufferObjectBuilder;
import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.StencilTextureBuilder;
import tetzlaff.gl.exceptions.GLException;
import tetzlaff.gl.exceptions.GLInvalidEnumException;
import tetzlaff.gl.exceptions.GLInvalidFramebufferOperationException;
import tetzlaff.gl.exceptions.GLInvalidOperationException;
import tetzlaff.gl.exceptions.GLInvalidValueException;
import tetzlaff.gl.exceptions.GLOutOfMemoryException;
import tetzlaff.gl.exceptions.GLStackOverflowException;
import tetzlaff.gl.exceptions.GLStackUnderflowException;
import tetzlaff.gl.glfw.GLFWWindowContextBase;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.opengl.OpenGLFramebufferObject.OpenGLFramebufferObjectBuilder;
import tetzlaff.gl.opengl.OpenGLProgram.OpenGLProgramBuilder;
import tetzlaff.gl.opengl.OpenGLTexture1D.OpenGLTexture1DFromBufferBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.OpenGLTexture2DColorBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.OpenGLTexture2DDepthBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.OpenGLTexture2DDepthStencilBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.OpenGLTexture2DFromBufferBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.OpenGLTexture2DFromFileBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.OpenGLTexture2DFromHDRFileBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.OpenGLTexture2DStencilBuilder;
import tetzlaff.gl.opengl.OpenGLTexture3D.OpenGLTexture3DColorBuilder;
import tetzlaff.gl.opengl.OpenGLTexture3D.OpenGLTexture3DDepthBuilder;
import tetzlaff.gl.opengl.OpenGLTexture3D.OpenGLTexture3DDepthStencilBuilder;
import tetzlaff.gl.opengl.OpenGLTexture3D.OpenGLTexture3DStencilBuilder;

public class OpenGLContext extends GLFWWindowContextBase<OpenGLContext> implements Context<OpenGLContext>
{
	OpenGLContext(long handle)
	{
		super(handle);
	}
	
	@Override
	public void flush()
	{
		glFlush();
		this.openGLErrorCheck();
	}
	
	@Override
	public void finish()
	{
		glFinish();
		this.openGLErrorCheck();
	}
	
	@Override
	public void enableDepthTest()
	{
		glEnable(GL_DEPTH_TEST);
		this.openGLErrorCheck();
	}
	
	@Override
	public void disableDepthTest()
	{
		glDisable(GL_DEPTH_TEST);
		this.openGLErrorCheck();
	}
	
	@Override
	public void enableMultisampling()
	{
		glEnable(GL_MULTISAMPLE);
		this.openGLErrorCheck();
	}
	
	@Override
	public void disableMultisampling()
	{
		glDisable(GL_MULTISAMPLE);
		this.openGLErrorCheck();
	}
	
	@Override
	public void enableBackFaceCulling()
	{
		glEnable(GL_CULL_FACE);
		this.openGLErrorCheck();
	}
	
	@Override
	public void disableBackFaceCulling()
	{
		glDisable(GL_CULL_FACE);
		this.openGLErrorCheck();
	}
	
	private int blendFuncEnumToInt(AlphaBlendingFunction.Weight func)
	{
		switch(func)
		{
		case DST_ALPHA: return GL_DST_ALPHA;
		case DST_COLOR: return GL_DST_COLOR;
		case ONE: return GL_ONE;
		case ONE_MINUS_DST_ALPHA: return GL_ONE_MINUS_DST_ALPHA;
		case ONE_MINUS_DST_COLOR: return GL_ONE_MINUS_DST_COLOR;
		case ONE_MINUS_SRC_ALPHA: return GL_ONE_MINUS_SRC_ALPHA;
		case ONE_MINUS_SRC_COLOR: return GL_ONE_MINUS_SRC_COLOR;
		case SRC_ALPHA: return GL_ONE_MINUS_SRC_ALPHA;
		case SRC_COLOR: return GL_ONE_MINUS_SRC_COLOR;
		case ZERO: return GL_ZERO;
		default: throw new IllegalArgumentException();
		}
	}
	
	@Override
	public void setAlphaBlendingFunction(AlphaBlendingFunction func)
	{
		glEnable(GL_BLEND);
		this.openGLErrorCheck();
		glBlendFunc(blendFuncEnumToInt(func.sourceWeightFunction), blendFuncEnumToInt(func.destinationWeightFunction));
		this.openGLErrorCheck();
	}
	
	@Override
	public void disableAlphaBlending()
	{
		glDisable(GL_BLEND);
		this.openGLErrorCheck();
	}
	
	private int getInteger(int queryId)
	{
		int queryResult = glGetInteger(queryId);
		this.openGLErrorCheck();
		return queryResult;
	}
	
	@Override
	public int getMaxCombinedVertexUniformComponents()
	{
		return getInteger(GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS);
	}
	
	@Override
	public int getMaxCombinedFragmentUniformComponents()
	{
		return getInteger(GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS);
	}
	
	@Override
	public int getMaxUniformBlockSize()
	{
		return getInteger(GL_MAX_UNIFORM_BLOCK_SIZE);
	}
	
	@Override
	public int getMaxVertexUniformComponents()
	{
		return getInteger(GL_MAX_VERTEX_UNIFORM_COMPONENTS);
	}
	
	@Override
	public int getMaxFragmentUniformComponents()
	{
		return getInteger(GL_MAX_FRAGMENT_UNIFORM_COMPONENTS);
	}
	
	@Override
	public int getMaxArrayTextureLayers()
	{
		return getInteger(GL_MAX_ARRAY_TEXTURE_LAYERS);
	}
	
	@Override
	public int getMaxCombinedTextureImageUnits()
	{
		return getInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
	}
	
	@Override
	public int getMaxCombinedUniformBlocks()
	{
		return getInteger(GL_MAX_COMBINED_UNIFORM_BLOCKS);
	}
	
	@Override
	public Shader<OpenGLContext> createShader(ShaderType type, String source) 
	{
		return new OpenGLShader(this, getOpenGLShaderType(type), source);
	}

	@Override
	public Shader<OpenGLContext> createShader(ShaderType type, File file) throws FileNotFoundException
	{
		return new OpenGLShader(this, getOpenGLShaderType(type), file);
	}

	@Override
	public ProgramBuilder<OpenGLContext> getShaderProgramBuilder() 
	{
		return new OpenGLProgramBuilder(this);
	}

	@Override
	public Framebuffer<OpenGLContext> getDefaultFramebuffer() 
	{
		return new OpenGLDefaultFramebuffer(this);
	}

	@Override
	public VertexBuffer<OpenGLContext> createVertexBuffer() 
	{
		return new OpenGLVertexBuffer(this);
	}

	@Override
	public IndexBuffer<OpenGLContext> createIndexBuffer() 
	{
		return new OpenGLIndexBuffer(this);
	}

	@Override
	public UniformBuffer<OpenGLContext> createUniformBuffer() 
	{
		return new OpenGLUniformBuffer(this);
	}
	
	@Override
	public Drawable<OpenGLContext> createDrawable(Program<OpenGLContext> program)
	{
		if (program instanceof OpenGLProgram)
		{
			return new OpenGLDrawable(this, (OpenGLProgram)program);
		}
		else
		{
			throw new IllegalArgumentException("'program' must be of type OpenGLProgram.");
		}
	}
	
	@Override
	public FramebufferObjectBuilder<OpenGLContext> getFramebufferObjectBuilder(int width, int height)
	{
		return new OpenGLFramebufferObjectBuilder(this, width, height);
	}
	
	int getPixelDataFormatFromDimensions(int dimensions)
	{
		switch(dimensions)
		{
		case 1: return GL_RED;
		case 2: return GL_RG;
		case 3: return GL_RGB;
		case 4: return GL_RGBA;
		default: throw new IllegalArgumentException("Data must be a vertex list of no more than 4 dimensions.");
		}
	}
	
	int getDataTypeConstant(NativeDataType dataType)
	{
		switch(dataType)
		{
		case UNSIGNED_BYTE: return GL_UNSIGNED_BYTE;
		case BYTE: return GL_BYTE;
		case UNSIGNED_SHORT: return GL_UNSIGNED_SHORT;
		case SHORT: return GL_SHORT;
		case UNSIGNED_INT: return GL_UNSIGNED_INT;
		case INT: return GL_INT;
		case FLOAT: return GL_FLOAT;
		case DOUBLE: return GL_DOUBLE;
		default: throw new IllegalArgumentException("Unrecognized data type."); // Shouldn't ever happen
		}
	}
	
	@Override
	public ColorTextureBuilder<OpenGLContext, ? extends Texture1D<OpenGLContext>> get1DColorTextureBuilder(NativeVectorBuffer data)
	{
		return new OpenGLTexture1DFromBufferBuilder(this, GL_TEXTURE_1D, data.getCount(), getPixelDataFormatFromDimensions(data.getDimensions()), getDataTypeConstant(data.getDataType()), data.getBuffer());
	}
	
	@Override
	public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> get2DColorTextureBuilder(int width, int height, NativeVectorBuffer data)
	{
		return new OpenGLTexture2DFromBufferBuilder(this, GL_TEXTURE_2D, width, height, getPixelDataFormatFromDimensions(data.getDimensions()), getDataTypeConstant(data.getDataType()), data.getBuffer());
	}
	
	@Override
	public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> get2DColorTextureBuilder(InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
	{
		return new OpenGLTexture2DFromFileBuilder(this, GL_TEXTURE_2D, imageStream, maskStream, flipVertical);
	}
	
	@Override
	public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> get2DColorTextureFromHDRBuilder(BufferedInputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
	{
		return new OpenGLTexture2DFromHDRFileBuilder(this, GL_TEXTURE_2D, imageStream, maskStream, flipVertical);
	}
	
	@Override
	public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> get2DColorTextureBuilder(int width, int height)
	{
		return new OpenGLTexture2DColorBuilder(this, GL_TEXTURE_2D, width, height);
	}
	
	@Override
	public DepthTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> get2DDepthTextureBuilder(int width, int height)
	{
		return new OpenGLTexture2DDepthBuilder(this, GL_TEXTURE_2D, width, height);
	}
	
	@Override
	public StencilTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> get2DStencilTextureBuilder(int width, int height)
	{
		return new OpenGLTexture2DStencilBuilder(this, GL_TEXTURE_2D, width, height);
	}
	
	@Override
	public DepthStencilTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> get2DDepthStencilTextureBuilder(int width, int height)
	{
		return new OpenGLTexture2DDepthStencilBuilder(this, GL_TEXTURE_2D, width, height);
	}
	
	// Supporting code:
	// Author: Stefan Gustavson (stegu@itn.liu.se) 2004
	// You may use, modify and redistribute this code free of charge,
	// provided that my name and this notice appears intact.
	// https://github.com/ashima/webgl-noise
	// Modified by Michael Tetzlaff
	
	private static final int[] perm = {151,160,137,91,90,15,
			  131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
			  190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
			  88,237,149,56,87,174,20,125,136,171,168, 68,175,74,165,71,134,139,48,27,166,
			  77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
			  102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,
			  135,130,116,188,159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,
			  5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
			  223,183,170,213,119,248,152, 2,44,154,163, 70,221,153,101,155,167, 43,172,9,
			  129,22,39,253, 19,98,108,110,79,113,224,232,178,185, 112,104,218,246,97,228,
			  251,34,242,193,238,210,144,12,191,179,162,241, 81,51,145,235,249,14,239,107,
			  49,192,214, 31,181,199,106,157,184, 84,204,176,115,121,50,45,127, 4,150,254,
			  138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180};
	
	private static final int[][] grad3 = {{0,1,1},{0,1,-1},{0,-1,1},{0,-1,-1},
                   {1,0,1},{1,0,-1},{-1,0,1},{-1,0,-1},
                   {1,1,0},{1,-1,0},{-1,1,0},{-1,-1,0}, // 12 cube edges
                   {1,0,-1},{-1,0,-1},{0,-1,1},{0,1,1}}; // 4 more to make 16
	
	@Override
	public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> getPerlinNoiseTextureBuilder()
	{
		ByteBuffer pixels = ByteBuffer.allocateDirect(256*256*4);
		for(int i = 0; i<256; i++)
		{
		    for(int j = 0; j<256; j++) 
		    {
		    	int offset = (i*256+j)*4;
		    	byte value = (byte)perm[(j+perm[i]) & 0xFF];
		    	pixels.put(offset, (byte)(grad3[value & 0x0F][0] * 64 + 64));   // Gradient x
		    	pixels.put(offset+1, (byte)(grad3[value & 0x0F][1] * 64 + 64)); // Gradient y
	    		pixels.put(offset+2, (byte)(grad3[value & 0x0F][2] * 64 + 64)); // Gradient z
	    		pixels.put(offset+3, value);                     // Permuted index
		    }
		}
		return new OpenGLTexture2DFromBufferBuilder(this, GL_TEXTURE_2D, 256, 256, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
	}

	// End of supporting code

	
	@Override
	public ColorTextureBuilder<OpenGLContext, ? extends Texture3D<OpenGLContext>> get2DColorTextureArrayBuilder(int width, int height, int length)
	{
		return new OpenGLTexture3DColorBuilder(this, GL_TEXTURE_2D_ARRAY, width, height, length);
	}
	
	@Override
	public DepthTextureBuilder<OpenGLContext, ? extends Texture3D<OpenGLContext>> get2DDepthTextureArrayBuilder(int width, int height, int length)
	{
		return new OpenGLTexture3DDepthBuilder(this, GL_TEXTURE_2D_ARRAY, width, height, length);
	}
	
	@Override
	public StencilTextureBuilder<OpenGLContext, ? extends Texture3D<OpenGLContext>> get2DStencilTextureArrayBuilder(int width, int height, int length)
	{
		return new OpenGLTexture3DStencilBuilder(this, GL_TEXTURE_2D_ARRAY, width, height, length);
	}
	
	@Override
	public DepthStencilTextureBuilder<OpenGLContext, ? extends Texture3D<OpenGLContext>> get2DDepthStencilTextureArrayBuilder(int width, int height, int length)
	{
		return new OpenGLTexture3DDepthStencilBuilder(this, GL_TEXTURE_2D_ARRAY, width, height, length);
	}
	
	@Override
	public ColorCubemapBuilder<OpenGLContext, ? extends Cubemap<OpenGLContext>> getColorCubemapBuilder(int faceSize) throws IOException
	{
		return new OpenGLCubemap.ColorBuilder(this, GL_TEXTURE_CUBE_MAP, faceSize);
	}
	
	public DepthTextureBuilder<OpenGLContext, ? extends Cubemap<OpenGLContext>> getDepthCubemapBuilder(int faceSize) throws IOException
	{
		return new OpenGLCubemap.DepthBuilder(this, GL_TEXTURE_CUBE_MAP, faceSize);
	}
	
	public StencilTextureBuilder<OpenGLContext, ? extends Cubemap<OpenGLContext>> getStencilCubemapBuilder(int faceSize) throws IOException
	{
		return new OpenGLCubemap.StencilBuilder(this, GL_TEXTURE_CUBE_MAP, faceSize);
	}
	
	public DepthStencilTextureBuilder<OpenGLContext, ? extends Cubemap<OpenGLContext>> getDepthStencilCubemapBuilder(int faceSize) throws IOException
	{
		return new OpenGLCubemap.DepthStencilBuilder(this, GL_TEXTURE_CUBE_MAP, faceSize);
	}
	
	protected void unbindBuffer(int bufferTarget, int index)
	{
		glBindBufferBase(bufferTarget, index, 0);
		this.openGLErrorCheck();
	}
	
	protected void unbindTextureUnit(int textureUnitIndex)
	{
		if (textureUnitIndex < 0)
		{
			throw new IllegalArgumentException("Texture unit index cannot be negative.");
		}
		else if (textureUnitIndex > this.getMaxCombinedTextureImageUnits())
		{
			throw new IllegalArgumentException("Texture unit index (" + textureUnitIndex + ") is greater than the maximum allowed index (" + 
					(this.getMaxCombinedTextureImageUnits()-1) + ").");
		}
		glActiveTexture(GL_TEXTURE0 + textureUnitIndex);
		this.openGLErrorCheck();
		glBindTexture(GL_TEXTURE_1D, 0);
		this.openGLErrorCheck();
		glBindTexture(GL_TEXTURE_2D, 0);
		this.openGLErrorCheck();
		glBindTexture(GL_TEXTURE_3D, 0);
		this.openGLErrorCheck();
		glBindTexture(GL_TEXTURE_1D_ARRAY, 0);
		this.openGLErrorCheck();
		glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
		this.openGLErrorCheck();
		glBindTexture(GL_TEXTURE_RECTANGLE, 0);
		this.openGLErrorCheck();
		glBindTexture(GL_TEXTURE_BUFFER, 0);
		this.openGLErrorCheck();
		glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
		this.openGLErrorCheck();
		glBindTexture(GL_TEXTURE_CUBE_MAP_ARRAY, 0);
		this.openGLErrorCheck();
		glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0);
		this.openGLErrorCheck();
		glBindTexture(GL_TEXTURE_2D_MULTISAMPLE_ARRAY, 0);
		this.openGLErrorCheck();
	}
	
	protected int getOpenGLInternalColorFormat(ColorFormat format)
	{
		if (format.alphaBits > 0)
		{
			switch(format.dataType)
			{
			case FLOATING_POINT:
				if (format.redBits <= 16 && format.greenBits <= 16 && format.blueBits <= 16 && format.alphaBits <= 16)
				{
					return GL_RGBA16F;
				}
				else
				{
					return GL_RGBA32F;
				}
			case UNSIGNED_INTEGER:
				if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8 && format.alphaBits <= 8)
				{
					return GL_RGBA8UI;
				}
				else if (format.redBits <= 10 && format.greenBits <= 10 && format.blueBits <= 10 && format.alphaBits <= 2)
				{
					return GL_RGB10_A2UI;
				}
				else if (format.redBits <= 16 && format.greenBits <= 16 && format.blueBits <= 16 && format.alphaBits <= 16)
				{
					return GL_RGBA16UI;
				}
				else
				{
					return GL_RGBA32UI;
				}
			case SIGNED_INTEGER:
				if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8 && format.alphaBits <= 8)
				{
					return GL_RGBA8I;
				}
				else if (format.redBits <= 16 && format.greenBits <= 16 && format.blueBits <= 16 && format.alphaBits <= 16)
				{
					return GL_RGBA16I;
				}
				else
				{
					return GL_RGBA32I;
				}
			case SRGB_FIXED_POINT:
				return GL_SRGB8_ALPHA8;
			case SIGNED_FIXED_POINT:
				if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8 && format.alphaBits <= 8)
				{
					return GL_RGBA8_SNORM;
				}
				else
				{
					return GL_RGBA16_SNORM;
				}
			case NORMALIZED_FIXED_POINT:
			default:
				if (format.redBits <= 2 && format.greenBits <= 2 && format.blueBits <= 2 && format.alphaBits <= 2)
				{
					return GL_RGBA2;
				}
				else if (format.redBits <= 4 && format.greenBits <= 4 && format.blueBits <= 4 && format.alphaBits <= 4)
				{
					return GL_RGBA4;
				}
				else if (format.redBits <= 5 && format.greenBits <= 5 && format.blueBits <= 5 && format.alphaBits <= 1)
				{
					return GL_RGB5_A1;
				}
				else if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8 && format.alphaBits <= 8)
				{
					return GL_RGBA8;
				}
				else if (format.redBits <= 10 || format.greenBits <= 10 || format.blueBits <= 10 || format.alphaBits <= 2)
				{
					return GL_RGB10_A2;
				}
				else if (format.redBits <= 12 || format.greenBits <= 12 || format.blueBits <= 12 || format.alphaBits <= 12)
				{
					return GL_RGBA12;
				}
				else
				{
					return GL_RGBA16;
				}
			}
		}
		else if (format.blueBits > 0)
		{
			switch(format.dataType)
			{
			case FLOATING_POINT:
				if (format.redBits <= 11 && format.greenBits <= 11 && format.blueBits <= 10)
				{
					return GL_R11F_G11F_B10F;
				}
				else if (format.redBits <= 16 && format.greenBits <= 16 && format.blueBits <= 16)
				{
					return GL_RGB16F;
				}
				else
				{
					return GL_RGB32F;
				}
			case UNSIGNED_INTEGER:
				if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8)
				{
					return GL_RGB8UI;
				}
				else if (format.redBits <= 16 && format.greenBits <= 16 && format.blueBits <= 16)
				{
					return GL_RGB16UI;
				}
				else
				{
					return GL_RGB32UI;
				}
			case SIGNED_INTEGER:
				if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8)
				{
					return GL_RGB8I;
				}
				else if (format.redBits <= 16 && format.greenBits <= 16 && format.blueBits <= 16)
				{
					return GL_RGB16I;
				}
				else
				{
					return GL_RGB32I;
				}
			case SRGB_FIXED_POINT:
				return GL_SRGB8;
			case SIGNED_FIXED_POINT:
				if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8)
				{
					return GL_RGB8_SNORM;
				}
				else
				{
					return GL_RGB16_SNORM;
				}
			case NORMALIZED_FIXED_POINT:
			default:
				if (format.redBits <= 3 && format.greenBits <= 3 && format.blueBits <= 2)
				{
					return GL_R3_G3_B2;
				}
				else if (format.redBits <= 4 && format.greenBits <= 4 && format.blueBits <= 4)
				{
					return GL_RGB4;
				}
				else if (format.redBits <= 5 && format.greenBits <= 5 && format.blueBits <= 5)
				{
					return GL_RGB5;
				}
				else if (format.redBits <= 5 && format.greenBits <= 6 && format.blueBits <= 5)
				{
					return GL_RGB565;
				}
				else if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8)
				{
					return GL_RGB8;
				}
				else if (format.redBits <= 10 || format.greenBits <= 10 || format.blueBits <= 10)
				{
					return GL_RGB10;
				}
				else if (format.redBits <= 12 || format.greenBits <= 12 || format.blueBits <= 12)
				{
					return GL_RGB12;
				}
				else
				{
					return GL_RGB16;
				}
			}
		}
		else if (format.greenBits > 0)
		{
			switch(format.dataType)
			{
			case FLOATING_POINT:
				if (format.redBits <= 16 && format.greenBits <= 16)
				{
					return GL_RG16F;
				}
				else
				{
					return GL_RG32F;
				}
			case UNSIGNED_INTEGER:
				if (format.redBits <= 8 && format.greenBits <= 8)
				{
					return GL_RG8UI;
				}
				else if (format.redBits <= 16 && format.greenBits <= 16)
				{
					return GL_RG16UI;
				}
				else
				{
					return GL_RG32UI;
				}
			case SIGNED_INTEGER:
				if (format.redBits <= 8 && format.greenBits <= 8)
				{
					return GL_RG8I;
				}
				else if (format.redBits <= 16 && format.greenBits <= 16)
				{
					return GL_RG16I;
				}
				else
				{
					return GL_RG32I;
				}
			case SRGB_FIXED_POINT:
				return GL_SRGB8;
			case SIGNED_FIXED_POINT:
				if (format.redBits <= 8 && format.greenBits <= 8)
				{
					return GL_RG8_SNORM;
				}
				else
				{
					return GL_RG16_SNORM;
				}
			case NORMALIZED_FIXED_POINT:
			default:
				if (format.redBits <= 8 && format.greenBits <= 8)
				{
					return GL_RG8;
				}
				else
				{
					return GL_RG16;
				}
			}
		}
		else
		{
			switch(format.dataType)
			{
			case FLOATING_POINT:
				if (format.redBits <= 16)
				{
					return GL_R16F;
				}
				else
				{
					return GL_R32F;
				}
			case UNSIGNED_INTEGER:
				if (format.redBits <= 8)
				{
					return GL_R8UI;
				}
				else if (format.redBits <= 16)
				{
					return GL_R16UI;
				}
				else
				{
					return GL_R32UI;
				}
			case SIGNED_INTEGER:
				if (format.redBits <= 8)
				{
					return GL_R8I;
				}
				else if (format.redBits <= 16)
				{
					return GL_R16I;
				}
				else
				{
					return GL_R32I;
				}
			case SRGB_FIXED_POINT:
				return GL_SRGB8;
			case SIGNED_FIXED_POINT:
				if (format.redBits <= 8)
				{
					return GL_R8_SNORM;
				}
				else
				{
					return GL_R16_SNORM;
				}
			case NORMALIZED_FIXED_POINT:
			default:
				if (format.redBits <= 8)
				{
					return GL_R8;
				}
				else
				{
					return GL_R16;
				}
			}
		}
	}
	
	
	
	protected int getOpenGLCompressionFormat(CompressionFormat format)
	{
		switch(format)
		{
		case RED_4BPP: return GL_COMPRESSED_RED_RGTC1;
		case SIGNED_RED_4BPP: return GL_COMPRESSED_SIGNED_RED_RGTC1;
		case RED_4BPP_GREEN_4BPP: return GL_COMPRESSED_RG_RGTC2;
		case SIGNED_RED_4BPP_GREEN_4BPP: return GL_COMPRESSED_SIGNED_RG_RGTC2;
		case RGB_4BPP: return GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
		case SRGB_4BPP: return GL_COMPRESSED_SRGB_S3TC_DXT1_EXT;
		case RGB_PUNCHTHROUGH_ALPHA1_4BPP: return GL_COMPRESSED_RGBA_S3TC_DXT1_EXT; 
		case SRGB_PUNCHTHROUGH_ALPHA1_4BPP: return GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT; 
		case RGB_4BPP_ALPHA_4BPP: return GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
		case SRGB_4BPP_ALPHA_4BPP: return GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT;
		default: throw new IllegalArgumentException("Unsupported compression format.");
		}
	}
	
	protected int getOpenGLInternalDepthFormat(int precision)
	{
		if (precision <= 16)
		{
			return GL_DEPTH_COMPONENT16;
		}
		else if (precision <= 24)
		{
			return GL_DEPTH_COMPONENT24;
		}
		else
		{
			return GL_DEPTH_COMPONENT32;
		}
	}
	
	protected int getOpenGLInternalStencilFormat(int precision)
	{
		if (precision == 1)
		{
			return GL_STENCIL_INDEX1;
		}
		if (precision <= 4)
		{
			return GL_STENCIL_INDEX4;
		}
		else if (precision <= 8)
		{
			return GL_STENCIL_INDEX8;
		}
		else
		{
			return GL_STENCIL_INDEX16;
		}
	}
	
	protected int getOpenGLShaderType(ShaderType type)
	{
		switch(type)
		{
		case VERTEX: return GL_VERTEX_SHADER;
		case FRAGMENT: return GL_FRAGMENT_SHADER;
		case GEOMETRY: return GL_GEOMETRY_SHADER;
		case TESSELATION_CONTROL: return GL_TESS_CONTROL_SHADER;
		case TESSELATION_EVALUATION: return GL_TESS_EVALUATION_SHADER;
		case COMPUTE: return GL_COMPUTE_SHADER;
		default: return 0;
		}
	}
	
	protected int getOpenGLBufferUsage(BufferAccessType accessType, BufferAccessFrequency accessFreq)
	{
		switch(accessFreq)
		{
		case STREAM:
			switch(accessType)
			{
			case DRAW: return GL_STREAM_DRAW;
			case READ: return GL_STREAM_READ;
			case COPY: return GL_STREAM_COPY;
			}
		case STATIC:
			switch(accessType)
			{
			case DRAW: return GL_STATIC_DRAW;
			case READ: return GL_STATIC_READ;
			case COPY: return GL_STATIC_COPY;
			}
		case DYNAMIC:
			switch(accessType)
			{
			case DRAW: return GL_DYNAMIC_DRAW;
			case READ: return GL_DYNAMIC_READ;
			case COPY: return GL_DYNAMIC_COPY;
			}
		}
		
		return 0;
	}
	
	protected String getFramebufferStatusString(String framebufferName, int statusID)
	{
		switch(statusID)
		{
		case GL_FRAMEBUFFER_COMPLETE: return framebufferName + " is framebuffer complete (no errors).";
		case GL_FRAMEBUFFER_UNDEFINED: return framebufferName + " is the default framebuffer, but the default framebuffer does not exist.";
		case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT: return framebufferName + " has attachment points that are framebuffer incomplete.";
		case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: return framebufferName + " does not have at least one image attached to it.";
		case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
		case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER: return framebufferName + " has a color attachment point without an attached image.";
		case GL_FRAMEBUFFER_UNSUPPORTED: return framebufferName + " has attached images with a combination of internal formats that violates an implementation-dependent set of restrictions.";
		case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE: return framebufferName + " has attachments with different multisample parameters.";
		case GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS: return framebufferName + " has a layered attachment and a populated non-layered attachment, or all populated color attachments are not from textures of the same target.";
		case 0: return framebufferName + " has an unknown completeness status because an error has occurred.";
		default: return framebufferName + " has failed an unrecognized completeness check.";
		}
	}
	
	void throwInvalidFramebufferOperationException()
	{
		int readStatus = glCheckFramebufferStatus(GL_READ_FRAMEBUFFER);
		int drawStatus = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER);
		throw new GLInvalidFramebufferOperationException("The framebuffer object is not complete:  " + 
					getFramebufferStatusString("The read framebuffer", readStatus) + "  " + getFramebufferStatusString("The draw framebuffer", drawStatus));
	}
	
	/**
	 * Should always be called after any OpenGL function
	 * Search for missing calls to this using this regex:
	 * gl[A-Z].*\(.*\);\s*[^\s(this.openGLErrorCheck\(\);)]
	 */
	protected void openGLErrorCheck()
	{
		int error = glGetError();
		switch (error)
		{
		case GL_NO_ERROR: return;
		case GL_INVALID_ENUM: throw new GLInvalidEnumException();
		case GL_INVALID_VALUE: throw new GLInvalidValueException();
		case GL_INVALID_OPERATION: throw new GLInvalidOperationException();
		case GL_INVALID_FRAMEBUFFER_OPERATION: throwInvalidFramebufferOperationException();
		case GL_OUT_OF_MEMORY: throw new GLOutOfMemoryException();
		case GL_STACK_UNDERFLOW: throw new GLStackUnderflowException();
		case GL_STACK_OVERFLOW: throw new GLStackOverflowException();
		default: throw new GLException("Unrecognized OpenGL Exception.");
		}
	}
}
