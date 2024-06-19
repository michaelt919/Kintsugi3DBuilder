/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.export.projectExporter;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.fit.settings.ExportSettings;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import kintsugi3d.builder.util.Kintsugi3DViewerLauncher;
import kintsugi3d.gl.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportRequestUI implements IBRRequestUI {

    private static final Logger log = LoggerFactory.getLogger(ExportRequestUI.class);

    //Initialize all the variables in the FXML file
    @FXML public Kintsugi3DBuilderState modelAccess;
    @FXML private Stage stage;
    @FXML private Button runButton;
//    @FXML private CheckBox combineWeightsCheckBox;
    @FXML private CheckBox generateLowResolutionCheckBox;
//    @FXML private CheckBox glTFEnabledCheckBox;
    @FXML private CheckBox glTFPackTexturesCheckBox;
    @FXML private CheckBox openViewerOnceCheckBox;
    @FXML private ComboBox<Integer> minimumTextureResolutionComboBox;
    public File CurrentDirectoryFile;
    public File ExportLocationFile;
    private final FileChooser objFileChooser = new FileChooser();


    public static ExportRequestUI create(Window window, Kintsugi3DBuilderState modelAccess) throws IOException {
        String fxmlFileName = "fxml/export/ExportRequestUI.fxml";
        URL url = ExportRequestUI.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;

        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        ExportRequestUI exportRequest = fxmlLoader.getController();
        exportRequest.modelAccess = modelAccess;

        if (modelAccess.getLoadingModel().getLoadedProjectFile() != null)
        {
            exportRequest.CurrentDirectoryFile = modelAccess.getLoadingModel().getLoadedProjectFile().getParentFile();
        }

        exportRequest.stage = new Stage();
        exportRequest.stage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        exportRequest.stage.setTitle("Export glTF");
        exportRequest.stage.setScene(new Scene(parent));
        exportRequest.stage.initOwner(window);
        return exportRequest;
    }

    @Override
    public <ContextType extends Context<ContextType>> void prompt(IBRRequestQueue<ContextType> requestQueue)
    {
        ExportSettings settings = new ExportSettings();

        stage.show();

        //Calls a function to set settings to defaults
        setAllVariables(settings);

        //Sets FileChooser defaults
        if (CurrentDirectoryFile != null)
        {
            objFileChooser.setInitialDirectory(CurrentDirectoryFile);
            objFileChooser.setInitialFileName(CurrentDirectoryFile.getName());
        }

        objFileChooser.setTitle("Save project");
        objFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GLTF file", "*.glb"));

        //Just sets the values in settings doesn't do anything else yet
        runButton.setOnAction(event ->
        {
            //Updates settings to equal what widget is displaying
            saveAllVariables(settings);

            try
            {
                ExportLocationFile = objFileChooser.showSaveDialog(stage);
                if (ExportLocationFile != null)
                {
                    requestQueue.addIBRRequest(new ObservableIBRRequest()
                    {
                        @Override
                        public <ContextType extends Context<ContextType>> void executeRequest(
                            IBRInstance<ContextType> renderable, LoadingMonitor callback) throws IOException
                        {
                            if (settings.isGlTFEnabled())
                            {
                                    renderable.saveGlTF(ExportLocationFile.getParentFile(), ExportLocationFile.getName(), settings);
                                    modelAccess.getLoadingModel().saveMaterialFiles(ExportLocationFile.getParentFile(), null);
                                }

                            if (settings.isOpenViewerOnceComplete())
                            {
                                    Kintsugi3DViewerLauncher.launchViewer(ExportLocationFile);
                                }
                            }
                    });
                }
            }
            catch (Exception ex)
            {
                log.error("Project didn't save correctly", ex);
            }
        });
    }

    @FXML //closes the stage
    public void cancelButtonAction() {
        stage.close();
    }

    //Sets all the settings values on the widget to equal what they are currently
    public void setAllVariables(ExportSettings settings){
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
    public void saveAllVariables(ExportSettings settings){
//        settings.setCombineWeights(combineWeightsCheckBox.isSelected());
        settings.setGenerateLowResTextures(generateLowResolutionCheckBox.isSelected());
//        settings.setGlTFEnabled(glTFEnabledCheckBox.isSelected());
        settings.setGlTFPackTextures(glTFPackTexturesCheckBox.isSelected());
        settings.setOpenViewerOnceComplete(openViewerOnceCheckBox.isSelected());
        settings.setMinimumTextureResolution(minimumTextureResolutionComboBox.getValue());
        System.out.println(minimumTextureResolutionComboBox.getValue());

    }
}