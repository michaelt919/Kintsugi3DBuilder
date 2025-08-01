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

package kintsugi3d.builder.javafx.controllers.scene.object;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.CurrentProjectInputSource;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPage;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageScrollerController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;
import kintsugi3d.builder.javafx.util.SafeLogScaleNumberStringConverter;
import kintsugi3d.builder.javafx.util.SafeNumberStringConverter;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class SettingsObjectSceneController implements Initializable
{
    private static final Logger log = LoggerFactory.getLogger(SettingsObjectSceneController.class);


    @FXML private VBox root;

    @FXML private Button updateOrientationViewButton;

    @FXML private TextField xCenterTextField;
    @FXML private TextField yCenterTextField;
    @FXML private TextField zCenterTextField;
    @FXML private TextField scaleTxtField;


    @FXML private Slider xCenterSlider;
    @FXML private Slider yCenterSlider;
    @FXML private Slider zCenterSlider;
    @FXML private Slider scaleSlider;

    @FXML private TextField rotateYTextField;
    @FXML private Slider rotateYSlider;
    @FXML private TextField rotateXTextField;
    @FXML private Slider rotateXSlider;
    @FXML private TextField rotateZTextField;
    @FXML private Slider rotateZSlider;
    @FXML private Button selectPointButton;

    private final SafeLogScaleNumberStringConverter log10converter = new SafeLogScaleNumberStringConverter(1);

    private final SafeNumberStringConverter converter = new SafeNumberStringConverter(0);

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        StaticUtilities.makeWrapAroundNumeric(-180, 180, rotateYTextField);
        StaticUtilities.makeClampedNumeric(-90, 90, rotateXTextField);
        StaticUtilities.makeWrapAroundNumeric(-180, 180, rotateZTextField);

        StaticUtilities.makeNumeric(scaleTxtField);
        StaticUtilities.makeNumeric(xCenterTextField);
        StaticUtilities.makeNumeric(yCenterTextField);
        StaticUtilities.makeNumeric(zCenterTextField);

        scaleSlider.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double object) {
                String out = log10converter.toString(object);
                if (out.length() >= 4) {
                    return out.substring(0, 4);
                } else {
                    return out;
                }
            }

            @Override
            public Double fromString(String string) {
                throw new UnsupportedOperationException();
            }
        });
    }

    public final ChangeListener<ObjectPoseSetting> changeListener =
        (observable, oldValue, newValue) ->
        {
            if (oldValue != null)
            {
                unbind(oldValue);
            }

            if (newValue != null)
            {
                bind(newValue);
                setDisabled(newValue.isLocked());
            }
            if (newValue == null)
            {
                setDisabled(true);
            }
        };

    public void setDisabled(Boolean disabled)
    {
        root.setDisable(disabled);
    }

    public void bind(ObjectPoseSetting objectPose)
    {
        xCenterTextField.textProperty().bindBidirectional(objectPose.centerXProperty(), converter);
        yCenterTextField.textProperty().bindBidirectional(objectPose.centerYProperty(), converter);
        zCenterTextField.textProperty().bindBidirectional(objectPose.centerZProperty(), converter);
        rotateYTextField.textProperty().bindBidirectional(objectPose.rotateYProperty(), converter);
        rotateXTextField.textProperty().bindBidirectional(objectPose.rotateXProperty(), converter);
        rotateZTextField.textProperty().bindBidirectional(objectPose.rotateZProperty(), converter);
        scaleTxtField.textProperty().bindBidirectional(objectPose.scaleProperty(), log10converter);

        //default value must be set here because it is overwritten when the slider is converted to logarithmic format
        scaleTxtField.textProperty().setValue("1.0");

        xCenterSlider.valueProperty().bindBidirectional(objectPose.centerXProperty());
        yCenterSlider.valueProperty().bindBidirectional(objectPose.centerYProperty());
        zCenterSlider.valueProperty().bindBidirectional(objectPose.centerZProperty());
        rotateYSlider.valueProperty().bindBidirectional(objectPose.rotateYProperty());
        rotateXSlider.valueProperty().bindBidirectional(objectPose.rotateXProperty());
        rotateZSlider.valueProperty().bindBidirectional(objectPose.rotateZProperty());
        scaleSlider.valueProperty().bindBidirectional(objectPose.scaleProperty());
    }

    public void unbind(ObjectPoseSetting objectPose)
    {

        xCenterTextField.textProperty().unbindBidirectional(objectPose.centerXProperty());
        yCenterTextField.textProperty().unbindBidirectional(objectPose.centerYProperty());
        zCenterTextField.textProperty().unbindBidirectional(objectPose.centerZProperty());
        rotateYTextField.textProperty().unbindBidirectional(objectPose.rotateYProperty());
        rotateXTextField.textProperty().unbindBidirectional(objectPose.rotateXProperty());
        rotateZTextField.textProperty().unbindBidirectional(objectPose.rotateZProperty());
        scaleTxtField.textProperty().unbindBidirectional(objectPose.scaleProperty());

        xCenterSlider.valueProperty().unbindBidirectional(objectPose.centerXProperty());
        yCenterSlider.valueProperty().unbindBidirectional(objectPose.centerYProperty());
        zCenterSlider.valueProperty().unbindBidirectional(objectPose.centerZProperty());
        rotateYSlider.valueProperty().unbindBidirectional(objectPose.rotateYProperty());
        rotateXSlider.valueProperty().unbindBidirectional(objectPose.rotateXProperty());
        rotateZSlider.valueProperty().unbindBidirectional(objectPose.rotateZProperty());
        scaleSlider.valueProperty().unbindBidirectional(objectPose.scaleProperty());
    }

    public void setOnActionSelectPoint(EventHandler<ActionEvent> actionEventEventHandler)
    {
        selectPointButton.setOnAction(actionEventEventHandler);
    }

    public void onUpdateOrientationView(ActionEvent actionEvent)
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/menubar/FXMLPageScroller.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
            stage.setTitle("Select Orientation Reference View");
            stage.setScene(new Scene(root));

            FXMLPageScrollerController scrollerController = loader.getController();

            String viewSelectPath = "/fxml/menubar/createnewproject/PrimaryViewSelect.fxml";
            FXMLLoader selectorLoader = new FXMLLoader(getClass().getResource(viewSelectPath));
            selectorLoader.load();

            ArrayList<FXMLPage> pages = new ArrayList<>();
            pages.add(new FXMLPage(viewSelectPath, selectorLoader));

            scrollerController.setPages(pages, viewSelectPath);
            scrollerController.addInfo(ShareInfo.Info.INPUT_SOURCE, new CurrentProjectInputSource());
            scrollerController.init();

            stage.show();
        }
        catch (Exception e)
        {
            log.error("Unable to open orientation view selector.", e);
        }
    }
}
