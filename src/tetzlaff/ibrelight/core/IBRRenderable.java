/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Cubemap;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.interactive.InteractiveRenderable;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.*;
import tetzlaff.util.AbstractImage;

/**
 * Interface for the implementation of the actual image-based rendering / relighting technique.
 * @param <ContextType> The type of the graphics context that this implementation uses.
 */
public interface IBRRenderable<ContextType extends Context<ContextType>> extends InteractiveRenderable<ContextType>
{
    /**
     * Draw the object using the current settings and selections in IBRelight,
     * potentially in subdivisions to avoid graphics card timeouts.
     * @param framebuffer The framebuffer into which to draw the object.
     * @param viewOverride The view matrix.  If this is null, it will default to the current viewpoint in the app.
     * @param projectionOverride The projection matrix.  If this is null, it will default to the current camera in the app.
     * @param subdivWidth The width of the rectangle of pixels to draw at once.  This can be set to a fraction of the
     *                    framebuffer width to reduce the likelihood of graphics card timeouts that would crash IBRelight.
     * @param subdivHeight The height of the rectangle of pixels to draw at once.  This can be set to a fraction of the
     *                     framebuffer height to reduce the likelihood of graphics card timeouts that would crash IBRelight.
     */
    void draw(Framebuffer<ContextType> framebuffer, Matrix4 viewOverride, Matrix4 projectionOverride, int subdivWidth, int subdivHeight);

    /**
     * Draw the object using the current settings and selections in IBRelight.
     * The whole frame will be drawn at once (no subdivisions).
     * @param framebuffer The framebuffer into which to draw the object.
     * @param viewOverride The view matrix.  If this is null, it will default to the current viewpoint in the app.
     * @param projectionOverride The projection matrix.  If this is null, it will default to the current camera in the app.
     */
    void draw(Framebuffer<ContextType> framebuffer, Matrix4 viewOverride, Matrix4 projectionOverride);

    /**
     * Draw the object using the current settings and viewpoint in IBRelight.
     * The whole frame will be drawn at once (no subdivisions).
     * @param framebuffer The framebuffer into which to draw the object.
     */
    @Override
    default void draw(Framebuffer<ContextType> framebuffer)
    {
        draw(framebuffer, null, null);
    }

    /**
     * Sets the loading monitor for this implementation.
     * The implementation may use this as a callback to update a loading bar as the object is being loaded.
     * @param loadingMonitor
     */
    void setLoadingMonitor(LoadingMonitor loadingMonitor);

    /**
     * Gets the view set for the currently loaded object.
     * @return The view set.
     */
    ViewSet getActiveViewSet();

    /**
     * Gets the geometry mesh for the currently loaded object.
     * @return The geometry mesh.
     */
    VertexGeometry getActiveGeometry();

    /**
     * Gets the scene model (object, camera, and lights)
     * Upon modifying the object, camera, or lights models,
     * subsequent changes to the model will be reflected in the behavior of this implementation.
     * @return The scene model.
     */
    SceneModel getSceneModel();

    /**
     * Gets pixel-by-pixel information about what is currently being displayed on screen.
     * @return The information encapsulated as a SceneViewport instance.
     */
    SceneViewport getSceneViewportModel();

    /**
     * Reloads all of the shaders.
     */
    void reloadShaders();

    /**
     * Load a new backplate image.
     * @param backplateFile The backplate image file.
     * @throws FileNotFoundException If the backplate image is not found.
     */
    void loadBackplate(File backplateFile) throws FileNotFoundException;

    /**
     * Load a new environment map image.
     * @param environmentFile The environment map image file.
     * @throws FileNotFoundException If the environment map image is not found.
     */
    Optional<AbstractImage> loadEnvironmentMap(File environmentFile) throws FileNotFoundException;

    /**
     * Gets the resources used by this IBR implementation.
     * These resources can be used to accomplish other shading tasks other than the built-in image-based renderer.
     * The resources are automatically destroyed when this implementation closes.
     * @return The IBR resources.
     */
    IBRResources<ContextType> getResources();

    /**
     * Gets the currently loaded environment map.
     * @return The environment map.
     */
    Optional<Cubemap<ContextType>> getEnvironmentMap();

    /**
     * Sets the tonemapping curve used to interpret the photographic data.
     * The data points specified using this function will be interpolated to form a smooth decoding curve.
     * @param linearLuminanceValues A sequence of luminance values interpreted as physically linear (gamma decoded).
     * @param encodedLuminanceValues A sequence of gamma-encoded luminance values representing the actual pixel values
     *                               that might be found in the photographs.
     */
    void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues);

    /**
     * Accept the current light calibration (intended to be used only by the application, when in light calibration mode).
     */
    void applyLightCalibration();
}
