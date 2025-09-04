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
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ObservableProjectGraphicsRequest;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.javafx.controllers.paged.NonDataPageControllerBase;
import kintsugi3d.builder.util.Kintsugi3DViewerLauncher;
import kintsugi3d.gl.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ExportModelController extends NonDataPageControllerBase
{
    private static final Logger LOG = LoggerFactory.getLogger(ExportModelController.class);

    //Initialize all the variables in the FXML file
    @FXML private Pane root;

    @FXML private ComboBox<String> formatComboBox;
    @FXML private CheckBox generateLowResolutionCheckBox;
    @FXML private CheckBox openViewerOnceCheckBox;
    @FXML private ComboBox<Integer> minimumTextureResolutionComboBox;

    private File exportLocationFile;
    private final FileChooser objFileChooser = new FileChooser();

    private ExportSettings settings;

    @Override
    public Region getRootNode()
    {
        return root;
    }

    @Override
    public void initPage()
    {
        settings = new ExportSettings();
        settings.setShouldSaveTextures(true);
        settings.setShouldAppendModelNameToTextures(true); // Give textures better filenames for export

        //Calls a function to set settings to defaults
        setAllVariables();

        objFileChooser.setTitle("Save project");
        objFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GLTF file", "*.glb"));
    }

    @Override
    public void refresh()
    {
    }

    public void run()
    {
        //Updates settings to equal what widget is displaying
        saveAllVariables();

        if (Global.state().getIOModel().getProgressMonitor().isConflictingProcess())
        {
            return;
        }

        try
        {
            exportLocationFile = objFileChooser.showSaveDialog(root.getScene().getWindow());
            if (exportLocationFile != null)
            {
                Rendering.getRequestQueue().addGraphicsRequest(new ObservableProjectGraphicsRequest()
                {
                    @Override
                    public <ContextType extends Context<ContextType>> void executeRequest(
                        ProjectInstance<ContextType> renderable, ProgressMonitor monitor)
                    {
                        if (settings.shouldSaveModel())
                        {
                            // Includes textures is shouldSaveTextures is true
                            renderable.saveGLTF(exportLocationFile.getParentFile(), exportLocationFile.getName(), settings,
                                () ->
                                {
                                    if (settings.shouldOpenViewerOnceComplete())
                                    {
                                        try
                                        {
                                            Kintsugi3DViewerLauncher.launchViewer(exportLocationFile);
                                        }
                                        catch (IOException e)
                                        {
                                            LOG.error("Error launching Kintsugi 3D Viewer", e);
                                        }

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
                                    }
                                });
                        }
                        else if (settings.shouldSaveTextures())
                        {
                            renderable.getResources().getSpecularMaterialResources()
                                .saveEssential(settings.getTextureFormat(), exportLocationFile.getParentFile());
                        }
                    }
                });
            }
        }
        catch (Exception ex)
        {
            LOG.error("Project didn't save correctly", ex);
        }
    }

    //Sets all the settings values on the widget to equal what they are currently
    public void setAllVariables()
    {
        generateLowResolutionCheckBox.setSelected(settings.shouldGenerateLowResTextures());
        minimumTextureResolutionComboBox.setItems(FXCollections.observableArrayList(256));
        minimumTextureResolutionComboBox.setValue(settings.getMinimumTextureResolution());
        openViewerOnceCheckBox.setSelected(settings.shouldOpenViewerOnceComplete());
    }

    //sets the settings to what the values are set on the widget
    public void saveAllVariables()
    {
        settings.setShouldGenerateLowResTextures(generateLowResolutionCheckBox.isSelected());
        settings.setMinimumTextureResolution(minimumTextureResolutionComboBox.getValue());
        settings.setShouldOpenViewerOnceComplete(openViewerOnceCheckBox.isSelected());
    }

    public void setCurrentDirectoryFile(File currentDirectoryFile)
    {
        // Sets FileChooser defaults
        if (currentDirectoryFile != null)
        {
            objFileChooser.setInitialDirectory(currentDirectoryFile);
            objFileChooser.setInitialFileName(currentDirectoryFile.getName());
        }
    }
}