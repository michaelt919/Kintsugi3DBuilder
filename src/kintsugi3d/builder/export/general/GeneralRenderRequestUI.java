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

package kintsugi3d.builder.export.general;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import kintsugi3d.builder.core.IBRRequestQueue;
import kintsugi3d.builder.core.IBRRequestUI;
import kintsugi3d.builder.core.Kintsugi3DBuilderState;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.core.RecentProjects;
import kintsugi3d.gl.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class GeneralRenderRequestUI implements IBRRequestUI
{
    @FXML private ComboBox<LoopMode> loopModeComboBox;
    @FXML private Label outputImageLabel;
    @FXML private TextField outputImageName;
    @FXML private Label frameCountLabel;
    @FXML private TextField frameCount;
    @FXML private Label targetVSetFileLabel;
    @FXML private TextField targetVSetFileField;
    @FXML private Button targetVsetFileButton;
    @FXML private ComboBox<VertexShaderMode> vertexShaderModeComboBox;
    @FXML private Label customVertexShaderLabel;
    @FXML private TextField customVertexShaderField;
    @FXML private Button vertexShaderButton;
    @FXML private TextField fragmentShaderField;
    @FXML private TextField exportDirectoryField;
    @FXML private TextField widthTextField;
    @FXML private TextField heightTextField;
    @FXML private Button runButton;

    private static final Logger log = LoggerFactory.getLogger(GeneralRenderRequestUI.class);

    private final FileChooser fileChooser = new FileChooser();
    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    private Stage stage;

    private RequestFactory requestFactory;

    private static final Scene SCENE;
    private static final GeneralRenderRequestUI INSTANCE;

    static
    {
        FXMLLoader fxmlLoader = null;
        Parent parent = null;
        try
        {
            String fxmlFileName = "fxml/export/GeneralRenderRequestUI.fxml";
            URL url = GeneralRenderRequestUI.class.getClassLoader().getResource(fxmlFileName);
            assert url != null : "Can't find " + fxmlFileName;

            fxmlLoader = new FXMLLoader(url);
            parent = fxmlLoader.load();
        }
        catch(IOException e)
        {
            log.error("Error occurred loading fxml file:", e);
        }

        if (parent != null)
        {
            SCENE = new Scene(parent);
            INSTANCE = fxmlLoader.getController();
            INSTANCE.init();
        }
        else
        {
            SCENE = null;
            INSTANCE = null;
        }
    }

    // TODO rename this and other UI create() methods, also changing its usage in reflection code, to better reflect that UIs are basically singletons.
    public static GeneralRenderRequestUI create(Window window, Kintsugi3DBuilderState modelAccess)
    {
        INSTANCE.stage = new Stage();
        INSTANCE.stage.setTitle("Generic export");

        try
        {
            INSTANCE.stage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        }
        catch (MalformedURLException e)
        {
            log.error("Error loading window icon:", e);
        }

        INSTANCE.stage.setScene(SCENE);
        INSTANCE.stage.initOwner(window);
        INSTANCE.requestFactory = RequestFactoryImplementation.create(modelAccess.getSettingsModel());
        return INSTANCE;
    }

    private void init()
    {
        loopModeComboBoxAction();
        vertexShaderModeComboBoxAction();
    }

    @Override
    public <ContextType extends Context<ContextType>> void prompt(IBRRequestQueue<ContextType> requestQueue)
    {
        stage.show();

        runButton.setOnAction(event ->
        {
//            stage.close();

            if(MultithreadModels.getInstance().getIOModel().getProgressMonitor().isConflictingProcess()){
                return;
            }

            File fragmentShader = new File(fragmentShaderField.getText());
            File outputDirectory = new File(exportDirectoryField.getText());

            RenderRequestBuilder builder;
            switch(loopModeComboBox.getValue())
            {
                case SINGLE_FRAME:
                    builder = requestFactory.buildSingleFrameRenderRequest(fragmentShader, outputDirectory,
                        outputImageName.getText());
                    break;
                case MULTIFRAME:
                    builder = requestFactory.buildMultiframeRenderRequest(fragmentShader, outputDirectory,
                        Integer.parseInt(frameCount.getText()));
                    break;
                case MULTIVIEW:
                    builder = requestFactory.buildMultiviewRenderRequest(fragmentShader, outputDirectory);
                    break;
                case MULTIVIEW_RETARGET:
                    builder = requestFactory.buildMultiviewRetargetRenderRequest(fragmentShader, outputDirectory,
                        new File(targetVSetFileField.getText()));
                    break;
                default:
                    throw new IllegalStateException("Unexpected loop mode.");
            }

            switch(vertexShaderModeComboBox.getValue())
            {
                case TEXTURE_SPACE:
                    builder.useTextureSpaceVertexShader();
                    break;
                case CAMERA_SPACE:
                    builder.useCameraSpaceVertexShader();
                    break;
                case CUSTOM:
                    builder.useCustomVertexShader(new File(customVertexShaderField.getText()));
                    break;
                default:
                    throw new IllegalStateException("Unexpected vertex shader mode.");
            }

            builder.setWidth(Integer.parseInt(this.widthTextField.getText()));
            builder.setHeight(Integer.parseInt(this.heightTextField.getText()));

            requestQueue.addIBRRequest(builder.create());
        });
    }

    @FXML
    private void loopModeComboBoxAction()
    {
        LoopMode loopMode = loopModeComboBox.getValue();

        outputImageLabel.setVisible(loopMode == LoopMode.SINGLE_FRAME);
        outputImageName.setVisible(loopMode == LoopMode.SINGLE_FRAME);

        frameCountLabel.setVisible(loopMode == LoopMode.MULTIFRAME);
        frameCount.setVisible(loopMode == LoopMode.MULTIFRAME);

        targetVSetFileLabel.setVisible(loopMode == LoopMode.MULTIVIEW_RETARGET);
        targetVSetFileField.setVisible(loopMode == LoopMode.MULTIVIEW_RETARGET);
        targetVsetFileButton.setVisible(loopMode == LoopMode.MULTIVIEW_RETARGET);
    }

    @FXML
    private void vertexShaderModeComboBoxAction()
    {
        VertexShaderMode vertexShaderMode = vertexShaderModeComboBox.getValue();

        customVertexShaderLabel.setVisible(vertexShaderMode == VertexShaderMode.CUSTOM);
        customVertexShaderField.setVisible(vertexShaderMode == VertexShaderMode.CUSTOM);
        vertexShaderButton.setVisible(vertexShaderMode == VertexShaderMode.CUSTOM);
    }

    @FXML
    private void targetVSetFileButtonAction()
    {
        this.fileChooser.setTitle("Choose a target view set file");
        this.fileChooser.getExtensionFilters().clear();
        this.fileChooser.getExtensionFilters().add(new ExtensionFilter("View set files", "*.vset"));
        if (targetVSetFileField.getText().isEmpty() || !new File(targetVSetFileField.getText()).exists())
        {
            this.fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
        }
        else
        {
            File currentValue = new File(targetVSetFileField.getText());
            this.fileChooser.setInitialDirectory(currentValue.getParentFile());
            this.fileChooser.setInitialFileName(currentValue.getName());
        }
        File file = this.fileChooser.showOpenDialog(stage.getOwner());
        if (file != null)
        {
            targetVSetFileField.setText(file.toString());
            RecentProjects.setMostRecentDirectory(file.getParentFile());
        }
    }

    @FXML
    private void vertexShaderButtonAction()
    {
        this.fileChooser.setTitle("Choose a custom vertex shader");
        this.fileChooser.getExtensionFilters().clear();
        this.fileChooser.getExtensionFilters().add(new ExtensionFilter("GLSL vertex shaders", "*.*"));
        if (customVertexShaderField.getText().isEmpty() || !new File(customVertexShaderField.getText()).exists())
        {
            this.fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
        }
        else
        {
            File currentValue = new File(customVertexShaderField.getText());
            this.fileChooser.setInitialDirectory(currentValue.getParentFile());
            this.fileChooser.setInitialFileName(currentValue.getName());
        }
        File file = this.fileChooser.showOpenDialog(stage.getOwner());
        if (file != null)
        {
            customVertexShaderField.setText(file.toString());
            RecentProjects.setMostRecentDirectory(file.getParentFile());
        }
    }

    @FXML
    private void fragmentShaderButtonAction()
    {
        this.fileChooser.setTitle("Choose a fragment shader");
        this.fileChooser.getExtensionFilters().clear();
        this.fileChooser.getExtensionFilters().add(new ExtensionFilter("GLSL fragment shaders", "*.*"));
        if (fragmentShaderField.getText().isEmpty() || !new File(fragmentShaderField.getText()).exists())
        {
            this.fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
        }
        else
        {
            File currentValue = new File(fragmentShaderField.getText());
            this.fileChooser.setInitialDirectory(currentValue.getParentFile());
            this.fileChooser.setInitialFileName(currentValue.getName());
        }
        File file = this.fileChooser.showOpenDialog(stage.getOwner());
        if (file != null)
        {
            fragmentShaderField.setText(file.toString());
            RecentProjects.setMostRecentDirectory(file.getParentFile());
        }
    }

    @FXML
    private void exportDirectoryButtonAction()
    {
        this.directoryChooser.setTitle("Choose an output directory");
        if (exportDirectoryField.getText().isEmpty() || !new File(exportDirectoryField.getText()).exists())
        {
            this.directoryChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
        }
        else
        {
            File currentValue = new File(exportDirectoryField.getText());
            this.directoryChooser.setInitialDirectory(currentValue);
        }
        File file = this.directoryChooser.showDialog(stage.getOwner());
        if (file != null)
        {
            exportDirectoryField.setText(file.toString());
            RecentProjects.setMostRecentDirectory(file.getParentFile());
        }
    }

    @FXML
    private void cancelButtonAction()
    {
        stage.close();
    }
}
