package tetzlaff.ibr.javafx.controllers.scene.camera;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import tetzlaff.ibr.util.StaticHouse;
import tetzlaff.util.SafeNumberStringConverter;
import tetzlaff.util.SafeNumberStringConverterPow10;

public class SettingsCameraSceneController implements Initializable {

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
@FXML private TextField twistTextField;
@FXML private Slider twistSlider;
@FXML private TextField fOVTextField;
@FXML private Slider fOVSlider;
@FXML private TextField focalLengthTextField;
@FXML private Slider focalLengthSlider;

@FXML private CheckBox orthographicCheckBox;

@FXML private Button selectPointButton;



private DoubleProperty fov = new SimpleDoubleProperty();

private final SafeNumberStringConverter n = new SafeNumberStringConverter(0);
private final SafeNumberStringConverterPow10 n10 = new SafeNumberStringConverterPow10(1);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        StaticHouse.wrap(-180, 180, azimuthTextField);
        StaticHouse.bound(-90, 90, inclinationTextField);
        StaticHouse.wrap(-180, 180, twistTextField);

        StaticHouse.cleanInput(xCenterTextField);
        StaticHouse.cleanInput(yCenterTextField);
        StaticHouse.cleanInput(zCenterTextField);

        StaticHouse.cleanInput(fOVTextField);
        StaticHouse.cleanInput(focalLengthTextField);



        distanceSlider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                String out = n10.toString(object);
                if(out.length() >=4 ) return out.substring(0, 4);
                else return out;
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        });

    }

    public final ChangeListener<CameraSetting> changeListener =
            (observable, oldValue, newValue) -> {
            if(oldValue != null) unbind(oldValue);

            if(newValue != null) {bind(newValue);setDisabled(newValue.isLocked());}
            if(newValue == null) setDisabled(true);
    };

    public void setDisabled(Boolean disabled){
        root.setDisable(disabled);
    }

    private void bind(CameraSetting c) {

        xCenterTextField.textProperty().bindBidirectional(c.xCenterProperty(), n);
        yCenterTextField.textProperty().bindBidirectional(c.yCenterProperty(), n);
        zCenterTextField.textProperty().bindBidirectional(c.zCenterProperty(), n);
        azimuthTextField.textProperty().bindBidirectional(c.azimuthProperty(), n);
        inclinationTextField.textProperty().bindBidirectional(c.inclinationProperty(), n);
        distanceTextField.textProperty().bindBidirectional(c.log10distanceProperty(), n10);
        twistTextField.textProperty().bindBidirectional(c.twistProperty(), n);
        fOVTextField.textProperty().bindBidirectional(c.fOVProperty(), n);
        focalLengthTextField.textProperty().bindBidirectional(c.focalLengthProperty(), n);
        
        xCenterSlider.valueProperty().bindBidirectional(c.xCenterProperty());
        yCenterSlider.valueProperty().bindBidirectional(c.yCenterProperty());
        zCenterSlider.valueProperty().bindBidirectional(c.zCenterProperty());
        azimuthSlider.valueProperty().bindBidirectional(c.azimuthProperty());
        inclinationSlider.valueProperty().bindBidirectional(c.inclinationProperty());
        distanceSlider.valueProperty().bindBidirectional(c.log10distanceProperty());
        twistSlider.valueProperty().bindBidirectional(c.twistProperty());
        //fOVSlider.valueProperty().bindBidirectional(c.fOVProperty());
        focalLengthSlider.valueProperty().bindBidirectional(c.focalLengthProperty());

        orthographicCheckBox.selectedProperty().bindBidirectional(c.orthographicProperty());

        fov.bindBidirectional(c.fOVProperty());

    }

    private void unbind(CameraSetting c) {

        xCenterTextField.textProperty().unbindBidirectional(c.xCenterProperty());
        yCenterTextField.textProperty().unbindBidirectional(c.yCenterProperty());
        zCenterTextField.textProperty().unbindBidirectional(c.zCenterProperty());
        azimuthTextField.textProperty().unbindBidirectional(c.azimuthProperty());
        inclinationTextField.textProperty().unbindBidirectional(c.inclinationProperty());
        distanceTextField.textProperty().unbindBidirectional(c.log10distanceProperty());
        twistTextField.textProperty().unbindBidirectional(c.twistProperty());
        fOVTextField.textProperty().unbindBidirectional(c.fOVProperty());
        focalLengthTextField.textProperty().unbindBidirectional(c.focalLengthProperty());

        xCenterSlider.valueProperty().unbindBidirectional(c.xCenterProperty());
        yCenterSlider.valueProperty().unbindBidirectional(c.yCenterProperty());
        zCenterSlider.valueProperty().unbindBidirectional(c.zCenterProperty());
        azimuthSlider.valueProperty().unbindBidirectional(c.azimuthProperty());
        inclinationSlider.valueProperty().unbindBidirectional(c.inclinationProperty());
        distanceSlider.valueProperty().unbindBidirectional(c.log10distanceProperty());
        twistSlider.valueProperty().unbindBidirectional(c.twistProperty());
        //fOVSlider.valueProperty().unbindBidirectional(c.fOVProperty());
        focalLengthSlider.valueProperty().unbindBidirectional(c.focalLengthProperty());

        orthographicCheckBox.selectedProperty().unbindBidirectional(c.orthographicProperty());

        fov.bindBidirectional(c.fOVProperty());
    }

    public void setOnActionSelectPoint(EventHandler<ActionEvent> actionEventEventHandler){
        selectPointButton.setOnAction(actionEventEventHandler);
    }


}
