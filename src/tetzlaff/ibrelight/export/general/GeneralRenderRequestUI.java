/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.general;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;

import javafx.beans.property.Property;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.IBRRequestUI;
import tetzlaff.ibrelight.core.IBRelightModels;
import tetzlaff.ibrelight.javafx.internal.SettingsModelImpl;

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

    @FXML private TextField distance;
    @FXML private TextField Aperture;
    @FXML private TextField Focal;

    @FXML private Slider rangeSliderDistance;
    @FXML private Slider rangeSliderAperture;
    @FXML private Slider rangeSliderFocalLength;

    private final FileChooser fileChooser = new FileChooser();
    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    private File lastDirectory;

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
            e.printStackTrace();
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
    public static GeneralRenderRequestUI create(Window window, IBRelightModels modelAccess)
    {
        INSTANCE.stage = new Stage();
        INSTANCE.stage.setTitle("Generic export");

        try
        {
            INSTANCE.stage.getIcons().add(new Image(new File("ibr-icon.png").toURI().toURL().toString()));
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
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
    public <ContextType extends Context<ContextType>> void prompt(Consumer<IBRRequest<ContextType>> requestHandler)
    {
        stage.show();

        runButton.setOnAction(event ->
        {
//            stage.close();

            File fragmentShader = new File(fragmentShaderField.getText());
            File outputDirectory = new File(exportDirectoryField.getText());

            RenderRequestBuilder<ContextType> builder;
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
            if(!distance.getText().equals("0.0") && !Aperture.getText().equals("0.0") && !Focal.getText().equals("0.0"))
            {
                System.out.println("good");
            }
            else {
                System.out.println("You should give value to all the sliders");
            }

            builder.setWidth(Integer.parseInt(this.widthTextField.getText()));
            builder.setHeight(Integer.parseInt(this.heightTextField.getText()));
            builder.setShaderSetupCallback(program ->
            {
                program.setUniform("distance",(float)rangeSliderDistance.getValue());
                program.setUniform("aperture",(float)rangeSliderAperture.getValue());
                program.setUniform("focal",(float)rangeSliderFocalLength.getValue());

            });
            requestHandler.accept(builder.create());
        });
    }

    @Override
    public void bind(SettingsModelImpl injectedSettingsModel) {



        rangeSliderDistance.valueProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("distance"));
        rangeSliderAperture.valueProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("aperture"));
        rangeSliderFocalLength.valueProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("focal"));

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
            if (lastDirectory != null)
            {
                this.fileChooser.setInitialDirectory(lastDirectory);
            }
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
            lastDirectory = file.getParentFile();
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
            if (lastDirectory != null)
            {
                this.fileChooser.setInitialDirectory(lastDirectory);
            }
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
            lastDirectory = file.getParentFile();
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
            if (lastDirectory != null)
            {
                this.fileChooser.setInitialDirectory(lastDirectory);
            }
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
            lastDirectory = file.getParentFile();
        }
    }

    @FXML
    private void exportDirectoryButtonAction()
    {
        this.directoryChooser.setTitle("Choose an output directory");
        if (exportDirectoryField.getText().isEmpty() || !new File(exportDirectoryField.getText()).exists())
        {
            if (lastDirectory != null)
            {
                this.directoryChooser.setInitialDirectory(lastDirectory);
            }
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
            lastDirectory = file;
        }
    }


    @FXML
    private void cancelButtonAction()
    {
        stage.close();
    }
}
