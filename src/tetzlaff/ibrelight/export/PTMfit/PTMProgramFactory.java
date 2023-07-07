/*
 *  Copyright (c) Zhangchi (Josh) Lyu, Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.PTMfit;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Program;
import tetzlaff.gl.core.ShaderType;
import tetzlaff.ibrelight.rendering.resources.IBRResourcesImageSpace;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Map;

public class PTMProgramFactory <ContextType extends Context<ContextType>>
{
    private final IBRResourcesImageSpace<ContextType> resources;

    public PTMProgramFactory(IBRResourcesImageSpace<ContextType> resources) {
        this.resources = resources;
    }
    public ProgramBuilder<ContextType> getShaderProgramBuilder(File vertexShader, File fragmentShader, boolean visibilityAndShadowTests)
    {
        return resources.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, vertexShader)
                .addShader(ShaderType.FRAGMENT, fragmentShader)
                .define("VISIBILITY_TEST_ENABLED",
                        visibilityAndShadowTests && resources.depthTextures != null)
                .define("SHADOW_TEST_ENABLED",
                        visibilityAndShadowTests && resources.shadowTextures != null)
                .define("BASIS_COUNT", 10);
    }
    public ProgramBuilder<ContextType> getShaderProgramBuilder(File vertexShader, File fragmentShader)
    {
        return getShaderProgramBuilder(vertexShader, fragmentShader, true);
    }
    public Program<ContextType> createProgram(File vertexShader, File fragmentShader, boolean visibilityAndShadowTests, Map<String, Object> additionalDefines)
            throws FileNotFoundException
    {
        // Common definitions for all specular fitting related shaders.
        ProgramBuilder<ContextType> programBuilder = getShaderProgramBuilder(vertexShader, fragmentShader, visibilityAndShadowTests);

        // Add additional defines provided to the function.
        for (Map.Entry<String, Object> definition : additionalDefines.entrySet())
        {
            programBuilder.define(definition.getKey(), definition.getValue());
        }

        // Actually create the program.
        Program<ContextType> program = programBuilder.createProgram();
        return program;
    }
    public Program<ContextType> createProgram(File vertexShader, File fragmentShader, boolean visibilityAndShadowTests) throws FileNotFoundException
    {
        return createProgram(vertexShader, fragmentShader, visibilityAndShadowTests, Collections.emptyMap());
    }

    public Program<ContextType> createProgram(File vertexShader, File fragmentShader) throws FileNotFoundException
    {
        return createProgram(vertexShader, fragmentShader, true);
    }
}
