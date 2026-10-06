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

package kintsugi3d.builder.test;

import kintsugi3d.builder.core.Kintsugi3DBuilderState;
import kintsugi3d.builder.core.viewset.View;
import kintsugi3d.builder.state.CacheModel;
import kintsugi3d.builder.state.CarouselModel;
import kintsugi3d.builder.state.SelectableViewListModel;
import kintsugi3d.builder.state.cards.CardsModel;
import kintsugi3d.builder.state.cards.ProjectDataCardFactory;
import kintsugi3d.builder.state.cards.TabsModel;
import kintsugi3d.builder.state.project.ProjectModel;
import kintsugi3d.builder.state.scene.*;
import kintsugi3d.builder.state.settings.DefaultSettings;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import kintsugi3d.builder.state.settings.SimpleGeneralSettingsModel;
import kintsugi3d.builder.state.shader.ActiveShaderModel;
import kintsugi3d.builder.test.TestingProjectModel.ErrorMessage;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

import java.util.Collection;
import java.util.List;
import java.util.Map;

class TestingState implements Kintsugi3DBuilderState
{
    private final TestingProjectModel projectModel = new TestingProjectModel();
    private final GeneralSettingsModel settings = new SimpleGeneralSettingsModel();
    private final ReadonlyViewpointModel cameraModel = new SimpleCameraModel();
    private final ReadonlyLightingEnvironmentModel lightingEnvironmentModel = new TestingLightingModel();
    private final SelectableViewListModel viewListModel = new TestingViewListModel();
    private final TabsModel tabsModel = new TestingTabsModel();
    private final ObjectPoseModel objectModel = new TestingObjectModel();

    TestingState()
    {
        DefaultSettings.applyGlobalDefaults(settings);
    }

    @Override
    public ReadonlyViewpointModel getCameraModel()
    {
        return cameraModel;
    }

    @Override
    public ReadonlyLightingEnvironmentModel getLightingModel()
    {
        return lightingEnvironmentModel;
    }

    @Override
    public ReadonlyObjectPoseModel getObjectModel()
    {
        return  objectModel;
    }

    @Override
    public ActiveShaderModel getUserShaderModel()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public SelectableViewListModel getViewListModel()
    {
        return viewListModel;
    }

    @Override
    public TabsModel getTabModels()
    {
        return tabsModel;
    }

    @Override
    public CarouselModel getCarouselModel()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public GeneralSettingsModel getSettingsModel()
    {
        return settings;
    }

    @Override
    public CacheModel getCacheModel()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectModel getProjectModel()
    {
        return projectModel;
    }

    public Collection<ErrorMessage> getErrors()
    {
        return projectModel.getErrors();
    }

    public Collection<ErrorMessage> getWarnings()
    {
        return projectModel.getWarnings();
    }
}
