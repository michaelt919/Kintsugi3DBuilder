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
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.javafx.Modal;
import kintsugi3d.builder.javafx.core.RecentProjects;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class GenericRenderRequestController implements Initializable
{
    @FXML private Pane root;
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

    private final FileChooser fileChooser = new FileChooser();
    private final DirectoryChooser directoryChooser = new DirectoryChooser();

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        loopModeComboBoxAction();
        vertexShaderModeComboBoxAction();
    }

    public void run()
    {
        if (Global.state().getIOModel().getProgressMonitor().isConflictingProcess())
        {
            return;
        }

        File fragmentShader = new File(fragmentShaderField.getText());
        File outputDirectory = new File(exportDirectoryField.getText());

        RequestFactory requestFactory = RequestFactoryImplementation.create();

        RenderRequestBuilder builder;
        switch (loopModeComboBox.getValue())
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

        switch (vertexShaderModeComboBox.getValue())
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

        Rendering.getRequestQueue().addGraphicsRequest(builder.create());
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
        File file = this.fileChooser.showOpenDialog(root.getScene().getWindow());
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
        File file = this.fileChooser.showOpenDialog(root.getScene().getWindow());
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
        File file = this.fileChooser.showOpenDialog(root.getScene().getWindow());
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
        File file = this.directoryChooser.showDialog(root.getScene().getWindow());
        if (file != null)
        {
            exportDirectoryField.setText(file.toString());
            RecentProjects.setMostRecentDirectory(file.getParentFile());
        }
    }

    @FXML
    private void cancel()
    {
        Modal.requestClose(root);
    }
}
