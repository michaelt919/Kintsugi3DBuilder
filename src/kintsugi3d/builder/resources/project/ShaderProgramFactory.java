/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.resources.project;

import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

public interface ShaderProgramFactory<ContextType extends Context<ContextType>> extends ContextBound<ContextType>
{
    /**
     * Gets a shader program builder with any required preprocessor defines automatically injected based on the
     * characteristics of this factory.
     *
     * @return A program builder with preprocessor defines specified, ready to have the vertex and fragment shaders
     * added as well as any additional application-specific preprocessor definitions.
     */
    ProgramBuilder<ContextType> getShaderProgramBuilder();

    /**
     * Sets up a shader program to use this instance's graphics resources.
     * While the geometry is generally associated with a Drawable using the createDrawable function,
     * this method binds all of the textures and associated data like camera poses, light positions, etc.
     * to the shader program's uniform variables.
     *
     * @param program The shader program to set up using this instance's resources.
     */
    void setupShaderProgram(Program<ContextType> program);

    default ProgramObject<ContextType> createProgram(File vertexShader, File fragmentShader,
                                                     Map<String, Object> additionalDefines)
        throws IOException
    {
        // Common definitions for all specular fitting related shaders.
        ProgramBuilder<ContextType> programBuilder = getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, vertexShader)
            .addShader(ShaderType.FRAGMENT, fragmentShader);;

        // Add additional defines provided to the function.
        for (Entry<String, Object> definition : additionalDefines.entrySet())
        {
            programBuilder.define(definition.getKey(), definition.getValue());
        }

        // Actually create the program.
        ProgramObject<ContextType> program = programBuilder.createProgram();

        // Setup uniforms.
        setupShaderProgram(program);

        return program;
    }

    default ProgramObject<ContextType> createProgram(File vertexShader, File fragmentShader)
        throws IOException
    {
        return createProgram(vertexShader, fragmentShader, Collections.emptyMap());
    }
}
