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

package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import kintsugi3d.builder.javafx.internal.CameraViewListModelImpl;

public class CameraViewListController
{
    @FXML private TextField searchTxtField;
    @FXML private ListView<String> cameraViewList;
    @FXML private CheckBox snapToView;

    private SearchableListView searchableListView;

    public void init(CameraViewListModelImpl cameraViewListModel)
    {
        cameraViewListModel.setSelectedCameraViewModel(cameraViewList.getSelectionModel());
        cameraViewListModel.setCameraViewListProperty(cameraViewList.itemsProperty());
        cameraViewListModel.setCameraViewSnapEnabledProperty(snapToView.selectedProperty());
        searchableListView = SearchableListView.createUnboundInstance(cameraViewList, searchTxtField);
    }

    public void rebindSearchableListView(){
        searchableListView.bind();
    }
}
