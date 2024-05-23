/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.builder.state.ReadonlySettingsModel;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.core.ProgramObject;
import kintsugi3d.gl.core.ShaderType;

public class SpecularFitProgramFactory<ContextType extends Context<ContextType>>
{
    private final ReadonlySettingsModel ibrSettings;
    private final SpecularBasisSettings specularBasisSettings;

    public SpecularFitProgramFactory(ReadonlySettingsModel ibrSettings, SpecularBasisSettings specularBasisSettings)
    {
        this.ibrSettings = ibrSettings;
        this.specularBasisSettings = specularBasisSettings;
    }

    public ProgramBuilder<ContextType> getShaderProgramBuilder(ReadonlyIBRResources<ContextType> resources, File vertexShader,
        File fragmentShader, boolean visibilityAndShadowTests)
    {
        ProgramBuilder<ContextType> builder = resources.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, vertexShader)
            .addShader(ShaderType.FRAGMENT, fragmentShader);

        // Disable occlusion / shadow culling if requested (may do nothing if already operating in texture space)
        // getIBRShaderProgramBuilder() will enable by default if depth / shadow maps are available
        if (!visibilityAndShadowTests || !ibrSettings.getBoolean("occlusionEnabled"))
        {
            builder.define("VISIBILITY_TEST_ENABLED", false);
        }

        if (!visibilityAndShadowTests || !ibrSettings.getBoolean("shadowsEnabled"))
        {
            builder.define("SHADOW_TEST_ENABLED", false);
        }

        // Common definitions for all specular fitting related shaders.
        return builder
                .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
                .define("SMITH_MASKING_SHADOWING", specularBasisSettings.isSmithMaskingShadowingEnabled())
                .define("BASIS_COUNT", specularBasisSettings.getBasisCount())
                .define("BASIS_RESOLUTION", specularBasisSettings.getBasisResolution());
    }

    public ProgramBuilder<ContextType> getShaderProgramBuilder(ReadonlyIBRResources<ContextType> resources, File vertexShader, File fragmentShader)
    {
        return getShaderProgramBuilder(resources, vertexShader, fragmentShader, true);
    }

    public ProgramObject<ContextType> createProgram(ReadonlyIBRResources<ContextType> resources, File vertexShader, File fragmentShader,
        boolean visibilityAndShadowTests, Map<String, Object> additionalDefines)
        throws IOException
    {
        // Common definitions for all specular fitting related shaders.
        ProgramBuilder<ContextType> programBuilder = getShaderProgramBuilder(resources, vertexShader, fragmentShader, visibilityAndShadowTests);

        // Add additional defines provided to the function.
        for (Entry<String, Object> definition : additionalDefines.entrySet())
        {
            programBuilder.define(definition.getKey(), definition.getValue());
        }

        // Actually create the program.
        ProgramObject<ContextType> program = programBuilder.createProgram();

        // Setup uniforms.
        setupShaderProgram(resources, program);

        return program;
    }

    public ProgramObject<ContextType> createProgram(ReadonlyIBRResources<ContextType> resources, File vertexShader, File fragmentShader, Map<String, Object> additionalDefines)
        throws IOException
    {
        return createProgram(resources, vertexShader, fragmentShader, true, additionalDefines);
    }

    public ProgramObject<ContextType> createProgram(ReadonlyIBRResources<ContextType> resources, File vertexShader, File fragmentShader, boolean visibilityAndShadowTests) throws IOException
    {
        return createProgram(resources, vertexShader, fragmentShader, visibilityAndShadowTests, Collections.emptyMap());
    }

    public ProgramObject<ContextType> createProgram(ReadonlyIBRResources<ContextType> resources, File vertexShader, File fragmentShader) throws IOException
    {
        return createProgram(resources, vertexShader, fragmentShader, true);
    }

    public void setupShaderProgram(ReadonlyIBRResources<ContextType> resources, Program<ContextType> program)
    {
        resources.setupShaderProgram(program);
        program.setUniform("occlusionBias", ibrSettings.getFloat("occlusionBias"));
    }
}
