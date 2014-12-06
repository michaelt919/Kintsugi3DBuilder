package tetzlaff.gl.opengl;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.openGLErrorCheck;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import tetzlaff.gl.exceptions.ProgramLinkFailureException;
import tetzlaff.gl.exceptions.ShaderCompileFailureException;


public class OpenGLShader implements OpenGLResource
{
	private int shaderId;
	
	public OpenGLShader(int shaderType, File file) throws FileNotFoundException
	{
		Scanner scanner = new Scanner(file);
        scanner.useDelimiter("\\Z"); // EOF
        String source = scanner.next();
        scanner.close();
        this.init(shaderType, source);
	}
	
	public OpenGLShader(int shaderType, String source)
	{
		this.init(shaderType, source);
	}
	
	private void init(int shaderType, String source)
	{
		shaderId = glCreateShader(shaderType);
		openGLErrorCheck();
        glShaderSource(shaderId, source);
		openGLErrorCheck();
        glCompileShader(shaderId);
		openGLErrorCheck();
        int compiled = glGetShaderi(shaderId, GL_COMPILE_STATUS);
		openGLErrorCheck();
        if (compiled == GL_FALSE)
        {
        	throw new ProgramLinkFailureException(glGetShaderInfoLog(shaderId));
        }
	}
	
	int getId()
	{
		return shaderId;
	}
	
	public void delete()
	{
		glDeleteShader(shaderId);
		openGLErrorCheck();
	}
}
