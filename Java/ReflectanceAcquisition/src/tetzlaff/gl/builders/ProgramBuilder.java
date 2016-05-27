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
package tetzlaff.gl.builders;

import java.io.File;
import java.io.FileNotFoundException;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.Shader;
import tetzlaff.gl.ShaderType;

/**
 * Implements the builder design pattern for creating shader programs.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the program will be associated with.
 */
public interface ProgramBuilder<ContextType extends Context<ContextType>>
{
	/**
	 * Adds a shader to the program.
	 * @param shader The shader to add.
	 * @return The calling builder object.
	 */
	ProgramBuilder<ContextType> addShader(Shader<ContextType> shader);
	
	/**
	 * Creates and adds a new shader to the program.
	 * @param type The type of shader to add.
	 * @param shaderFile A file containing the source code for the shader.
	 * @return The calling builder object.
	 * @throws FileNotFoundException Upon a File I/O problem when reading the shader file.
	 */
	ProgramBuilder<ContextType> addShader(ShaderType type, File shaderFile) throws FileNotFoundException;
	
	/**
	 * Creates and adds a new shader to the program.
	 * @param type The type of shader to add.
	 * @param shaderSource A string containing the source code for the shader.
	 * @return The calling builder object.
	 */
	ProgramBuilder<ContextType> addShader(ShaderType type, String shaderSource);
	
	/**
	 * Creates the shader program.
	 * @return The newly created shader program.
	 */
	Program<ContextType> createProgram();
}
