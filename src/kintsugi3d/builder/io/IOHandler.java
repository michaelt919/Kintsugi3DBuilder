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

package kintsugi3d.builder.io;

import kintsugi3d.builder.core.viewset.ViewSet;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.io.metashape.MetashapeModel;
import kintsugi3d.builder.rendering.RenderableInstance;
import kintsugi3d.builder.state.scene.UserShader;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.interactive.ProgressMonitor;
import kintsugi3d.util.EncodableColorImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

public interface IOHandler
{
    ViewSet getLoadedViewSet();
    VertexGeometry getLoadedGeometry();

    boolean isRenderableLoaded();
    RenderableInstance<?> getMainRenderable();
    RenderableInstance<?> getRenderableForShader(UserShader shader);

    void addViewSetLoadCallback(Consumer<ViewSet> callback);
    void addViewSetLoadCallback(Runnable callback);

    void addMainRenderableLoadCallback(Consumer<RenderableInstance<?>> callback);

    /**
     * Must NOT be called on the rendering thread or deadlock will result while generating preview images.
     * @param id
     * @param vsetFile
     * @param supportingFilesDirectory
     * @param loadOptions
     */
    void loadFromVSETFile(String id, File vsetFile, File supportingFilesDirectory, ReadonlyLoadOptionsModel loadOptions);

    /**
     * Must NOT be called on the rendering thread or deadlock will result while generating preview images.
     * @param id
     * @param xmlFile
     * @param viewSetLoadOptions
     * @param imageLoadOptions
     */
    void loadFromLooseFiles(String id, File xmlFile, ViewSetLoadOptions viewSetLoadOptions, ReadonlyLoadOptionsModel imageLoadOptions);

    /**
     * Must NOT be called on the rendering thread or deadlock will result while generating preview images.
     * @param model
     * @param loadOptionsModel
     */
    void loadFromMetashapeModel(MetashapeModel model, ReadonlyLoadOptionsModel loadOptionsModel);

    Optional<EncodableColorImage> loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException;
    void loadBackplate(File backplateFile) throws FileNotFoundException;

    void saveToVSETFile(File vsetFile) throws IOException;
    void saveAllMaterialFiles(File materialDirectory, Runnable finishedCallback);
    void saveGLTF(File outputDirectory, ExportSettings settings);

    /**
     *
     * @param onUnloadComplete Whether or not a project needs to be unloaded, will run once the unload process has finished.
     *                         This callback is the point at which it is safe to start loading another project again without a race condition.
     *                         The one exception to this rule is that UI elements may still update after the callable has run,
     *                         but the request to update them will have been submitted so it should usually be fine with FIFO sequencing.
     */
    void unload(Runnable onUnloadComplete);

    void setProgressMonitor(ProgressMonitor progressMonitor);

    DoubleUnaryOperator getLuminanceEncodingFunction();
    void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues);
    void clearTonemapping();
    void requestLightIntensityCalibration();

    void applyLightCalibration();
}
