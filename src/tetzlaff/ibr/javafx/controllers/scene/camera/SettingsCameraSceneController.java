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
import tetzlaff.ibr.javafx.util.SafeNumberStringConverter;
import tetzlaff.ibr.javafx.util.SafeNumberStringConverterPow10;
import tetzlaff.ibr.javafx.util.StaticUtilities;

public class SettingsCameraSceneController implements Initializable
{

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
    @FXML private TextField fovTextField;
    @FXML private Slider fovSlider;
    @FXML private TextField focalLengthTextField;
    @FXML private Slider focalLengthSlider;

    @FXML private CheckBox orthographicCheckBox;

    @FXML private Button selectPointButton;

    private final DoubleProperty fov = new SimpleDoubleProperty();
    private final DoubleProperty focalLength = new SimpleDoubleProperty();

    private final SafeNumberStringConverter n = new SafeNumberStringConverter(0);
    private final SafeNumberStringConverterPow10 n10 = new SafeNumberStringConverterPow10(1);

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        StaticUtilities.wrap(-180, 180, azimuthTextField);
        StaticUtilities.bound(-90, 90, inclinationTextField);
        StaticUtilities.wrap(-180, 180, twistTextField);

        StaticUtilities.cleanInput(xCenterTextField);
        StaticUtilities.cleanInput(yCenterTextField);
        StaticUtilities.cleanInput(zCenterTextField);

        StaticUtilities.cleanInput(fovTextField);
        StaticUtilities.cleanInput(focalLengthTextField);

        fov.addListener(change -> focalLength.setValue(18 / Math.tan(fov.getValue() * Math.PI / 360 /* convert and divide by 2 */)));
        focalLength.addListener(change -> fov.setValue(360 / Math.PI /* convert and multiply by 2) */ * Math.atan(18 / focalLength.getValue())));

        distanceSlider.setLabelFormatter(new StringConverter<Double>()
        {
            @Override
            public String toString(Double object)
            {
                String out = n10.toString(object);
                if (out.length() >= 4)
                {
                    return out.substring(0, 4);
                }
                else
                {
                    return out;
                }
            }

            @Override
            public Double fromString(String string)
            {
                return null;
            }
        });
    }

    public final ChangeListener<CameraSetting> changeListener =
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

    private void bind(CameraSetting camera)
    {

        xCenterTextField.textProperty().bindBidirectional(camera.xCenterProperty(), n);
        yCenterTextField.textProperty().bindBidirectional(camera.yCenterProperty(), n);
        zCenterTextField.textProperty().bindBidirectional(camera.zCenterProperty(), n);
        azimuthTextField.textProperty().bindBidirectional(camera.azimuthProperty(), n);
        inclinationTextField.textProperty().bindBidirectional(camera.inclinationProperty(), n);
        distanceTextField.textProperty().bindBidirectional(camera.log10distanceProperty(), n10);
        twistTextField.textProperty().bindBidirectional(camera.twistProperty(), n);
        fovTextField.textProperty().bindBidirectional(camera.fovProperty(), n);
        focalLengthTextField.textProperty().bindBidirectional(camera.focalLengthProperty(), n);

        xCenterSlider.valueProperty().bindBidirectional(camera.xCenterProperty());
        yCenterSlider.valueProperty().bindBidirectional(camera.yCenterProperty());
        zCenterSlider.valueProperty().bindBidirectional(camera.zCenterProperty());
        azimuthSlider.valueProperty().bindBidirectional(camera.azimuthProperty());
        inclinationSlider.valueProperty().bindBidirectional(camera.inclinationProperty());
        distanceSlider.valueProperty().bindBidirectional(camera.log10distanceProperty());
        twistSlider.valueProperty().bindBidirectional(camera.twistProperty());
        fovSlider.valueProperty().bindBidirectional(camera.fovProperty());
        focalLengthSlider.valueProperty().bindBidirectional(camera.focalLengthProperty());

        orthographicCheckBox.selectedProperty().bindBidirectional(camera.orthographicProperty());

        fov.bindBidirectional(camera.fovProperty());
        focalLength.bindBidirectional(camera.focalLengthProperty());
    }

    private void unbind(CameraSetting camera)
    {

        xCenterTextField.textProperty().unbindBidirectional(camera.xCenterProperty());
        yCenterTextField.textProperty().unbindBidirectional(camera.yCenterProperty());
        zCenterTextField.textProperty().unbindBidirectional(camera.zCenterProperty());
        azimuthTextField.textProperty().unbindBidirectional(camera.azimuthProperty());
        inclinationTextField.textProperty().unbindBidirectional(camera.inclinationProperty());
        distanceTextField.textProperty().unbindBidirectional(camera.log10distanceProperty());
        twistTextField.textProperty().unbindBidirectional(camera.twistProperty());
        fovTextField.textProperty().unbindBidirectional(camera.fovProperty());
        focalLengthTextField.textProperty().unbindBidirectional(camera.focalLengthProperty());

        xCenterSlider.valueProperty().unbindBidirectional(camera.xCenterProperty());
        yCenterSlider.valueProperty().unbindBidirectional(camera.yCenterProperty());
        zCenterSlider.valueProperty().unbindBidirectional(camera.zCenterProperty());
        azimuthSlider.valueProperty().unbindBidirectional(camera.azimuthProperty());
        inclinationSlider.valueProperty().unbindBidirectional(camera.inclinationProperty());
        distanceSlider.valueProperty().unbindBidirectional(camera.log10distanceProperty());
        twistSlider.valueProperty().unbindBidirectional(camera.twistProperty());
        fovSlider.valueProperty().unbindBidirectional(camera.fovProperty());
        focalLengthSlider.valueProperty().unbindBidirectional(camera.focalLengthProperty());

        orthographicCheckBox.selectedProperty().unbindBidirectional(camera.orthographicProperty());

        fov.unbindBidirectional(camera.fovProperty());
        focalLength.unbindBidirectional(camera.focalLengthProperty());
    }

    public void setOnActionSelectPoint(EventHandler<ActionEvent> actionEventEventHandler)
    {
        selectPointButton.setOnAction(actionEventEventHandler);
    }
}
