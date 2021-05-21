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

public interface IBRRenderable<ContextType extends Context<ContextType>> extends InteractiveRenderable<ContextType>
{
    void draw(Framebuffer<ContextType> framebuffer, Matrix4 viewOverride, Matrix4 projectionOverride, int subdivWidth, int subdivHeight);
    void setLoadingMonitor(LoadingMonitor loadingMonitor);

    ViewSet getActiveViewSet();
    VertexGeometry getActiveGeometry();

    SceneViewport getSceneViewportModel();

    SafeReadonlySettingsModel getSettingsModel();
    void setSettingsModel(ReadonlySettingsModel settingsModel);

    void reloadShaders();

    void loadBackplate(File backplateFile) throws FileNotFoundException;
    Optional<AbstractImage> loadEnvironmentMap(File environmentFile) throws FileNotFoundException;

    ReadonlyObjectModel getObjectModel();
    ReadonlyCameraModel getCameraModel();
    ReadonlyLightingModel getLightingModel();

    void setObjectModel(ReadonlyObjectModel objectModel);
    void setCameraModel(ReadonlyCameraModel cameraModel);
    void setLightingModel(ReadonlyLightingModel lightingModel);

    void setMultiTransformationModel(List<Matrix4> multiTransformationModel);
    void setReferenceScene(VertexGeometry scene);

    IBRResources<ContextType> getResources();

    Optional<Cubemap<ContextType>> getEnvironmentMap();
    Matrix4 getEnvironmentMapMatrix();

    Matrix4 getAbsoluteViewMatrix(Matrix4 relativeViewMatrix);

    void draw(Framebuffer<ContextType> framebuffer, Matrix4 viewOverride, Matrix4 projectionOverride);

    @Override
    default void draw(Framebuffer<ContextType> framebuffer)
    {
        draw(framebuffer, null, null);
    }

    void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues);
    void applyLightCalibration();
}
