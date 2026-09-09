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

package kintsugi3d.builder.core;

import kintsugi3d.builder.state.*;
import kintsugi3d.builder.state.cards.TabsModel;
import kintsugi3d.builder.state.project.ProjectModel;
import kintsugi3d.builder.state.scene.ReadonlyLightingEnvironmentModel;
import kintsugi3d.builder.state.scene.ReadonlyObjectPoseModel;
import kintsugi3d.builder.state.scene.ReadonlyViewpointModel;
import kintsugi3d.builder.state.scene.UserShaderModel;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;

public interface Kintsugi3DBuilderState
{
    // Models related to presentation within Kintsugi 3D Builder
    ReadonlyViewpointModel getCameraModel();
    ReadonlyLightingEnvironmentModel getLightingModel();
    ReadonlyObjectPoseModel getObjectModel();
    UserShaderModel getUserShaderModel();

    // Mainly for light calibration?
    CameraViewListModel getCameraViewListModel();

    // Cards and tabs
    TabsModel getTabModels();

    // Global settings
    /**
     * Not read-only to allow export functions to change rendering mode (i.e. focus calibration)
     * @return
     */
    GeneralSettingsModel getSettingsModel();

    // Settings that must be applied prior to load
    LoadOptionsModel getLoadOptionsModel();

    // Project cache info and access
    CacheModel getCacheModel();

    // Main view and carousel
    CanvasModel getMainCanvasModel();
    CanvasListModel getCanvasListModel();
    CarouselModel getCarouselModel();

    // Global access to 3D view
    SceneViewportModel getSceneViewportModel();

    // Load / save / export of project elements -- not intended for frontend display
    IOModel getIOModel();

    // Global project state and other miscellaneous properties that are available for frontend display.
    ProjectModel getProjectModel();
}
