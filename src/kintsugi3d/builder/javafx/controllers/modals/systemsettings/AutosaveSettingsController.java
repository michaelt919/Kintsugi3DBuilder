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

package kintsugi3d.builder.javafx.controllers.modals.systemsettings;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.core.JavaFXState;

import java.awt.*;
import java.io.File;

public class AutosaveSettingsController implements SystemSettingsControllerBase
{
    @FXML private ChoiceBox<String> autosaveOptionsChoiceBox;
    private static final String DEFAULT_AUTOSAVE_PATH = "C:\\";//TODO: WILL CHANGE WHEN FILE STRUCTURE IS CEMENTED

    private static final String DEFAULT_AUTOSAVE_SELECTION = String.format("Default Path: --> %s", DEFAULT_AUTOSAVE_PATH);
    static final String CHOOSE_LOCATION = "Choose Location...";
    private final DirectoryChooser directoryChooser = new DirectoryChooser();

    private Window parentWindow;

    @Override
    public void initializeSettingsPage(Window parentWindow, JavaFXState state)
    {
        this.parentWindow = parentWindow;

        //add "Default Path" and "Choose Location..." items to choiceBox
        //initialize directory selection dropdown menu
        autosaveOptionsChoiceBox.getItems().addAll(DEFAULT_AUTOSAVE_SELECTION, CHOOSE_LOCATION);

        //initialize option to default path
        autosaveOptionsChoiceBox.setValue(DEFAULT_AUTOSAVE_SELECTION);

        //attach event handler (this cannot be done in scenebuilder)
        autosaveOptionsChoiceBox.setOnAction(this::handleDirectoryDropdownSelection);
    }

    private void handleDirectoryDropdownSelection(ActionEvent event)
    {
        //if user clicks "choose directory" option, open the directory chooser
        //then add an item to the dropdown which contains the path they selected

        if (autosaveOptionsChoiceBox.getValue().equals(CHOOSE_LOCATION))
        {
            this.directoryChooser.setTitle("Choose an output directory");

            Stage stage = (Stage) parentWindow;
            File file = this.directoryChooser.showDialog(stage.getOwner());

            if (file != null && file.exists())
            {
                directoryChooser.setInitialDirectory(file);
                autosaveOptionsChoiceBox.getItems().add(file.getAbsolutePath());
                autosaveOptionsChoiceBox.setValue(file.getAbsolutePath());
            }
            else
            {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }
}
