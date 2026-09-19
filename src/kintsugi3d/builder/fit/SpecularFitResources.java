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

package kintsugi3d.builder.fit;

import kintsugi3d.builder.fit.settings.ReadonlyBasisSettings;
import kintsugi3d.builder.resources.project.ReadonlyGraphicsResources;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Drawable;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.geometry.ReadonlyGeometryResources;

final class SpecularFitResources<ContextType extends Context<ContextType>> implements ReadonlyGraphicsResources<ContextType>
{
    private final ReadonlyGraphicsResources<ContextType> base;
    private final ReadonlyBasisSettings basisSettings;
    private final boolean smithMaskingShadowing;

    SpecularFitResources(ReadonlyGraphicsResources<ContextType> base, boolean smithMaskingShadowing, ReadonlyBasisSettings basisSettings)
    {
        this.base = base;
        this.basisSettings = basisSettings;
        this.smithMaskingShadowing = smithMaskingShadowing;
    }

    @Override
    public ProgramBuilder<ContextType> getShaderProgramBuilder()
    {
        // Common definitions for all specular fitting related shaders.
        // Explicitly set basis count and resolution since the base shader program builder
        // might be still referencing an old or non-existent material basis.

        ProgramBuilder<ContextType> builder = base.getShaderProgramBuilder()
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", smithMaskingShadowing);

        if (basisSettings != null)
        {
            // If basis settings are defined, override whatever basis count and basis resolution
            // would otherwise be specified by the wrapped resources.
            builder
                .define("BASIS_COUNT", basisSettings.getBasisCount())
                .define("BASIS_RESOLUTION", basisSettings.getBasisResolution());
        }

        return builder;
    }

    @Override
    public void setupShaderProgram(Program<ContextType> program)
    {
        base.setupShaderProgram(program);
    }

    @Override
    public ContextType getContext()
    {
        return base.getContext();
    }

    @Override
    public ReadonlyGeometryResources<ContextType> getGeometryResources()
    {
        return base.getGeometryResources();
    }

    @Override
    public Drawable<ContextType> createDrawable(Program<ContextType> program)
    {
        return base.createDrawable(program);
    }
}
