/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.resources.ibr;

import kintsugi3d.builder.core.ColorAppearanceMode;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Drawable;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.geometry.GeometryMode;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.material.TextureLoadOptions;

public class IBRResourcesAnalytic<ContextType extends Context<ContextType>> extends IBRResourcesBase<ContextType>
{
    public IBRResourcesAnalytic(ContextType context, ViewSet viewSet, VertexGeometry geometry)
    {
        super(new IBRSharedResources<>(context, viewSet, geometry, new TextureLoadOptions()), true);
    }

    @Override
    public ProgramBuilder<ContextType> getShaderProgramBuilder()
    {
        return getSharedResources().getShaderProgramBuilder()
                .define("GEOMETRY_MODE", GeometryMode.PROJECT_3D_TO_2D) // should default to this, but just in case
                .define("GEOMETRY_TEXTURES_ENABLED", false) // should default to this, but just in case
                .define("COLOR_APPEARANCE_MODE", ColorAppearanceMode.ANALYTIC)
                .define("CAMERA_PROJECTION_COUNT", getViewSet().getCameraProjectionCount());
    }

    @Override
    public void setupShaderProgram(Program<ContextType> program)
    {
        getSharedResources().setupShaderProgram(program);
    }

    @Override
    public Drawable<ContextType> createDrawable(Program<ContextType> program)
    {
        return getGeometryResources().createDrawable(program);
    }
}
