package tetzlaff.ibr.gui2.controllers.scene.environment_map;


import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.ColorPicker;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.URL;
import java.text.NumberFormat;
import java.util.ResourceBundle;

public class SettingsEVSceneController {
// Boolean evUseImage
// Boolean evUseColor
// Boolean bpUseImage
// Boolean bpUseColor
// Boolean imagePathsRelative
// String evImagePath
// String bpImagePath
// Double evColorIntensity
// Double evRotation
// Color evColor
// Color bpColor
// String name

    @FXML VBox root;

    @FXML CheckBox evUseImageCheckBox;
    @FXML CheckBox evUseColorCheckBox;
    @FXML CheckBox bpUseImageCheckBox;
    @FXML CheckBox bpUseColorCheckBox;
    @FXML TextField evColorIntensityTextField;
    @FXML Slider evColorIntensitySlider;
    @FXML TextField evRotationTextField;
    @FXML Slider evRotationSlider;
    @FXML ColorPicker evColorPicker;
    @FXML ColorPicker bpColorPicker;

    public ChangeListener<EVSetting> changeListener =
            (observable, oldValue, newValue) -> {
                if (oldValue != null) unbind(oldValue);

                if (newValue != null){ bind(newValue); setDisabled(newValue.isLocked());}
                else setDisabled(true);
            };


    public void setDisabled(boolean value){
        root.setDisable(value);
    }

    private void bind(EVSetting evSetting){
        NumberFormat n = NumberFormat.getNumberInstance();

        evUseImageCheckBox.selectedProperty().bindBidirectional(evSetting.evUseImageProperty());
        evUseColorCheckBox.selectedProperty().bindBidirectional(evSetting.evUseColorProperty());
        bpUseImageCheckBox.selectedProperty().bindBidirectional(evSetting.bpUseImageProperty());
        bpUseColorCheckBox.selectedProperty().bindBidirectional(evSetting.bpUseColorProperty());
        evColorIntensitySlider.valueProperty().bindBidirectional(evSetting.evColorIntensityProperty());
        evRotationSlider.valueProperty().bindBidirectional(evSetting.evRotationProperty());
        evColorIntensityTextField.textProperty().bindBidirectional(evSetting.evColorIntensityProperty(), n);
        evRotationTextField.textProperty().bindBidirectional(evSetting.evRotationProperty(), n);
        evColorPicker.valueProperty().bindBidirectional(evSetting.evColorProperty());
        bpColorPicker.valueProperty().bindBidirectional(evSetting.bpColorProperty());



    }

    private void unbind(EVSetting evSetting){

        evUseImageCheckBox.selectedProperty().unbindBidirectional(evSetting.evUseImageProperty());
        evUseColorCheckBox.selectedProperty().unbindBidirectional(evSetting.evUseColorProperty());
        bpUseImageCheckBox.selectedProperty().unbindBidirectional(evSetting.bpUseImageProperty());
        bpUseColorCheckBox.selectedProperty().unbindBidirectional(evSetting.bpUseColorProperty());
        evColorIntensitySlider.valueProperty().unbindBidirectional(evSetting.evColorIntensityProperty());
        evRotationSlider.valueProperty().unbindBidirectional(evSetting.evRotationProperty());
        evColorIntensityTextField.textProperty().unbindBidirectional(evSetting.evColorIntensityProperty());
        evRotationTextField.textProperty().unbindBidirectional(evSetting.evRotationProperty());
        evColorPicker.valueProperty().unbindBidirectional(evSetting.evColorProperty());
        bpColorPicker.valueProperty().unbindBidirectional(evSetting.bpColorProperty());

    }
}

