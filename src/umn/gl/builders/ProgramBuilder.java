/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.builders;

import java.io.File;
import java.io.FileNotFoundException;

import umn.gl.core.Context;
import umn.gl.core.Program;
import umn.gl.core.Shader;
import umn.gl.core.ShaderType;

public interface ProgramBuilder<ContextType extends Context<ContextType>>
{
    ProgramBuilder<ContextType> define(String key, Object value);
    ProgramBuilder<ContextType> addShader(Shader<ContextType> shader);
    ProgramBuilder<ContextType> addShader(ShaderType type, File shaderFile);
    ProgramBuilder<ContextType> addShader(ShaderType type, String shaderSource);

    Program<ContextType> createProgram() throws FileNotFoundException;
}
