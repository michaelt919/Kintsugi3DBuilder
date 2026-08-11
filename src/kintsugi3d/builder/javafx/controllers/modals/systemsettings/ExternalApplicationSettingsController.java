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

package kintsugi3d.builder.javafx.controllers.modals.systemsettings;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.core.JavaFXState;

import java.awt.*;
import java.io.File;

public class ExternalApplicationSettingsController implements SystemSettingsControllerBase
{
    @FXML
    private Label blenderLocationLabel;

    @FXML
    private void assignApplication(MouseEvent e)
    {
        // Should only ever be called by a label
        Label label = (Label) e.getSource();
        File file = new File(label.getText());
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Application");

        // Start file search where current file is
        if (file.exists())
        {
            fileChooser.setInitialDirectory(file.getParentFile());
        }

        // Add file selection options
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Executable", "*.exe"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        // Open the file window and assign the path if valid
        file = fileChooser.showOpenDialog(((Node) e.getSource()).getScene().getWindow());
        if ((file != null) && file.canExecute())
        {
            label.setText(file.getAbsolutePath());
        }
    }

    @Override
    public void initializePage(Window parentWindow, JavaFXState state)
    {
        blenderLocationLabel.textProperty().bindBidirectional(state.getSettingsModel().getObjectProperty("blenderLocation", String.class));
    }
}
