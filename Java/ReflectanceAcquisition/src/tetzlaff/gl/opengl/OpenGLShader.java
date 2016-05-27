/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package tetzlaff.gl.opengl;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Scanner;

import tetzlaff.gl.Shader;
import tetzlaff.gl.exceptions.ShaderCompileFailureException;

class OpenGLShader implements Shader<OpenGLContext>
{
	protected final OpenGLContext context;
	
	private int shaderId;
	
	OpenGLShader(OpenGLContext context, int shaderType, File file) throws FileNotFoundException
	{
		this.context = context;
		
		Scanner scanner;
		
		try
		{
			// Try to open an ordinary file first.
			scanner = new Scanner(file);
		}
		catch(FileNotFoundException e)
		{
			InputStream is = getClass().getResourceAsStream('/' + file.getPath().replace(File.separatorChar, '/'));
			if(is == null)
			{
				throw new FileNotFoundException(file + " (no such file or resource)");
			}
			
			scanner = new Scanner(is);
		}
        scanner.useDelimiter("\\Z"); // EOF
        String source = scanner.next();
        scanner.close();
        
        try
        {
        	this.init(shaderType, source);
        }
        catch (ShaderCompileFailureException e)
        {
        	throw new ShaderCompileFailureException(file.getAbsolutePath() + " failed to compile.", e);
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
	public void delete()
	{
		glDeleteShader(shaderId);
		this.context.openGLErrorCheck();
	}
}
