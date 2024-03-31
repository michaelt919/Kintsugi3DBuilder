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

package kintsugi3d.builder.javafx.internal;

import java.util.Collections;
import java.util.List;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.Property;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;
import kintsugi3d.builder.state.CameraViewListModel;

public class CameraViewListModelImpl implements CameraViewListModel
{
    private MultipleSelectionModel<String> selectedCameraViewModel;
    private Property<ObservableList<String>> cameraViewListProperty;

    @Override
    public String getSelectedCameraView()
    {
        return selectedCameraViewModel.getSelectedItem();
    }

    @Override
    public int getSelectedCameraViewIndex()
    {
        return selectedCameraViewModel.getSelectedIndex();
    }

    @Override
    public void setSelectedCameraViewIndex(int cameraViewIndex)
    {
        selectedCameraViewModel.getSelectedIndices().clear();
        selectedCameraViewModel.getSelectedIndices().add(cameraViewIndex);
    }

    @Override
    public List<String> getCameraViewList()
    {
        return Collections.unmodifiableList(cameraViewListProperty.getValue());
    }

    @Override
    public void setCameraViewList(List<String> cameraViewList)
    {
        cameraViewListProperty.setValue(new ObservableListWrapper<>(cameraViewList));
    }

    public MultipleSelectionModel<String> getSelectedCameraViewModel()
    {
        return selectedCameraViewModel;
    }

    public Property<ObservableList<String>> getCameraViewListProperty()
    {
        return cameraViewListProperty;
    }

    public void setSelectedCameraViewModel(MultipleSelectionModel<String> selectedCameraViewModel)
    {
        this.selectedCameraViewModel = selectedCameraViewModel;
    }

    public void setCameraViewListProperty(Property<ObservableList<String>> cameraViewListProperty)
    {
        this.cameraViewListProperty = cameraViewListProperty;
    }
}
