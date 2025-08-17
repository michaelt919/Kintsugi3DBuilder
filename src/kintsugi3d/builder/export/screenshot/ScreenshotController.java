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

package kintsugi3d.builder.export.screenshot;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.export.screenshot.ScreenshotRequest.Builder;
import kintsugi3d.builder.javafx.Modal;
import kintsugi3d.builder.javafx.core.RecentProjects;

import java.io.File;

public class ScreenshotController
{
    @FXML private Pane root;
    @FXML private TextField widthTextField;
    @FXML private TextField heightTextField;
    @FXML private TextField exportFileField;

    private final FileChooser fileChooser = new FileChooser();

    @FXML
    private void exportFileButtonAction()
    {
        this.fileChooser.setTitle("Choose an export file");
        if (exportFileField.getText().isEmpty())
        {
            this.fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
        }
        else
        {
            File currentValue = new File(exportFileField.getText());
            this.fileChooser.setInitialDirectory(currentValue);
        }
        File file = this.fileChooser.showSaveDialog(root.getScene().getWindow());
        if (file != null)
        {
            exportFileField.setText(file.toString());
            RecentProjects.setMostRecentDirectory(file);
        }
    }

    @FXML
    public void cancel()
    {
        Modal.requestClose(root);
    }

    @FXML
    public void run()
    {
        if(Global.state().getIOModel().getProgressMonitor().isConflictingProcess()){
            return;
        }

        Rendering.getRequestQueue().addGraphicsRequest(
            new Builder()
                .setWidth(Integer.parseInt(widthTextField.getText()))
                .setHeight(Integer.parseInt(heightTextField.getText()))
                .setExportFile(new File(exportFileField.getText()))
                .create());
    }
}
