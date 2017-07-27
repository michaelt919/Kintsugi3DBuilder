package tetzlaff.gl.opengl;

import tetzlaff.gl.Shader;
import tetzlaff.gl.exceptions.ShaderCompileFailureException;
import tetzlaff.gl.exceptions.ShaderPreprocessingFailureException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

class OpenGLShader implements Shader<OpenGLContext>
{
	protected final OpenGLContext context;
	
	private int shaderId;
	
	OpenGLShader(OpenGLContext context, int shaderType, File file) throws FileNotFoundException
	{
		this.context = context;
		
		StringBuilder sb = new StringBuilder();
		
		loadSource(file, sb);
        
        try
        {
        	this.init(shaderType, sb.toString());
        }
        catch (ShaderCompileFailureException e)
        {
        	throw new ShaderCompileFailureException(file.getAbsolutePath() + " failed to compile.", e);
        }
	}
	
	private void loadSource(File file, StringBuilder sb) throws FileNotFoundException
	{
		int lineCounter = 1;
		
		try(Scanner scanner = new Scanner(file))
		{
	//      scanner.useDelimiter("\\Z"); // EOF
	//      String source = scanner.next();
	//      scanner.close();
			while (scanner.hasNextLine())
			{
				String nextLine = scanner.nextLine();
				
				if (nextLine.startsWith("#include"))
				{
					String[] parts = nextLine.split("\\s");
					File includeFile;
					if (parts.length < 2)
					{
						// This won't work of course, but it's a reasonably way to generate an exception in a manner consistent with a non-existent file.
						includeFile = file.getParentFile();
					}
					else
					{
						String filename = parts[1].replaceAll("['\"]", ""); // Remove single or double quotes around filename
						includeFile = new File(file.getParentFile(), filename);
					}
					
					loadSource(includeFile, sb);
				}
				else
				{
					sb.append(nextLine);
					sb.append("\n");
				}
				
				lineCounter++;
			}
			
			// Read remaining characters in case the file is not newline-terminated.
			scanner.useDelimiter("\\Z");
			if (scanner.hasNext())
			{
				sb.append(scanner.next());
			}
		}
		catch(RuntimeException e)
		{
			throw new ShaderPreprocessingFailureException("An exception occurred while processing line " + lineCounter + " of the following shader file: " + file, e);
		}
	}
	
	OpenGLShader(OpenGLContext context, int shaderType, String source)
	{
		this.context = context;
		this.init(shaderType, source);
	}
	
	@Override
	public OpenGLContext getContext()
	{
		return this.context;
	}
	
	private void init(int shaderType, String source)
	{
		shaderId = glCreateShader(shaderType);
		this.context.openGLErrorCheck();
        glShaderSource(shaderId, source);
		this.context.openGLErrorCheck();
        glCompileShader(shaderId);
		this.context.openGLErrorCheck();
        int compiled = glGetShaderi(shaderId, GL_COMPILE_STATUS);
		this.context.openGLErrorCheck();
        if (compiled == GL_FALSE)
        {
        	throw new ShaderCompileFailureException(glGetShaderInfoLog(shaderId));
        }
	}
	
	int getId()
	{
		return shaderId;
	}
	
	@Override
	public void close()
	{
		glDeleteShader(shaderId);
		this.context.openGLErrorCheck();
	}
}
