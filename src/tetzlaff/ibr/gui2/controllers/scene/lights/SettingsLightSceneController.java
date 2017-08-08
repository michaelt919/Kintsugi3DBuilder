package tetzlaff.ibr.gui2.controllers.scene.lights;//Created by alexk on 7/16/2017.


import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import tetzlaff.ibr.util.StaticHouse;
import tetzlaff.util.SafeNumberStringConverter;
import tetzlaff.util.SafeNumberStringConverterPow10;

public class SettingsLightSceneController implements Initializable{
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setDisabled(true);

        distanceSlider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                String s = n10.toString(object);
                if(s.length() > 3) return s.substring(0,3);
                else return s;
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        });


        StaticHouse.cleanInput(xCenterTextField);
        StaticHouse.cleanInput(yCenterTextField);
        StaticHouse.cleanInput(zCenterTextField);

        StaticHouse.bound(0, Double.MAX_VALUE, distanceTextField);
        StaticHouse.bound(0, Double.MAX_VALUE, intensityTextField);


        StaticHouse.wrap(-180, 180, azimuthTextField);
        StaticHouse.bound(-90,90,inclinationTextField);

    }

    @FXML private VBox root;
    @FXML private TextField xCenterTextField;
    @FXML private TextField yCenterTextField;
    @FXML private TextField zCenterTextField;
    @FXML private Slider xCenterSlider;
    @FXML private Slider yCenterSlider;
    @FXML private Slider zCenterSlider;
    @FXML private TextField azimuthTextField;
    @FXML private Slider azimuthSlider;
    @FXML private TextField inclinationTextField;
    @FXML private Slider inclinationSlider;
    @FXML private TextField distanceTextField;
    @FXML private Slider distanceSlider;
    @FXML private TextField intensityTextField;
    @FXML private Slider intensitySlider;
    @FXML private ColorPicker colorPicker;
    @FXML private ChoiceBox<LightType> lightTypeChoiceBox;

    private final SafeNumberStringConverter n = new SafeNumberStringConverter(0);
    private final SafeNumberStringConverterPow10 n10 = new SafeNumberStringConverterPow10(1);

    public final ChangeListener<SubLightSetting> changeListener = (observable, oldValue, newValue) -> {
        if(oldValue != null) unbind(oldValue);

        if(newValue != null){ bind(newValue); setDisabled(newValue.isLocked() | newValue.getGroupLocked()); }
        else setDisabled(true);
    };


    public void setDisabled(Boolean disabled){
        root.setDisable(disabled);
    }

    private void bind(SubLightSetting c){

        xCenterTextField.textProperty().bindBidirectional(c.xCenterProperty(), n);
        yCenterTextField.textProperty().bindBidirectional(c.yCenterProperty(), n);
        zCenterTextField.textProperty().bindBidirectional(c.zCenterProperty(), n);
        azimuthTextField.textProperty().bindBidirectional(c.azimuthProperty(), n);
        inclinationTextField.textProperty().bindBidirectional(c.inclinationProperty(), n);
        distanceTextField.textProperty().bindBidirectional(c.log10distanceProperty(), n10);
        intensityTextField.textProperty().bindBidirectional(c.intensityProperty(), n);
        xCenterSlider.valueProperty().bindBidirectional(c.xCenterProperty());
        yCenterSlider.valueProperty().bindBidirectional(c.yCenterProperty());
        zCenterSlider.valueProperty().bindBidirectional(c.zCenterProperty());
        azimuthSlider.valueProperty().bindBidirectional(c.azimuthProperty());
        inclinationSlider.valueProperty().bindBidirectional(c.inclinationProperty());
        distanceSlider.valueProperty().bindBidirectional(c.log10distanceProperty());
        intensitySlider.valueProperty().bindBidirectional(c.intensityProperty());
        lightTypeChoiceBox.valueProperty().bindBidirectional(c.lightTypeProperty());

        colorPicker.valueProperty().bindBidirectional(c.colorProperty());

    }

    private void unbind(SubLightSetting c){

        xCenterTextField.textProperty().unbindBidirectional(c.xCenterProperty());
        yCenterTextField.textProperty().unbindBidirectional(c.yCenterProperty());
        zCenterTextField.textProperty().unbindBidirectional(c.zCenterProperty());
        azimuthTextField.textProperty().unbindBidirectional(c.azimuthProperty());
        inclinationTextField.textProperty().unbindBidirectional(c.inclinationProperty());
        distanceTextField.textProperty().unbindBidirectional(c.log10distanceProperty());
        intensityTextField.textProperty().unbindBidirectional(c.intensityProperty());
        xCenterSlider.valueProperty().unbindBidirectional(c.xCenterProperty());
        yCenterSlider.valueProperty().unbindBidirectional(c.yCenterProperty());
        zCenterSlider.valueProperty().unbindBidirectional(c.zCenterProperty());
        azimuthSlider.valueProperty().unbindBidirectional(c.azimuthProperty());
        inclinationSlider.valueProperty().unbindBidirectional(c.inclinationProperty());
        distanceSlider.valueProperty().unbindBidirectional(c.log10distanceProperty());
        intensitySlider.valueProperty().unbindBidirectional(c.intensityProperty());
        lightTypeChoiceBox.valueProperty().unbindBidirectional(c.lightTypeProperty());
        colorPicker.valueProperty().unbindBidirectional(c.colorProperty());

    }
}
