package tetzlaff.ibr.gui2.controllers.scene.camera;

import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import tetzlaff.ibr.util.U;
import tetzlaff.util.SafeNumberStringConverter;


import java.net.URL;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

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


private DoubleProperty fov = new SimpleDoubleProperty();

private final SafeNumberStringConverter n = new SafeNumberStringConverter();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        U.wrap(-180, 180, azimuthTextField);
        U.bound(-90, 90, inclinationTextField);
        U.wrap(-180, 180, twistTextField);


        fOVSlider.setMin(-2);
        fOVSlider.setMax(2);
        fOVSlider.setValue(0);
        fOVSlider.setShowTickMarks(true);
        fOVSlider.setShowTickLabels(true);
        fOVSlider.setBlockIncrement(0.1);
        fOVSlider.setMajorTickUnit(1);
        fOVSlider.setMinorTickCount(1);
        fOVSlider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                return Double.toString(Math.pow(10, object));
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        });

        U.powerBind(fOVSlider.valueProperty(), fov);

//        fOVSlider.setMin(-100);
//        fOVSlider.setMax(100);
//        fOVSlider.setShowTickLabels(true);
//        fOVSlider.setShowTickMarks(true);
//        fOVSlider.setBlockIncrement(10);
//        fOVSlider.setMajorTickUnit(50);
//        fOVSlider.setMinorTickCount(4);
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
        distanceTextField.textProperty().bindBidirectional(c.distanceProperty(), n);
        twistTextField.textProperty().bindBidirectional(c.twistProperty(), n);
        fOVTextField.textProperty().bindBidirectional(c.fOVProperty(), n);
        focalLengthTextField.textProperty().bindBidirectional(c.focalLengthProperty(), n);
        
        xCenterSlider.valueProperty().bindBidirectional(c.xCenterProperty());
        yCenterSlider.valueProperty().bindBidirectional(c.yCenterProperty());
        zCenterSlider.valueProperty().bindBidirectional(c.zCenterProperty());
        azimuthSlider.valueProperty().bindBidirectional(c.azimuthProperty());
        inclinationSlider.valueProperty().bindBidirectional(c.inclinationProperty());
        distanceSlider.valueProperty().bindBidirectional(c.distanceProperty());
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
        distanceTextField.textProperty().unbindBidirectional(c.distanceProperty());
        twistTextField.textProperty().unbindBidirectional(c.twistProperty());
        fOVTextField.textProperty().unbindBidirectional(c.fOVProperty());
        focalLengthTextField.textProperty().unbindBidirectional(c.focalLengthProperty());

        xCenterSlider.valueProperty().unbindBidirectional(c.xCenterProperty());
        yCenterSlider.valueProperty().unbindBidirectional(c.yCenterProperty());
        zCenterSlider.valueProperty().unbindBidirectional(c.zCenterProperty());
        azimuthSlider.valueProperty().unbindBidirectional(c.azimuthProperty());
        inclinationSlider.valueProperty().unbindBidirectional(c.inclinationProperty());
        distanceSlider.valueProperty().unbindBidirectional(c.distanceProperty());
        twistSlider.valueProperty().unbindBidirectional(c.twistProperty());
        //fOVSlider.valueProperty().unbindBidirectional(c.fOVProperty());
        focalLengthSlider.valueProperty().unbindBidirectional(c.focalLengthProperty());

        orthographicCheckBox.selectedProperty().unbindBidirectional(c.orthographicProperty());

        fov.bindBidirectional(c.fOVProperty());
    }


@FXML
private void pressSelectPointButton(){
        //TODO
    System.out.println("TODO: point selected");
}


}
