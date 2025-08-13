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

package kintsugi3d.builder.javafx.controllers.menubar.systemsettings;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import kintsugi3d.builder.javafx.InternalModels;
import kintsugi3d.builder.javafx.controllers.scene.lights.LightInstanceSetting;
import kintsugi3d.builder.javafx.internal.LightInstanceModelImpl;
import kintsugi3d.builder.javafx.util.SafeNumberStringConverter;

import java.util.Collection;

public class PhotoLightingSettingsController implements SystemSettingsControllerBase{
    @FXML public CheckBox fresnelEffectCheckBox;
    @FXML public CheckBox shadowsCheckBox;
    @FXML public CheckBox phyMaskingCheckBox;
    @FXML public CheckBox relightingCheckBox;
    @FXML public CheckBox visibleLightWidgetsCheckBox;
//    @FXML public CheckBox enableLight1CheckBox;
//    @FXML public TextField perLight1IntensityTxtField;
//    @FXML public Slider perLight1IntensitySlider;
//    @FXML public CheckBox enableLight2CheckBox;
//    @FXML public TextField perLight2IntensityTxtField;
//    @FXML public Slider perLight2IntensitySlider;
//    @FXML public CheckBox enableLight3CheckBox;
//    @FXML public TextField perLight3IntensityTxtField;
//    @FXML public Slider perLight3IntensitySlider;
//    @FXML public CheckBox enableLight4CheckBox;
//    @FXML public TextField perLight4IntensityTxtField;
//    @FXML public Slider perLight4IntensitySlider;
//    @FXML public TextField ambientLightIntensityTxtField;
//    @FXML public Slider ambientLightIntensitySlider;
//    @FXML public ColorPicker light1ColorPicker;
//    @FXML public ColorPicker light2ColorPicker;
//    @FXML public ColorPicker light3ColorPicker;
//    @FXML public ColorPicker light4ColorPicker;
//    @FXML public Label perLight1IntensityLabel;
//    @FXML public Label perLight2IntensityLabel;
//    @FXML public Label perLight3IntensityLabel;
//    @FXML public Label perLight4IntensityLabel;
    private InternalModels internalModels;
    private final Property<LightInstanceSetting> light1 = new SimpleObjectProperty<>();
    private final Property<LightInstanceSetting> light2 = new SimpleObjectProperty<>();
    private final Property<LightInstanceSetting> light3 = new SimpleObjectProperty<>();
    private final Property<LightInstanceSetting> light4 = new SimpleObjectProperty<>();

    private final SafeNumberStringConverter numberStringConverter = new SafeNumberStringConverter(0);



    public void updateRelightingVisibility() {
//        Collection<Object> light1ControlItems = new ArrayList<>();
//        light1ControlItems.add(perLight1IntensitySlider);
//        light1ControlItems.add(perLight1IntensityTxtField);
//        light1ControlItems.add(perLight1IntensityLabel);
//        light1ControlItems.add(light1ColorPicker);
//        updateCheckboxVisibilities(enableLight1CheckBox, light1ControlItems);
//
//        Collection<Object> light2ControlItems = new ArrayList<>();
//        light2ControlItems.add(perLight2IntensitySlider);
//        light2ControlItems.add(perLight2IntensityTxtField);
//        light2ControlItems.add(perLight2IntensityLabel);
//        light2ControlItems.add(light2ColorPicker);
//        updateCheckboxVisibilities(enableLight2CheckBox, light2ControlItems);
//
//        Collection<Object> light3ControlItems = new ArrayList<>();
//        light3ControlItems.add(perLight3IntensitySlider);
//        light3ControlItems.add(perLight3IntensityTxtField);
//        light3ControlItems.add(perLight3IntensityLabel);
//        light3ControlItems.add(light3ColorPicker);
//        updateCheckboxVisibilities(enableLight3CheckBox, light3ControlItems);
//
//        Collection<Object> light4ControlItems = new ArrayList<>();
//        light4ControlItems.add(perLight4IntensitySlider);
//        light4ControlItems.add(perLight4IntensityTxtField);
//        light4ControlItems.add(perLight4IntensityLabel);
//        light4ControlItems.add(light4ColorPicker);
//        updateCheckboxVisibilities(enableLight4CheckBox, light4ControlItems);
    }

    @Override
    public void init() {
        updateRelightingVisibility();
    }

    @Override
    public void bindInfo(InternalModels internalModels) {
        this.internalModels = internalModels;

        fresnelEffectCheckBox.selectedProperty().bindBidirectional(
                internalModels.getSettingsModel().getBooleanProperty("fresnelEnabled"));
        phyMaskingCheckBox.selectedProperty().bindBidirectional(
                internalModels.getSettingsModel().getBooleanProperty("pbrGeometricAttenuationEnabled"));
        shadowsCheckBox.selectedProperty().bindBidirectional(
                internalModels.getSettingsModel().getBooleanProperty("shadowsEnabled"));
        relightingCheckBox.selectedProperty().bindBidirectional(
                internalModels.getSettingsModel().getBooleanProperty("relightingEnabled"));
        visibleLightWidgetsCheckBox.selectedProperty().bindBidirectional(
                internalModels.getSettingsModel().getBooleanProperty("lightWidgetsEnabled"));


        //TODO: BIND PER-LIGHT INTENSITY AND AMBIENT LIGHT STUFF HERE
        LightInstanceModelImpl light = internalModels.getLightingModel().getLight(0);

        light1.addListener(changeListener);
        light2.addListener(changeListener);
        light3.addListener(changeListener);
        light4.addListener(changeListener);

//        LightInstanceSetting setting
//        xCenterSlider.valueProperty().bindBidirectional(setting.targetX());
    }

    public final ChangeListener<LightInstanceSetting> changeListener = (observable, oldValue, newValue) ->
    {
//        if (oldValue != null)
//        {
//            unbind(oldValue);
//        }
//
//        if (newValue != null)
//        {
//            bind(newValue);
//            setDisabled(newValue.locked().get() || newValue.isGroupLocked());
//        }
//        else
//        {
//            setDisabled(true);
//        }
    };

//    private void bind(LightInstanceSetting setting)
//    {
//
//        perLight1IntensitySlider.valueProperty().bindBidirectional(setting.intensity());
//        perLight1IntensityTxtField
//        light1ColorPicker
//
//        xCenterTextField.textProperty().bindBidirectional(setting.targetX(), numberStringConverter);
//        yCenterTextField.textProperty().bindBidirectional(setting.targetY(), numberStringConverter);
//        zCenterTextField.textProperty().bindBidirectional(setting.targetZ(), numberStringConverter);
//        azimuthTextField.textProperty().bindBidirectional(setting.azimuth(), numberStringConverter);
//        inclinationTextField.textProperty().bindBidirectional(setting.inclination(), numberStringConverter);
//        distanceTextField.textProperty().bindBidirectional(setting.log10Distance(), logScaleNumberStringConverter);
//        intensityTextField.textProperty().bindBidirectional(setting.intensity(), numberStringConverter);
//        spotSizeTextField.textProperty().bindBidirectional(setting.spotSize(), numberStringConverter);
//        spotTaperTextField.textProperty().bindBidirectional(setting.spotTaper(), numberStringConverter);
//        xCenterSlider.valueProperty().bindBidirectional(setting.targetX());
//        yCenterSlider.valueProperty().bindBidirectional(setting.targetY());
//        zCenterSlider.valueProperty().bindBidirectional(setting.targetZ());
//        azimuthSlider.valueProperty().bindBidirectional(setting.azimuth());
//        inclinationSlider.valueProperty().bindBidirectional(setting.inclination());
//        distanceSlider.valueProperty().bindBidirectional(setting.log10Distance());
//        spotSizeSlider.valueProperty().bindBidirectional(setting.spotSize());
//        spotTaperSlider.valueProperty().bindBidirectional(setting.spotTaper());
//        trueIntensity.bindBidirectional(setting.intensity());
//        colorPicker.valueProperty().bindBidirectional(setting.color());
//    }

    //when a checkbox is disabled, also disable the controls it is associated with (text fields, color pickers, etc.)
    private void updateCheckboxVisibilities(CheckBox checkBox, Collection<Object> items){
        boolean isChecked = checkBox.isSelected();

        for(Object item : items){
            if (item instanceof javafx.scene.control.Control){
                Control convertedControlItem = (Control) item;
                convertedControlItem.setDisable(!isChecked);
            }
            else if (item instanceof MenuItem){
                MenuItem convertedMenuItem = (MenuItem) item;
                convertedMenuItem.setDisable(!isChecked);
            }
        }
    }
}
