/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.multithread;

import java.util.List;

import kintsugi3d.builder.javafx.util.MultithreadValue;
import kintsugi3d.builder.state.CameraViewListModel;

public class CameraViewListModelWrapper implements CameraViewListModel
{
    private final MultithreadValue<Integer> selectedCameraViewIndex;
    private final MultithreadValue<List<String>> cameraViewList;
    private final MultithreadValue<Boolean> cameraViewSnapEnabled;
    private final CameraViewListModel baseModel;

    public CameraViewListModelWrapper(CameraViewListModel baseModel)
    {
        this.selectedCameraViewIndex = MultithreadValue.createFromFunctions(baseModel::getSelectedCameraViewIndex, baseModel::setSelectedCameraViewIndex);
        this.cameraViewList = MultithreadValue.createFromFunctions(baseModel::getCameraViewList, baseModel::setCameraViewList);
        this.cameraViewSnapEnabled = MultithreadValue.createFromFunctions(baseModel::isCameraViewSnapEnabled, baseModel::setCameraViewSnapEnabled);
        this.baseModel = baseModel;
    }

    /**
     * Not synchronized; may be out of sync with the view index and/or view list model.
     * @return
     */
    @Override
    public String getSelectedCameraView()
    {
        // Not synchronized; should be fine -- worst case scenario it's just out-of-sync with the view index and/or view list model.
        return baseModel.getSelectedCameraView();
    }

    @Override
    public int getSelectedCameraViewIndex()
    {
        return selectedCameraViewIndex.getValue();
    }

    @Override
    public void setSelectedCameraViewIndex(int cameraViewIndex)
    {
        this.selectedCameraViewIndex.setValue(cameraViewIndex);
    }

    @Override
    public List<String> getCameraViewList()
    {
        return cameraViewList.getValue();
    }

    @Override
    public void setCameraViewList(List<String> cameraViewList)
    {
        // Need to run on JavaFX thread as this will completely change the backend model for the list view.
        this.cameraViewList.setValue(cameraViewList);
    }

    @Override
    public boolean isCameraViewSnapEnabled()
    {
        return cameraViewSnapEnabled.getValue();
    }

    @Override
    public void setCameraViewSnapEnabled(boolean cameraViewSnapEnabled)
    {
        this.cameraViewSnapEnabled.setValue(cameraViewSnapEnabled);
    }
}
