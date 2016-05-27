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
package tetzlaff.gl.builders.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.Context;
import tetzlaff.gl.Shader;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.builders.ProgramBuilder;

public abstract class ProgramBuilderBase<ContextType extends Context<ContextType>> implements ProgramBuilder<ContextType> 
{
	protected final ContextType context;
	private final List<Shader<ContextType>> shaders;
	
	protected ProgramBuilderBase(ContextType context)
	{
		this.context = context;
		this.shaders = new ArrayList<Shader<ContextType>>();
	}
	
	protected Iterable<Shader<ContextType>> getShaders()
	{
		return this.shaders;
	}
	
	@Override
	public ProgramBuilder<ContextType> addShader(Shader<ContextType> shader)
	{
		shaders.add(shader);
		return this;
	}
	
	@Override
	public ProgramBuilder<ContextType> addShader(ShaderType type, File shaderFile) throws FileNotFoundException
	{
		shaders.add(context.createShader(type, shaderFile));
		return this;
	}
	
	@Override
	public ProgramBuilder<ContextType> addShader(ShaderType type, String shaderSource)
	{
		shaders.add(context.createShader(type, shaderSource));
		return this;
	}
}
