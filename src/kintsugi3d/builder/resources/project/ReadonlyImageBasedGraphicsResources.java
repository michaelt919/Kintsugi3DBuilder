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

import kintsugi3d.builder.core.viewset.ReadonlyViewSet;
import kintsugi3d.builder.io.events.ProjectProcessedListener;
import kintsugi3d.builder.resources.project.specular.ReadonlyTextureResources;
import kintsugi3d.builder.resources.project.stream.GraphicsStreamFactory;
import kintsugi3d.builder.util.EventListeners;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.vecmath.IntVector2;

import java.util.List;

public interface ReadonlyImageBasedGraphicsResources<ContextType extends Context<ContextType>> extends ReadonlyGraphicsResources<ContextType>
{
    /**
     * The view set that these resources were loaded from.
     * @return A read-only view of the view set
     */
    ReadonlyViewSet getViewSet();

    /**
     * Diffuse, normal, specular, roughness maps
     * @return
     */
    ReadonlyTextureResources<ContextType> getTextureResources();

    /**
     * 1D textures for encoding and decoding
     * @return
     */
    ReadonlyLuminanceMapResources<ContextType> getLuminanceMapResources();

    /**
     * Gets the weight associated with a given view/camera (determined by the distance from other views).
     *
     * @param index The index of the view for which to retrieve its weight.
     * @return The weight for the specified view.
     */
    float getCameraWeight(int index);

    /**
     * Gets a read-only view of the whole list of camera weights
     * @return
     */
    List<Float> getCameraWeights();

    boolean hasProcessedWeightMaps();

    /**
     *
     * @return The texture resolution of the weight maps if the project has been fully processed,
     * otherwise throws IllegalStateException
     */
    IntVector2 getProcessedWeightMapResolution();

    EventListeners<ProjectProcessedListener> weightMapsProcessedListeners();

    /**
     * Stream over enabled views only -- disabled views are not included.
     * @return
     */
    default GraphicsStreamFactory<ContextType> streamFactory()
    {
        return new GraphicsStreamFactory<>(this);
    }
}
