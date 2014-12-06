package tetzlaff.gl.opengl.helpers;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
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
