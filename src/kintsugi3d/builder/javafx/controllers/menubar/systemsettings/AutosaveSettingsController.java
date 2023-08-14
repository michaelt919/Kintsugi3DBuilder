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

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.InternalModels;

import java.awt.*;
import java.io.File;

public class AutosaveSettingsController implements SystemSettingsControllerBase{
    @FXML public javafx.scene.control.ChoiceBox<String> autosaveOptionsChoiceBox;
    String defaultAutosavePath = "C:\\";//TODO: WILL CHANGE WHEN FILE STRUCTURE IS CEMENTED

    String defaultAutosaveSelection = "Default Path: --> " + defaultAutosavePath;
    static final String CHOOSE_LOCATION = "Choose Location...";
    private DirectoryChooser directoryChooser = new DirectoryChooser();

    private Window window;

    @Override
    public void init() {
        //add "Default Path" and "Choose Location..." items to choiceBox
        //initialize directory selection dropdown menu
        autosaveOptionsChoiceBox.getItems().addAll(defaultAutosaveSelection, CHOOSE_LOCATION);

        //initialize option to default path
        autosaveOptionsChoiceBox.setValue(defaultAutosaveSelection);

        //attach event handler (this cannot be done in scenebuilder)
        autosaveOptionsChoiceBox.setOnAction(this::handleDirectoryDropdownSelection);
    }

    private void handleDirectoryDropdownSelection(ActionEvent event) {
        //if user clicks "choose directory" option, open the directory chooser
        //then add an item to the dropdown which contains the path they selected

        if (autosaveOptionsChoiceBox.getValue().equals(CHOOSE_LOCATION)){
            this.directoryChooser.setTitle("Choose an output directory");

            Stage stage = (Stage) window;
            File file = this.directoryChooser.showDialog(stage.getOwner());

            if (file != null && file.exists()){
                directoryChooser.setInitialDirectory(file);
                autosaveOptionsChoiceBox.getItems().add(file.getAbsolutePath());
                autosaveOptionsChoiceBox.setValue(file.getAbsolutePath());
            }
            else{
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    public void injectWindow(Window window){
        this.window = window;
    }

    @Override
    public void bindInfo(InternalModels internalModels) {
        //TODO: imp.
    }

}
