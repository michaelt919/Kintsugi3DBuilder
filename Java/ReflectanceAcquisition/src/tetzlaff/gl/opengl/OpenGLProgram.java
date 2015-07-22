package tetzlaff.gl.opengl;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.AbstractCollection;
import java.util.ArrayList;

import tetzlaff.gl.Program;
import tetzlaff.gl.Resource;
import tetzlaff.gl.Shader;
import tetzlaff.gl.Texture;
import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.exceptions.ProgramLinkFailureException;
import tetzlaff.gl.exceptions.UnlinkedProgramException;
import tetzlaff.gl.helpers.IntVector2;
import tetzlaff.gl.helpers.IntVector3;
import tetzlaff.gl.helpers.IntVector4;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector2;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.gl.opengl.helpers.ResourceManager;

public class OpenGLProgram implements Resource, Program<OpenGLContext>
{
	private int programId;
	private AbstractCollection<OpenGLShader> ownedShaders;
	private ResourceManager<OpenGLTexture> textureManager;
	private ResourceManager<OpenGLUniformBuffer> uniformBufferManager;
	
	public OpenGLProgram()
	{
		initProgram();
	}
	
	public OpenGLProgram(File vertexShader, File fragmentShader) throws FileNotFoundException
	{
		initProgram();
		OpenGLShader vertexShaderObj = new OpenGLShader(GL_VERTEX_SHADER, vertexShader);
		OpenGLShader fragmentShaderObj = new OpenGLShader(GL_FRAGMENT_SHADER, fragmentShader);
		this.attachShader(vertexShaderObj, true);
		this.attachShader(fragmentShaderObj, true);
		this.link();
	}
	
	private void initProgram()
	{
		programId = glCreateProgram();
		openGLErrorCheck();
		ownedShaders = new ArrayList<OpenGLShader>();
		textureManager = new ResourceManager<OpenGLTexture>(OpenGLTexture2D.MAX_COMBINED_TEXTURE_IMAGE_UNITS);
		uniformBufferManager = new ResourceManager<OpenGLUniformBuffer>(OpenGLUniformBuffer.MAX_COMBINED_UNIFORM_BLOCKS);
	}
	
	@Override
	public void attachShader(Shader<OpenGLContext> shader, boolean owned)
	{
		if (shader instanceof OpenGLShader)
		{
			OpenGLShader shaderCast = (OpenGLShader)shader;
			glAttachShader(programId, shaderCast.getId());
			openGLErrorCheck();
			if (owned)
			{
				ownedShaders.add(shaderCast);
			}
		}
		else
		{
			throw new IllegalArgumentException("'shader' must be of type OpenGLShader.");
		}
	}
	
	@Override
	public void detachShader(Shader<OpenGLContext> shader)
	{
		if (shader instanceof OpenGLShader)
		{
			glDetachShader(programId, ((OpenGLShader)shader).getId());
			openGLErrorCheck();
		}
		else
		{
			throw new IllegalArgumentException("'shader' must be of type OpenGLShader.");
		}
	}
	
	@Override
	public boolean isLinked()
	{
		int linked = glGetProgrami(programId, GL_LINK_STATUS);
		openGLErrorCheck();
    	return linked == GL_TRUE;
	}
	
	@Override
	public void link()
	{
    	glLinkProgram(programId);
		openGLErrorCheck();
    	if (!this.isLinked())
    	{
    		throw new ProgramLinkFailureException(glGetProgramInfoLog(programId));
    	}
	}
	
	private void useForUniformAssignment()
	{
		if (!this.isLinked())
		{
			throw new UnlinkedProgramException("An OpenGL program cannot be used if it has not been linked.");
		}
		else
		{
			glUseProgram(programId);
			openGLErrorCheck();
		}
	}
	
	void use()
	{
		if (!this.isLinked())
		{
			throw new UnlinkedProgramException("An OpenGL program cannot be used if it has not been linked.");
		}
		else
		{
			glUseProgram(programId);
			openGLErrorCheck();
			
			for (int i = 0; i < textureManager.length; i++)
			{
				OpenGLTexture texture = textureManager.getResourceByUnit(i);
				if (texture != null)
				{
					texture.bindToTextureUnit(i);
				}
				else
				{
					OpenGLTexture.unbindTextureUnit(i);
				}
			}
			
			for (int i = 0; i < uniformBufferManager.length; i++)
			{
				OpenGLUniformBuffer uniformBuffer = uniformBufferManager.getResourceByUnit(i);
				if (uniformBuffer != null)
				{
					uniformBuffer.bindToIndex(i);
				}
				else
				{
					OpenGLUniformBuffer.unbindFromIndex(i);
				}
			}
		}
	}
	
	@Override
	public boolean setTexture(int location, Texture<OpenGLContext> texture)
	{
		if (texture instanceof OpenGLTexture)
		{
			int textureUnit = textureManager.assignResourceByKey(location, (OpenGLTexture)texture);
			return this.setUniform(location, textureUnit);
		}
		else
		{
			throw new IllegalArgumentException("'textur' must be of type OpenGLTexture.");
		}
	}
	
	@Override
	public boolean setTexture(String name, Texture<OpenGLContext> texture)
	{
		return this.setTexture(this.getUniformLocation(name), texture);
	}
	
	@Override
	public boolean setUniformBuffer(int index, UniformBuffer<OpenGLContext> buffer)
	{
		if (buffer instanceof OpenGLUniformBuffer)
		{
			if (index >= 0)
			{
				int bindingPoint = uniformBufferManager.assignResourceByKey(index, (OpenGLUniformBuffer)buffer);
				
				glUniformBlockBinding(this.programId, index, bindingPoint);
				openGLErrorCheck();
				
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			throw new IllegalArgumentException("'buffer' must be of type OpenGLUniformBuffer.");
		}
	}
	
	@Override
	public boolean setUniformBuffer(String name, UniformBuffer<OpenGLContext> buffer)
	{
		return this.setUniformBuffer(this.getUniformBlockIndex(name), buffer);
	}
	
	@Override
	public int getUniformBlockIndex(String name)
	{
		int index = glGetUniformBlockIndex(this.programId, name);
		openGLErrorCheck();
		return index;
	}
	
	@Override
	public void delete()
	{
		glDeleteProgram(programId);
		openGLErrorCheck();
		for (OpenGLShader shader : ownedShaders)
		{
			shader.delete();
		}
	}
	
	@Override
	public int getUniformLocation(String name)
	{
		int location = glGetUniformLocation(programId, name);
		openGLErrorCheck();
		return location;
	}
	
	@Override
	public boolean setUniform(int location, int value)
	{
		if (location >= 0)
		{
			this.useForUniformAssignment(); 
			
			glUniform1i(location, value);
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setUniform(int location, IntVector2 value)
	{
		if (location >= 0)
		{
			this.useForUniformAssignment(); 
			
			glUniform2i(location, value.x, value.y);
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setUniform(int location, IntVector3 value)
	{
		if (location >= 0)
		{
			this.useForUniformAssignment(); 
			
			glUniform3i(location, value.x, value.y, value.z);
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setUniform(int location, IntVector4 value)
	{
		if (location >= 0)
		{
			this.useForUniformAssignment(); 
			
			glUniform4i(location, value.x, value.y, value.z, value.w);
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean setUniform(int location, float value)
	{
		if (location >= 0)
		{
			this.useForUniformAssignment(); 
			
			glUniform1f(location, value);
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setUniform(int location, Vector2 value)
	{
		if (location >= 0)
		{
			this.useForUniformAssignment(); 
			
			glUniform2f(location, value.x, value.y);
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setUniform(int location, Vector3 value)
	{
		if (location >= 0)
		{
			this.useForUniformAssignment(); 
			
			glUniform3f(location, value.x, value.y, value.z);
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setUniform(int location, Vector4 value)
	{
		if (location >= 0)
		{
			this.useForUniformAssignment(); 
			
			glUniform4f(location, value.x, value.y, value.z, value.w);
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean setUniform(int location, boolean value)
	{
		return this.setUniform(location, value ? GL_TRUE : GL_FALSE);
	}
	
	@Override
	public boolean setUniform(String name, boolean value)
	{
		return this.setUniform(this.getUniformLocation(name), value ? GL_TRUE : GL_FALSE);
	}
	
	@Override
	public boolean setUniform(String name, int value)
	{
		return this.setUniform(this.getUniformLocation(name), value);
	}
	
	@Override
	public boolean setUniform(String name, IntVector2 value)
	{
		return this.setUniform(this.getUniformLocation(name), value);
	}
	
	@Override
	public boolean setUniform(String name, IntVector3 value)
	{
		return this.setUniform(this.getUniformLocation(name), value);
	}
	
	@Override
	public boolean setUniform(String name, IntVector4 value)
	{
		return this.setUniform(this.getUniformLocation(name), value);
	}

	@Override
	public boolean setUniform(String name, float value)
	{
		return this.setUniform(this.getUniformLocation(name), value);
	}
	
	@Override
	public boolean setUniform(String name, Vector2 value)
	{
		return this.setUniform(this.getUniformLocation(name), value);
	}
	
	@Override
	public boolean setUniform(String name, Vector3 value)
	{
		return this.setUniform(this.getUniformLocation(name), value);
	}
	
	@Override
	public boolean setUniform(String name, Vector4 value)
	{
		return this.setUniform(this.getUniformLocation(name), value);
	}
	
	@Override
	public int getVertexAttribLocation(String name)
	{
		int location = glGetAttribLocation(programId, name);
		openGLErrorCheck();
		return location;
	}

	@Override
	public boolean setUniform(int location, Matrix4 value) 
	{
		if (location >= 0)
		{
			this.useForUniformAssignment(); 
			
			glUniformMatrix4(location, false, value.asFloatBuffer());
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean setUniform(String name, Matrix4 value) 
	{
		return this.setUniform(this.getUniformLocation(name), value);
	}
}
