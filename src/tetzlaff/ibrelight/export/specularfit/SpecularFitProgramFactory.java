/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Program;
import tetzlaff.gl.core.ShaderType;
import tetzlaff.ibrelight.rendering.IBRResources;

public class SpecularFitProgramFactory<ContextType extends Context<ContextType>>
{
    // Use height-correlated Smith for masking / shadowing unless trying to replicate Nam et al. 2018.
    private static final boolean SMITH_MASKING_SHADOWING = !SpecularFitRequest.ORIGINAL_NAM_METHOD;

    private final IBRResources<ContextType> resources;
    private final SpecularFitSettings settings;

    public SpecularFitProgramFactory(IBRResources<ContextType> resources, SpecularFitSettings settings)
    {
        this.resources = resources;
        this.settings = settings;
    }

    public Program<ContextType> createProgram(File vertexShader, File fragmentShader, boolean visibilityAndShadowTests, Map<String, Object> additionalDefines)
        throws FileNotFoundException
    {
        // Common definitions for all specular fitting related shaders.
        ProgramBuilder<ContextType> programBuilder = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, vertexShader)
            .addShader(ShaderType.FRAGMENT, fragmentShader)
            .define("VISIBILITY_TEST_ENABLED",
                visibilityAndShadowTests && resources.depthTextures != null && settings.additional.getBoolean("occlusionEnabled"))
            .define("SHADOW_TEST_ENABLED",
                visibilityAndShadowTests && resources.shadowTextures != null && settings.additional.getBoolean("shadowsEnabled"))
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", SMITH_MASKING_SHADOWING)
            .define("BASIS_COUNT", settings.basisCount);

        // Add additional defines provided to the function.
        for (Entry<String, Object> definition : additionalDefines.entrySet())
        {
            programBuilder.define(definition.getKey(), definition.getValue());
        }

        // Actually create the program.
        Program<ContextType> program = programBuilder.createProgram();

        if (visibilityAndShadowTests)
        {
            program.setUniform("occlusionBias", settings.additional.getFloat("occlusionBias"));
        }

        resources.setupShaderProgram(program);

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
