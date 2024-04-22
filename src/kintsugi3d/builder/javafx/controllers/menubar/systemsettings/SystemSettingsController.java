/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar.systemsettings;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.InternalModels;

public class SystemSettingsController {

    public AnchorPane settingsFxmlHost;//holds the fxml which contains whatever settings the user is modifying
    //TODO: NEED TO REAPPLY ALL DEFAULT CHECKBOX SETTINGS
    @FXML
    private ListView<String> settingsListView;

    //Note: settings string MUST MATCH their .fxml counterparts
    //ex. Autosave Settings --> AutosaveSettings.fxml
    //    System Memory --> SysMem.fxml will not work
    static final String[] settingsNames =
    {
        //"Accessibility",
        //"Autosave Settings",
        "Cache Settings",
        "Lighting Settings",
        "Photo Projection Settings",
        "System Memory Settings",
        "Visual Settings"//,
//        "Miscellaneous"
    };
    private InternalModels internalModels;
    private Window window;

    public void init(InternalModels internalModels, Window window) {
        this.internalModels = internalModels;
        this.window = window;
        populateFileList();
        //initialize listeners for cell items
        settingsListView.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            String selectedItem = settingsListView.getSelectionModel().getSelectedItem();

            //remove spaces from string and append ".fxml"
            String fileName = "/fxml/menubar/systemsettings/" + selectedItem.replace(" ", "") + ".fxml";

            Parent newContent = null;
            try {
                //TODO: VERIFY THAT THE USER HAS SAVED ALL NECESSARY SETTINGS BEFORE SWITCHING?
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fileName));
                newContent = loader.load();

                //initialize controller
                SystemSettingsControllerBase controller = loader.getController();
                controller.init();

                //attach controller info
                controller.bindInfo(internalModels);
                if (controller instanceof AutosaveSettingsController){
                    ((AutosaveSettingsController) controller).injectWindow(window);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (newContent != null) {
                settingsFxmlHost.getChildren().setAll(newContent);
            }
        });
        settingsListView.setFixedCellSize(40);
    }

    //fill the listView with the fxml files in String folderPath
    private void populateFileList() {
        settingsListView.getItems().clear();
        settingsListView.getItems().addAll(settingsNames);
    }
}

