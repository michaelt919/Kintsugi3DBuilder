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

package kintsugi3d.builder.core;

import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.builder.state.SceneViewport;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Framebuffer;
import kintsugi3d.gl.core.FramebufferSize;
import kintsugi3d.gl.geometry.ReadonlyVertexGeometry;
import kintsugi3d.gl.interactive.InteractiveRenderable;
import kintsugi3d.gl.vecmath.Matrix4;

import java.io.File;

/**
 * Interface for the implementation of the actual image-based rendering / relighting technique.
 * @param <ContextType> The type of the graphics context that this implementation uses.
 */
public interface IBRInstance<ContextType extends Context<ContextType>> extends InteractiveRenderable<ContextType>
{
    /**
     * Draw the object using the current settings and selections in the 3D viewport,
     * potentially in subdivisions to avoid graphics card timeouts.
     * @param framebuffer The framebuffer into which to draw the object.
     * @param viewOverride The view matrix.  If this is null, it will default to the current viewpoint in the app.
     * @param projectionOverride The projection matrix.  If this is null, it will default to the current camera in the app.
     * @param subdivWidth The width of the rectangle of pixels to draw at once.  This can be set to a fraction of the
     *                    framebuffer width to reduce the likelihood of graphics card timeouts that would crash Kintsugi 3D Builder.
     * @param subdivHeight The height of the rectangle of pixels to draw at once.  This can be set to a fraction of the
     *                     framebuffer height to reduce the likelihood of graphics card timeouts that would crash Kintsugi 3D Builder.
     */
    void draw(Framebuffer<ContextType> framebuffer, Matrix4 viewOverride, Matrix4 projectionOverride, int subdivWidth, int subdivHeight);

    /**
     * Draw the object using the current settings and selections in the 3D viewport.
     * The whole frame will be drawn at once (no subdivisions).
     * @param framebuffer The framebuffer into which to draw the object.
     * @param viewOverride The view matrix.  If this is null, it will default to the current viewpoint in the app.
     * @param projectionOverride The projection matrix.  If this is null, it will default to the current camera in the app.
     */
    default void draw(Framebuffer<ContextType> framebuffer, Matrix4 viewOverride, Matrix4 projectionOverride)
    {
        FramebufferSize framebufferSize = framebuffer.getSize();
        this.draw(framebuffer, viewOverride, projectionOverride, framebufferSize.width, framebufferSize.height);
    }

    /**
     * Draw the object using the current settings and viewpoint in the 3D viewport.
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
     *
     * @return The view set.
     */
    ViewSet getActiveViewSet();

    /**
     * Gets the geometry mesh for the currently loaded object.
     * @return The geometry mesh.
     */
    ReadonlyVertexGeometry getActiveGeometry();

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
     * Gets the resources used by this IBR implementation.
     * These resources can be used to accomplish other shading tasks other than the built-in image-based renderer.
     * The resources are automatically destroyed when this implementation closes.
     * @return The IBR resources.
     */
    IBRResourcesImageSpace<ContextType> getIBRResources();

    /**
     * Gets the scene resource manager (handles environment map, backplate, tonemapping, etc.)
     * @return The secene resources manager
     */
    DynamicResourceManager getDynamicResourceManager();

    void saveGlTF(File outputDirectory, ExportSettings settings);
}
