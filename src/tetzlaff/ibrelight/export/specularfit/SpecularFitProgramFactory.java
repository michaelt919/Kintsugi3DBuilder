/*
 *  Copyright (c) Michael Tetzlaff 2022
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
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Program;
import tetzlaff.gl.core.ShaderType;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.rendering.resources.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

public class SpecularFitProgramFactory<ContextType extends Context<ContextType>>
{
    private final ReadonlySettingsModel ibrSettings;
    private final SpecularBasisSettings specularBasisSettings;

    public SpecularFitProgramFactory(ReadonlySettingsModel ibrSettings, SpecularBasisSettings specularBasisSettings)
    {
        this.ibrSettings = ibrSettings;
        this.specularBasisSettings = specularBasisSettings;
    }

    public ProgramBuilder<ContextType> getShaderProgramBuilder(IBRResources<ContextType> resources, File vertexShader,
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
                .define("MICROFACET_DISTRIBUTION_RESOLUTION", specularBasisSettings.getMicrofacetDistributionResolution());
    }

    public ProgramBuilder<ContextType> getShaderProgramBuilder(IBRResources<ContextType> resources, File vertexShader, File fragmentShader)
    {
        return getShaderProgramBuilder(resources, vertexShader, fragmentShader, true);
    }

    public Program<ContextType> createProgram(IBRResources<ContextType> resources, File vertexShader, File fragmentShader,
        boolean visibilityAndShadowTests, Map<String, Object> additionalDefines)
        throws FileNotFoundException
    {
        // Common definitions for all specular fitting related shaders.
        ProgramBuilder<ContextType> programBuilder = getShaderProgramBuilder(resources, vertexShader, fragmentShader, visibilityAndShadowTests);

        // Add additional defines provided to the function.
        for (Entry<String, Object> definition : additionalDefines.entrySet())
        {
            programBuilder.define(definition.getKey(), definition.getValue());
        }

        // Actually create the program.
        Program<ContextType> program = programBuilder.createProgram();

        // Setup uniforms.
        setupShaderProgram(resources, program);

        return program;
    }

    public Program<ContextType> createProgram(IBRResources<ContextType> resources, File vertexShader, File fragmentShader, boolean visibilityAndShadowTests) throws FileNotFoundException
    {
        return createProgram(resources, vertexShader, fragmentShader, visibilityAndShadowTests, Collections.emptyMap());
    }

    public Program<ContextType> createProgram(IBRResources<ContextType> resources, File vertexShader, File fragmentShader) throws FileNotFoundException
    {
        return createProgram(resources, vertexShader, fragmentShader, true);
    }

    public void setupShaderProgram(IBRResources<ContextType> resources, Program<ContextType> program)
    {
        resources.setupShaderProgram(program);
        program.setUniform("occlusionBias", ibrSettings.getFloat("occlusionBias"));
    }
}
