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

package kintsugi3d.builder.javafx.controllers.modals.workflow;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.io.ExportTexturesRequest;
import kintsugi3d.builder.javafx.util.SquareResolution;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ExportModelController extends DeferredProjectSettingsControllerBase
{
    private static final Logger LOG = LoggerFactory.getLogger(ExportModelController.class);

    //Initialize all the variables in the FXML file
    @FXML private Pane root;

    @FXML private ComboBox<String> formatComboBox;
    @FXML private CheckBox generateLowResolutionCheckBox;
    @FXML private CheckBox openViewerOnceCheckBox;
    @FXML private ComboBox<SquareResolution> minimumTextureResolutionComboBox;

    private File exportLocationFile;
    private final FileChooser objFileChooser = new FileChooser();

    @Override
    public Region getRootNode()
    {
        return root;
    }

    @Override
    public void initPage()
    {
        StaticUtilities.makeSquareResolutionComboBox(minimumTextureResolutionComboBox);

        objFileChooser.setTitle("Save project");
        objFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GLTF file", "*.glb"));

        // Enable min. texture resolution combo box when LODs are enabled.
        minimumTextureResolutionComboBox.disableProperty()
            .bind(generateLowResolutionCheckBox.selectedProperty().not());

        // Bind settings
        getLocalSettingsModel().bindTextComboBox(formatComboBox, "textureFormat");
        getLocalSettingsModel().bindBooleanSetting(generateLowResolutionCheckBox, "exportLODEnabled");
        getLocalSettingsModel().bindNumericComboBox(minimumTextureResolutionComboBox, "minimumLODSize",
            SquareResolution::new, SquareResolution::getSize);
        getLocalSettingsModel().bindBooleanSetting(openViewerOnceCheckBox, "openViewerOnExportComplete");

        File loadedProjectFile = Global.state().getIOModel().validateHandler().getLoadedProjectFile();
        if (loadedProjectFile != null)
        {
            setCurrentDirectoryFile(loadedProjectFile.getParentFile());
        }

        setCanAdvance(true);
        setCanConfirm(true);
    }

    @Override
    public boolean confirm()
    {
        if (!applySettings())
        {
            return false;
        }

        if (Global.state().getIOModel().getProgressMonitor().isConflictingProcess())
        {
            error("Failed to export model", "Another process is already running.");
            return false;
        }

        try
        {
            exportLocationFile = objFileChooser.showSaveDialog(root.getScene().getWindow());
            if (exportLocationFile != null)
            {
                Rendering.getRequestQueue().addGraphicsRequest(new ExportTexturesRequest(
                    exportLocationFile,
                    () ->
                    {
                        // Display message when all textures have been saved on graphics thread.
                        //TODO: MAKE PRETTIER, LOOK INTO NULL SAFETY
                        Platform.runLater(() ->
                        {
                            Dialog<ButtonType> saveInfo = new Alert(Alert.AlertType.INFORMATION,
                                "Export Complete!");
                            saveInfo.setTitle("Export successful");
                            saveInfo.setHeaderText(exportLocationFile.getName());
                            saveInfo.show();
                        });
                    }));

                return true;
            }
            else
            {
                return false;
            }
        }
        catch (RuntimeException ex)
        {
            LOG.error("Failed to save project", ex);
            error("Failed to save project", "An unknown error occurred.");
            return false;
        }
    }

    private void setCurrentDirectoryFile(File currentDirectoryFile)
    {
        // Sets FileChooser defaults
        if (currentDirectoryFile != null)
        {
            objFileChooser.setInitialDirectory(currentDirectoryFile);
            objFileChooser.setInitialFileName(currentDirectoryFile.getName());
        }
    }
}