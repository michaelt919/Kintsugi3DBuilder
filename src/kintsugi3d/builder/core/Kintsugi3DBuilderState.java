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

import kintsugi3d.builder.state.CameraViewListModel;
import kintsugi3d.builder.state.CanvasModel;
import kintsugi3d.builder.state.SceneViewportModel;
import kintsugi3d.builder.state.cards.TabsModel;
import kintsugi3d.builder.state.project.ProjectModel;
import kintsugi3d.builder.state.scene.ReadonlyLightingEnvironmentModel;
import kintsugi3d.builder.state.scene.ReadonlyObjectPoseModel;
import kintsugi3d.builder.state.scene.ReadonlyViewpointModel;
import kintsugi3d.builder.state.scene.UserShaderModel;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;

public interface Kintsugi3DBuilderState
{
    ReadonlyViewpointModel getCameraModel();
    ReadonlyLightingEnvironmentModel getLightingModel();
    ReadonlyObjectPoseModel getObjectModel();
    UserShaderModel getUserShaderModel();

    CameraViewListModel getCameraViewListModel();
    TabsModel getTabModels();

    /**
     * Not read-only to allow export functions to change rendering mode (i.e. focus calibration)
     * @return
     */
    GeneralSettingsModel getSettingsModel();

    CanvasModel getCanvasModel();

    SceneViewportModel getSceneViewportModel();
    LoadOptionsModel getLoadOptionsModel();
    IOModel getIOModel();

    ProjectModel getProjectModel();
}
