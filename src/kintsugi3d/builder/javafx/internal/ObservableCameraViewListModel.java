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

package kintsugi3d.builder.javafx.internal;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;
import kintsugi3d.builder.core.viewset.View;
import kintsugi3d.builder.state.CameraViewListModel;

import java.util.Collections;
import java.util.List;

public class ObservableCameraViewListModel implements CameraViewListModel
{
    private MultipleSelectionModel<View> selectedCameraViewModel;
    private Property<ObservableList<View>> cameraViewListProperty;
    private BooleanProperty cameraViewSnapEnabledProperty;

    @Override
    public View getSelectedCameraView() { return selectedCameraViewModel.getSelectedItem();}

    @Override
    public void setSelectedCameraView(View cameraView)
    {
        selectedCameraViewModel.select(cameraView);
    }

    @Override
    public List<View> getCameraViewList()
    {
        return Collections.unmodifiableList(cameraViewListProperty.getValue());
    }

    @Override
    public void setCameraViewList(List<View> cameraViewList)
    {
        cameraViewListProperty.setValue(new ObservableListWrapper<>(cameraViewList));
    }

    @Override
    public boolean isCameraViewSnapEnabled()
    {
        return cameraViewSnapEnabledProperty.get();
    }

    @Override
    public void setCameraViewSnapEnabled(boolean cameraViewSnapEnabled)
    {
        this.cameraViewSnapEnabledProperty.set(cameraViewSnapEnabled);
    }

    public BooleanProperty getCameraViewSnapEnabledProperty()
    {
        return cameraViewSnapEnabledProperty;
    }

    public MultipleSelectionModel<View> getSelectedCameraViewModel()
    {
        return selectedCameraViewModel;
    }

    public Property<ObservableList<View>> getCameraViewListProperty()
    {
        return cameraViewListProperty;
    }

    public void setSelectedCameraViewModel(MultipleSelectionModel<View> selectedCameraViewModel)
    {
        this.selectedCameraViewModel = selectedCameraViewModel;
    }

    public void setCameraViewListProperty(Property<ObservableList<View>> cameraViewListProperty)
    {
        this.cameraViewListProperty = cameraViewListProperty;
    }

    public void setCameraViewSnapEnabledProperty(BooleanProperty cameraViewSnapEnabledProperty)
    {
        this.cameraViewSnapEnabledProperty = cameraViewSnapEnabledProperty;
    }
}
