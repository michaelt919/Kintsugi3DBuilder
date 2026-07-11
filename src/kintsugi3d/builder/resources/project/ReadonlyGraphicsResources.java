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

import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.ReadonlyLoadOptionsModel;
import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.core.UserCancellationException;
import kintsugi3d.builder.resources.project.specular.ReadonlyTextureResources;
import kintsugi3d.builder.resources.project.stream.GraphicsStreamFactory;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Drawable;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.geometry.ReadonlyGeometryResources;
import kintsugi3d.gl.geometry.ReadonlyVertexGeometry;

import java.io.IOException;
import java.util.List;

public interface ReadonlyGraphicsResources<ContextType extends Context<ContextType>> extends ShaderProgramFactory<ContextType>
{
    /**
     * The graphics context associated with this instance.
     *  @return The graphics context
     */
    @Override
    ContextType getContext();

    /**
     * The view set that these resources were loaded from.
     * @return A read-only view of the view set
     */
    ReadonlyViewSet getViewSet();

    /**
     * The geometry used with this instance.
     * @return A read-only view of the geometry
     */
    ReadonlyVertexGeometry getGeometry();

    ReadonlyGeometryResources<ContextType> getGeometryResources();

    /**
     * Diffuse, normal, specular, roughness maps, etc.
     * @return
     */
    ReadonlyTextureResources<ContextType> getTextureResources();

    /**
     * 1D textures for encoding and decoding
     * @return
     */
    ReadonlyLuminanceMapResources<ContextType> getLuminanceMapResources();

    /**
     * Gets a read-only view of the whole list of camera weights
     * @return
     */
    List<Float> getCameraWeights();

    /**
     * Gets the weight associated with a given view/camera (determined by the distance from other views).
     *
     * @param index The index of the view for which to retrieve its weight.
     * @return The weight for the specified view.
     */
    float getCameraWeight(int index);

    /**
     * Creates a Drawable using this instance's geometry resources, and the specified shader program.
     *
     * @param program The program to use to construct the Drawable.
     * @return A Drawable for rendering this instance using the specified shader program.
     */
    Drawable<ContextType> createDrawable(Program<ContextType> program);

    default GraphicsStreamFactory<ContextType> streamFactory()
    {
        return new GraphicsStreamFactory<>(this);
    }
    /**
     * Creates a resource for just a single view, using the default image for that view but with custom load options
     *
     * @param viewIndex
     * @param loadOptions
     * @return
     * @throws IOException
     */
    SingleCalibratedImageResource<ContextType> createSingleImageResource(int viewIndex, ReadonlyLoadOptionsModel loadOptions)
        throws IOException;

    ImageCache<ContextType> cache(ImageCacheSettings settings, ProgressMonitor monitor) throws IOException, UserCancellationException;
}
