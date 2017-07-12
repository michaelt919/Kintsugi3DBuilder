package tetzlaff.ibr.gui2.controllers.scene.camera;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;


import java.net.URL;
import java.text.NumberFormat;
import java.util.ResourceBundle;

public class SettingsCameraSceneController {

    @FXML
    private TextField azimuthBox;
    @FXML
    private Slider azimuthSlider;


    public ChangeListener<CameraSetting> getCameraSettingChangeListener() {
        return cameraSettingChangeListener;
    }

    private ChangeListener<CameraSetting> cameraSettingChangeListener = new ChangeListener<CameraSetting>() {
        @Override
        public void changed(ObservableValue<? extends CameraSetting> observable, CameraSetting oldValue, CameraSetting newValue) {
            //System.out.println("Change detected re-linking values");

            //relink values
            if (oldValue != null) {
                azimuthSlider.valueProperty().unbindBidirectional(oldValue.azimuthProperty());

                azimuthBox.textProperty().unbindBidirectional(oldValue.azimuthProperty());

            }

            StringConverter<Double> dts = new StringConverter<Double>() {
                @Override
                public String toString(Double object) {
                    return object.toString();
                }

                @Override
                public Double fromString(String string) {
                    return Double.valueOf(string);
                }
            };

            System.out.println("BIND");
            assert newValue != null : "the the camera in the list got deselected, I didn't know that could happen!";

            azimuthSlider.valueProperty().bindBidirectional(newValue.azimuthProperty());

            azimuthBox.textProperty().bindBidirectional(newValue.azimuthProperty(), NumberFormat.getNumberInstance());
        }
    };




}
