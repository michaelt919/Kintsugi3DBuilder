/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.core;

import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace;
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
public interface ProjectInstance<ContextType extends Context<ContextType>> extends InteractiveRenderable<ContextType>
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
        this.draw(framebuffer, viewOverride, projectionOverride, framebufferSize.width , framebufferSize.height);
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
     * @param progressMonitor
     */
    void setProgressMonitor(ProgressMonitor progressMonitor);

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
     * Gets the resources used by this project instance.
     * These resources can be used to accomplish other shading tasks other than the built-in image-based renderer.
     * The resources are automatically destroyed when this implementation closes.
     * @return The project resources.
     */
    GraphicsResourcesImageSpace<ContextType> getResources();

    /**
     * Gets the scene resource manager (handles environment map, backplate, tonemapping, etc.)
     * @return The secene resources manager
     */
    DynamicResourceManager getDynamicResourceManager();

    default void saveGLTF(File outputDirectory, ExportSettings settings)
    {
        saveGLTF(outputDirectory, "model.glb", settings);
    }

    default void saveGLTF(File outputDirectory, String filename, ExportSettings settings)
    {
        saveGLTF(outputDirectory, filename, settings, null);
    }

    /**
     * Saves the glTF file.
     * Various export settings are available, including the ability to toggle whether textures are saved automatically.
     * Typically, there are three scenarios in which this might be called.<br/><br/>
     * 1) When specular fit / process textures finishes, export the glTF model so that it can be previewed in Kintsugi 3D Viewer.
     *      In this scenario, typically it should be configured to not re-export the textures
     *      since the specular fit process saves textures automatically as it processes them.<br/><br/>
     * 2) When the project is saved, similarly export the glTF model so that it can be previewed in Kintsugi 3D Viewer.
     *      In this scenario, texture saving should be the responsibility of the code module that also handles loading
     *      so that projects can successfully find the texture files.
     *      Ideally this matches the texture filenames specified in the glTF model
     *      so that the Kintsugi 3D Viewer can load them successfully --
     *      but this is a lower priority than ensuring that Kintsugi 3D Builder projects open correctly.<br/><br/>
     * 3) When exporting the project, save the glTF model for use in Kintsugi 3D Viewer, Sketchfab, etc.
     *      This is the one scenario in which the glTF model export should also handle texture export --
     *      since the primary purpose is for transmission to Kintsugi 3D Viewer, Sketchfab and so on,
     *      so it is critical that the textures can be found by the glTF model
     *      (more so than the convenience feature of opening in Kintsugi 3D Viewer;
     *      this is the primary export mechanism and must work correctly.)
     *      This also the only scenario in which exporting LODs is relevant since they aren't really needed
     *      or desirable for the Kintsugi 3D Viewer preview, or used by Builder itself.
     * @param outputDirectory
     * @param filename
     * @param settings
     * @param finishedCallback
     */
    void saveGLTF(File outputDirectory, String filename, ExportSettings settings, Runnable finishedCallback);
}
