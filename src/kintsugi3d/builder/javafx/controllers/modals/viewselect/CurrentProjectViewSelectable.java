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

package kintsugi3d.builder.javafx.controllers.modals.viewselect;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.io.primaryview.GenericViewSelectionModel;
import kintsugi3d.builder.io.primaryview.ViewSelectionModel;

import java.util.function.Consumer;

public class CurrentProjectViewSelectable extends ViewSelectableBase
{
    @Override
    public String getAdvanceLabelOverride()
    {
        // No override
        return null;
    }

    @Override
    public void loadForViewSelection(Consumer<ViewSelectionModel> onLoadComplete)
    {
        ViewSet currentViewSet = Global.state().getIOModel().validateHandler().getLoadedViewSet();
        setViewSelectionModel(new GenericViewSelectionModel("Current Project", currentViewSet));

        if (currentViewSet.getOrientationViewIndex() >= 0)
        {
            String viewName = currentViewSet.getImageFileName(currentViewSet.getOrientationViewIndex());
            selectView(viewName, currentViewSet.getOrientationViewRotationDegrees());
        }

        onLoadComplete.accept(getViewSelectionModel());
    }

    @Override
    public void confirm()
    {
        // Will be handled by the controller itself if a project is already loaded.
    }
}
