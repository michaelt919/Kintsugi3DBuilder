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

package kintsugi3d.builder.javafx.controllers.modals;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ObservableProjectGraphicsRequest;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.javafx.experience.Modal;
import kintsugi3d.builder.util.Kintsugi3DViewerLauncher;
import kintsugi3d.gl.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ExportModelController implements Initializable
{
    private static final Logger LOG = LoggerFactory.getLogger(ExportModelController.class);

    //Initialize all the variables in the FXML file
    //    @FXML private CheckBox combineWeightsCheckBox;
    @FXML private Pane root;
    @FXML private CheckBox generateLowResolutionCheckBox;
    //    @FXML private CheckBox glTFEnabledCheckBox;
    @FXML private CheckBox glTFPackTexturesCheckBox;
    @FXML private CheckBox openViewerOnceCheckBox;
    @FXML private ComboBox<Integer> minimumTextureResolutionComboBox;

    private File exportLocationFile;
    private final FileChooser objFileChooser = new FileChooser();

    private ExportSettings settings;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        settings = new ExportSettings();

        //Calls a function to set settings to defaults
        setAllVariables();

        objFileChooser.setTitle("Save project");
        objFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GLTF file", "*.glb"));
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
                        ProjectInstance<ContextType> renderable, ProgressMonitor monitor) throws IOException
                    {
                        if (settings.isGlTFEnabled())
                        {
                            renderable.saveGlTF(exportLocationFile.getParentFile(), exportLocationFile.getName(), settings);
                            Global.state().getIOModel().saveEssentialMaterialFiles(exportLocationFile.getParentFile(), null);
                            // Skip standalone occlusion (which is often really a renamed ORM where we ignore the G & B channels)
                            // and unpacked weight maps for the user export as those are only intended to be used internally.
                        }

                        if (settings.isOpenViewerOnceComplete())
                        {
                            Kintsugi3DViewerLauncher.launchViewer(exportLocationFile);
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

    @FXML //closes the stage
    public void cancel()
    {
        Modal.requestClose(root);
    }

    //Sets all the settings values on the widget to equal what they are currently
    public void setAllVariables()
    {
//        combineWeightsCheckBox.setSelected(settings.isCombineWeights());
        generateLowResolutionCheckBox.setSelected(settings.isGenerateLowResTextures());
//        glTFEnabledCheckBox.setSelected(settings.isGlTFEnabled());
        glTFPackTexturesCheckBox.setSelected(settings.isGlTFPackTextures());
        openViewerOnceCheckBox.setSelected(settings.isOpenViewerOnceComplete());
        int getMinimumTexRes = settings.getMinimumTextureResolution();
        minimumTextureResolutionComboBox.setItems(FXCollections.observableArrayList(256));
        minimumTextureResolutionComboBox.setValue(getMinimumTexRes);
    }

    //sets the settings to what the values are set on the widget
    public void saveAllVariables()
    {
//        settings.setCombineWeights(combineWeightsCheckBox.isSelected());
        settings.setGenerateLowResTextures(generateLowResolutionCheckBox.isSelected());
//        settings.setGlTFEnabled(glTFEnabledCheckBox.isSelected());
        settings.setGlTFPackTextures(glTFPackTexturesCheckBox.isSelected());
        settings.setOpenViewerOnceComplete(openViewerOnceCheckBox.isSelected());
        settings.setMinimumTextureResolution(minimumTextureResolutionComboBox.getValue());
        System.out.println(minimumTextureResolutionComboBox.getValue());

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