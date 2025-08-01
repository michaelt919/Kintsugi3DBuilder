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

package kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources;

import javafx.scene.control.TreeItem;
import javafx.stage.FileChooser;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.io.primaryview.GenericPrimaryViewSelectionModel;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.PrimaryViewSelectController;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class CurrentProjectInputSource extends InputSource
{
    @Override
    public List<FileChooser.ExtensionFilter> getExtensionFilters()
    {
        return List.of();
    }

    @Override
    public void initTreeView()
    {
        ViewSet currentViewSet = Global.state().getIOModel().getLoadedViewSet();
        primaryViewSelectionModel = new GenericPrimaryViewSelectionModel("Current Project", currentViewSet);

        addTreeElems(primaryViewSelectionModel);
        searchableTreeView.bind();
    }

    @Override
    public void setOrientationViewDefaultSelections(PrimaryViewSelectController controller)
    {
        ViewSet currentViewSet = Global.state().getIOModel().getLoadedViewSet();

        if (currentViewSet == null)
            return;

        // Set the initial selection to what is currently being used
        TreeItem<String> selectionItem = InputSource.NONE_ITEM;

        if (currentViewSet.getOrientationViewIndex() >= 0)
        {
            String viewName = currentViewSet.getImageFileName(currentViewSet.getOrientationViewIndex());

            for (int i = 0; i < searchableTreeView.getTreeView().getExpandedItemCount(); i++)
            {
                TreeItem<String> item = searchableTreeView.getTreeView().getTreeItem(i);
                if (Objects.equals(item.getValue(), viewName))
                {
                    selectionItem = item;
                    break;
                }
            }
        }

        searchableTreeView.getTreeView().getSelectionModel().select(selectionItem);
        controller.setImageRotation(currentViewSet.getOrientationViewRotationDegrees());
    }

    @Override
    public File getMasksDirectory() {
        ViewSet currentViewSet = MultithreadModels.getInstance().getIOModel().getLoadedViewSet();
        return currentViewSet.getMasksDirectory();
    }

    @Override
    public File getInitialMasksDirectory() {
       return getMasksDirectory();
    }

    @Override
    public boolean doEnableProjectMasksButton() {
        ViewSet currentViewSet = MultithreadModels.getInstance().getIOModel().getLoadedViewSet();
        return currentViewSet.hasMasks();
    }

    @Override
    public void setMasksDirectory(File file) {
        //implement this later if we need to
       throw new UnsupportedOperationException("Cannot change masks directory of an existing project.");
    }

    @Override
    public void loadProject(String orientationViewName, double rotate)
    {
        ViewSet currentViewSet = Global.state().getIOModel().getLoadedViewSet();

        if (currentViewSet == null)
            return;

        if (orientationViewName == null)
        {
            currentViewSet.setOrientationViewIndex(-1);
        }
        else
        {
            currentViewSet.setOrientationView(orientationViewName);
        }

        currentViewSet.setOrientationViewRotationDegrees(rotate);
    }

    @Override
    public boolean equals(Object obj)
    {
        return false;
    }
}
