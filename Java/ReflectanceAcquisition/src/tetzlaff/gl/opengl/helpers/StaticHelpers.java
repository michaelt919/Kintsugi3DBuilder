package tetzlaff.gl.opengl.helpers;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL41.*;
import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.opengl.exceptions.OpenGLException;
import tetzlaff.gl.opengl.exceptions.OpenGLInvalidEnumException;
import tetzlaff.gl.opengl.exceptions.OpenGLInvalidFramebufferOperationException;
import tetzlaff.gl.opengl.exceptions.OpenGLInvalidOperationException;
import tetzlaff.gl.opengl.exceptions.OpenGLInvalidValueException;
import tetzlaff.gl.opengl.exceptions.OpenGLOutOfMemoryException;
import tetzlaff.gl.opengl.exceptions.OpenGLStackOverflowException;
import tetzlaff.gl.opengl.exceptions.OpenGLStackUnderflowException;

public class StaticHelpers 
{
	public static int getOpenGLInternalFormat(ColorFormat format)
	{
		if (format.alphaBits > 0)
		{
			switch(format.dataType)
			{
			case FloatingPoint:
				if (format.redBits <= 16 && format.greenBits <= 16 && format.blueBits <= 16 && format.alphaBits <= 16)
				{
					return GL_RGBA16F;
				}
				else
				{
					return GL_RGBA32F;
				}
			case UnsignedInteger:
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
			case SignedInteger:
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
			case SRGBFixedPoint:
				return GL_SRGB8_ALPHA8;
			case SignedFixedPoint:
				if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8 && format.alphaBits <= 8)
				{
					return GL_RGBA8_SNORM;
				}
				else
				{
					return GL_RGBA16_SNORM;
				}
			case NormalizedFixedPoint:
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
			case FloatingPoint:
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
			case UnsignedInteger:
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
			case SignedInteger:
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
			case SRGBFixedPoint:
				return GL_SRGB8;
			case SignedFixedPoint:
				if (format.redBits <= 8 && format.greenBits <= 8 && format.blueBits <= 8)
				{
					return GL_RGB8_SNORM;
				}
				else
				{
					return GL_RGB16_SNORM;
				}
			case NormalizedFixedPoint:
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
			case FloatingPoint:
				if (format.redBits <= 16 && format.greenBits <= 16)
				{
					return GL_RG16F;
				}
				else
				{
					return GL_RG32F;
				}
			case UnsignedInteger:
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
			case SignedInteger:
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
			case SRGBFixedPoint:
				return GL_SRGB8;
			case SignedFixedPoint:
				if (format.redBits <= 8 && format.greenBits <= 8)
				{
					return GL_RG8_SNORM;
				}
				else
				{
					return GL_RG16_SNORM;
				}
			case NormalizedFixedPoint:
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
			case FloatingPoint:
				if (format.redBits <= 16)
				{
					return GL_R16F;
				}
				else
				{
					return GL_R32F;
				}
			case UnsignedInteger:
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
			case SignedInteger:
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
			case SRGBFixedPoint:
				return GL_SRGB8;
			case SignedFixedPoint:
				if (format.redBits <= 8)
				{
					return GL_R8_SNORM;
				}
				else
				{
					return GL_R16_SNORM;
				}
			case NormalizedFixedPoint:
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
	
	/**
	 * Should always be called after any OpenGL function
	 * Search for missing calls to this using this regex:
	 * gl[A-Z].*\(.*\);\s*[^\s(openGLErrorCheck\(\);)]
	 */
	public static void openGLErrorCheck()
	{
		int error = glGetError();
		switch (error)
		{
		case GL_NO_ERROR: return;
		case GL_INVALID_ENUM: throw new OpenGLInvalidEnumException();
		case GL_INVALID_VALUE: throw new OpenGLInvalidValueException();
		case GL_INVALID_OPERATION: throw new OpenGLInvalidOperationException();
		case GL_INVALID_FRAMEBUFFER_OPERATION: throw new OpenGLInvalidFramebufferOperationException();
		case GL_OUT_OF_MEMORY: throw new OpenGLOutOfMemoryException();
		case GL_STACK_UNDERFLOW: throw new OpenGLStackUnderflowException();
		case GL_STACK_OVERFLOW: throw new OpenGLStackOverflowException();
		default: throw new OpenGLException("Unrecognized OpenGL Exception.");
		}
	}
}
