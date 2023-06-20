/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.rendering.resources;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.geometry.GeometryResources;
import tetzlaff.gl.geometry.GeometryTextures;
import tetzlaff.gl.material.TextureLoadOptions;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.StandardRenderingMode;
import tetzlaff.ibrelight.core.ViewSet;

import java.io.IOException;

public class IBRResourcesTextureSpace<ContextType extends Context<ContextType>> extends IBRResourcesBase<ContextType>
{
    /**
     * Array of image data pre-projected into texture space
     */
    private Texture3D<ContextType> textureArray;

    /**
     * Simple rectangle for draw calls
     */
    private VertexBuffer<ContextType> rectangle;

    /**
     * Textures storing pre-computed geometry information per-texel
     */
    private GeometryTextures<ContextType> geometryTextures;

    protected IBRResourcesTextureSpace(
            ContextType context, ViewSet viewSet, float[] cameraWeights, GeometryResources<ContextType> geometryResources,
            TextureLoadOptions loadOptions, int texWidth, int texHeight, LoadingMonitor loadingMonitor) throws IOException
    {
        super(context, viewSet, cameraWeights, geometryResources.geometry.getMaterial(), loadOptions);

        // Deferred rendering: draw to geometry textures initially and then just draw using a rectangle and
        // the geometry info cached in the textures
        geometryTextures = geometryResources.createGeometryFramebuffer(texWidth, texHeight);
        rectangle = context.createRectangle();
    }

    @Override
    public ProgramBuilder<ContextType> getShaderProgramBuilder(StandardRenderingMode renderingMode)
    {
        return super.getShaderProgramBuilder(renderingMode)
            .define("GEOMETRY_TEXTURES_ENABLED", true);
    }

    @Override
    public void setupShaderProgram(Program<ContextType> program)
    {
        super.setupShaderProgram(program);
        program.setTexture("positionTex", geometryTextures.getPositionTexture());
        program.setTexture("normalTex", geometryTextures.getNormalTexture());
        program.setTexture("tangentTex", geometryTextures.getTangentTexture());
        program.setTexture("viewImages", textureArray);
    }

    @Override
    public Drawable<ContextType> createDrawable(Program<ContextType> program)
    {
        Drawable<ContextType> drawable = getContext().createDrawable(program);
        drawable.addVertexBuffer("position", rectangle);
        return drawable;
    }

    @Override
    public void close()
    {
        if (this.geometryTextures != null)
        {
            this.geometryTextures.close();
            this.geometryTextures = null;
        }

        if (this.rectangle != null)
        {
            this.rectangle.close();
            this.rectangle = null;
        }
    }
}
