package tetzlaff.gl.opengl;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL40.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.AbstractCollection;
import java.util.ArrayList;

import tetzlaff.gl.Program;
import tetzlaff.gl.exceptions.ProgramLinkFailureException;
import tetzlaff.gl.exceptions.UnlinkedProgramException;
import tetzlaff.gl.helpers.*;
import tetzlaff.gl.opengl.helpers.ResourceManager;

public class OpenGLProgram implements OpenGLResource, Program<OpenGLShader, OpenGLTexture, OpenGLUniformBuffer>
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
	public void attachShader(OpenGLShader shader, boolean owned)
	{
		glAttachShader(programId, shader.getId());
		openGLErrorCheck();
		if (owned)
		{
			ownedShaders.add(shader);
		}
	}
	
	@Override
	public void detachShader(OpenGLShader shader)
	{
		glDetachShader(programId, shader.getId());
		openGLErrorCheck();
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
	public boolean setTexture(int location, OpenGLTexture texture)
	{
		int textureUnit = textureManager.assignResourceByKey(location, texture);
		return this.setUniform(location, textureUnit);
	}
	
	@Override
	public boolean setTexture(String name, OpenGLTexture texture)
	{
		return this.setTexture(this.getUniformLocation(name), texture);
	}
	
	@Override
	public boolean setUniformBuffer(int index, OpenGLUniformBuffer buffer)
	{
		if (index >= 0)
		{
			int bindingPoint = uniformBufferManager.assignResourceByKey(index, buffer);
			
			glUniformBlockBinding(this.programId, index, bindingPoint);
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setUniformBuffer(String name, OpenGLUniformBuffer buffer)
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
			this.use(); 
			
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
			this.use(); 
			
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
			this.use(); 
			
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
			this.use(); 
			
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
			this.use(); 
			
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
			this.use(); 
			
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
			this.use(); 
			
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
			this.use(); 
			
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
	public boolean setUniform(int location, double value)
	{
		if (location >= 0)
		{
			this.use(); 
			
			glUniform1d(location, value);
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setUniform(int location, DoubleVector2 value)
	{
		if (location >= 0)
		{
			this.use(); 
			
			glUniform2d(location, value.x, value.y);
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setUniform(int location, DoubleVector3 value)
	{
		if (location >= 0)
		{
			this.use(); 
			
			glUniform3d(location, value.x, value.y, value.z);
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean setUniform(int location, DoubleVector4 value)
	{
		if (location >= 0)
		{
			this.use(); 
			
			glUniform4d(location, value.x, value.y, value.z, value.w);
			openGLErrorCheck();
			
			return true;
		}
		else
		{
			return false;
		}
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
	public boolean setUniform(String name, double value)
	{
		return this.setUniform(this.getUniformLocation(name), value);
	}
	
	@Override
	public boolean setUniform(String name, DoubleVector2 value)
	{
		return this.setUniform(this.getUniformLocation(name), value);
	}
	
	@Override
	public boolean setUniform(String name, DoubleVector3 value)
	{
		return this.setUniform(this.getUniformLocation(name), value);
	}
	
	@Override
	public boolean setUniform(String name, DoubleVector4 value)
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
			this.use(); 
			
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
