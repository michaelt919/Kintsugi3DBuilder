package tetzlaff.gl.opengl;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL40.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.AbstractCollection;
import java.util.ArrayList;

import tetzlaff.gl.Program;
import tetzlaff.gl.exceptions.ProgramLinkFailureException;
import tetzlaff.gl.exceptions.UnlinkedProgramException;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.opengl.helpers.TextureManager;

public class OpenGLProgram implements OpenGLResource, Program<OpenGLShader, OpenGLTexture>
{
	private int programId;
	private AbstractCollection<OpenGLShader> ownedShaders;
	private TextureManager<OpenGLTexture> textureManager;
	
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
		textureManager = new TextureManager<OpenGLTexture>(OpenGLTexture2D.MAX_COMBINED_TEXTURE_IMAGE_UNITS);
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
				OpenGLTexture texture = textureManager.getTextureByUnit(i);
				if (texture != null)
				{
					texture.bindToTextureUnit(i);
				}
				else
				{
					OpenGLTexture.unbindTextureUnit(i);
				}
			}
		}
	}
	
	@Override
	public void setTexture(int location, OpenGLTexture texture)
	{
		int textureUnit = textureManager.assignTextureByKey(location, texture);
		this.setUniform(location, textureUnit);
	}
	
	@Override
	public void setTexture(String name, OpenGLTexture texture)
	{
		this.setTexture(this.getUniformLocation(name), texture);
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
	public void setUniform(int location, int value)
	{
		this.use(); glUniform1i(location, value);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(int location, int value1, int value2)
	{
		this.use(); glUniform2i(location, value1, value2);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(int location, int value1, int value2, int value3)
	{
		this.use(); glUniform3i(location, value1, value2, value3);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(int location, int value1, int value2, int value3, int value4)
	{
		this.use(); glUniform4i(location, value1, value2, value3, value4);
		openGLErrorCheck();
	}

	@Override
	public void setUniform(int location, float value)
	{
		this.use(); glUniform1f(location, value);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(int location, float value1, float value2)
	{
		this.use(); glUniform2f(location, value1, value2);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(int location, float value1, float value2, float value3)
	{
		this.use(); glUniform3f(location, value1, value2, value3);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(int location, float value1, float value2, float value3, float value4)
	{
		this.use(); glUniform4f(location, value1, value2, value3, value4);
		openGLErrorCheck();
	}

	@Override
	public void setUniform(int location, double value)
	{
		this.use(); glUniform1d(location, value);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(int location, double value1, double value2)
	{
		this.use(); glUniform2d(location, value1, value2);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(int location, double value1, double value2, double value3)
	{
		this.use(); glUniform3d(location, value1, value2, value3);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(int location, double value1, double value2, double value3, double value4)
	{
		this.use(); glUniform4d(location, value1, value2, value3, value4);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(String name, int value)
	{
		this.use(); glUniform1i(this.getUniformLocation(name), value);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(String name, int value1, int value2)
	{
		this.use(); glUniform2i(this.getUniformLocation(name), value1, value2);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(String name, int value1, int value2, int value3)
	{
		this.use(); glUniform3i(this.getUniformLocation(name), value1, value2, value3);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(String name, int value1, int value2, int value3, int value4)
	{
		this.use(); glUniform4i(this.getUniformLocation(name), value1, value2, value3, value4);
		openGLErrorCheck();
	}

	@Override
	public void setUniform(String name, float value)
	{
		this.use(); glUniform1f(this.getUniformLocation(name), value);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(String name, float value1, float value2)
	{
		this.use(); glUniform2f(this.getUniformLocation(name), value1, value2);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(String name, float value1, float value2, float value3)
	{
		this.use(); glUniform3f(this.getUniformLocation(name), value1, value2, value3);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(String name, float value1, float value2, float value3, float value4)
	{
		this.use(); glUniform4f(this.getUniformLocation(name), value1, value2, value3, value4);
		openGLErrorCheck();
	}

	@Override
	public void setUniform(String name, double value)
	{
		this.use(); glUniform1d(this.getUniformLocation(name), value);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(String name, double value1, double value2)
	{
		this.use(); glUniform2d(this.getUniformLocation(name), value1, value2);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(String name, double value1, double value2, double value3)
	{
		this.use(); glUniform3d(this.getUniformLocation(name), value1, value2, value3);
		openGLErrorCheck();
	}
	
	@Override
	public void setUniform(String name, double value1, double value2, double value3, double value4)
	{
		this.use(); glUniform4d(this.getUniformLocation(name), value1, value2, value3, value4);
		openGLErrorCheck();
	}
	
	@Override
	public int getVertexAttribLocation(String name)
	{
		int location = glGetAttribLocation(programId, name);
		openGLErrorCheck();
		return location;
	}

	@Override
	public void setUniform(String name, Matrix4 value) 
	{
		this.use();
		glUniformMatrix4(this.getUniformLocation(name), false, value.asFloatBuffer());
		openGLErrorCheck();
	}

	@Override
	public void setUniform(int location, Matrix4 value) 
	{
		this.use();
		glUniformMatrix4(location, false, value.asFloatBuffer());
		openGLErrorCheck();
	}
}
