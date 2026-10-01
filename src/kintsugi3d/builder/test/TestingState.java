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
import kintsugi3d.builder.javafx.internal.ObservableViewListModel;
import kintsugi3d.builder.javafx.multithread.SynchronizedViewListModel;
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
    private final ReadonlyLightingEnvironmentModel lightingEnvironmentModel = new ReadonlyLightingEnvironmentModel()
    {
        @Override
        public ReadonlyLightWidgetModel getLightWidgetModel(int index)
        {
            return null;
        }

        @Override
        public int getLightCount()
        {
            return 0;
        }

        @Override
        public int getMaxLightCount()
        {
            return 0;
        }

        @Override
        public boolean isLightVisualizationEnabled(int index)
        {
            return false;
        }

        @Override
        public boolean isLightWidgetEnabled(int index)
        {
            return false;
        }

        @Override
        public boolean areLightWidgetsEthereal()
        {
            return false;
        }

        @Override
        public float getAmbientLightIntensity()
        {
            return 0;
        }

        @Override
        public Vector3 getAmbientLightColor()
        {
            return null;
        }

        @Override
        public boolean isEnvironmentMappingEnabled()
        {
            return false;
        }

        @Override
        public Matrix4 getEnvironmentMapMatrix()
        {
            return null;
        }

        @Override
        public float getEnvironmentMapFilteringBias()
        {
            return 0;
        }

        @Override
        public ReadonlyLightPrototypeModel getLightPrototype(int i)
        {
            return null;
        }

        @Override
        public Matrix4 getLightMatrix(int i)
        {
            return null;
        }

        @Override
        public Vector3 getLightCenter(int i)
        {
            return null;
        }

        @Override
        public float getBackgroundIntensity()
        {
            return 0;
        }

        @Override
        public Vector3 getBackgroundColor()
        {
            return null;
        }

        @Override
        public BackgroundMode getBackgroundMode()
        {
            return null;
        }

        @Override
        public Vector3 getGroundPlaneColor()
        {
            return null;
        }

        @Override
        public boolean isGroundPlaneEnabled()
        {
            return false;
        }

        @Override
        public float getGroundPlaneHeight()
        {
            return 0;
        }

        @Override
        public float getGroundPlaneSize()
        {
            return 0;
        }
    };
    private final SelectableViewListModel viewListModel = new ObservableViewListModel();
    private final TabsModel tabsModel = new TabsModel()
    {
        @Override
        public <T> void addTab(String tabName, ProjectDataCardFactory<T> cardFactory, String path)
        {

        }

        @Override
        public void clearTabs()
        {

        }

        @Override
        public CardsModel<?> getTab(String label)
        {
            return null;
        }

        @Override
        public <T> CardsModel<T> getTab(String label, Class<T> dataClass)
        {
            return null;
        }

        @Override
        public Map<String, ? extends CardsModel<?>> getTabsMap()
        {
            return Map.of();
        }
    };

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
        throw new UnsupportedOperationException();
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
