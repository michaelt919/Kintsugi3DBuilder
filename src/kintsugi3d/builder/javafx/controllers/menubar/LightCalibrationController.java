package kintsugi3d.builder.javafx.controllers.menubar;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.javafx.internal.SettingsModelImpl;
import kintsugi3d.builder.javafx.util.SafeDecimalNumberStringConverter;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.gl.vecmath.Vector2;

public class LightCalibrationController implements Initializable
{
    @FXML private AnchorPane root;
    @FXML private TextField xTextField;
    @FXML private TextField yTextField;
    @FXML private Slider xSlider;
    @FXML private Slider ySlider;

    private ChangeListener<? super Number> xListener;
    private ChangeListener<? super Number> yListener;
    private ChangeListener<Vector2> settingsListener;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        StaticUtilities.makeNumeric(xTextField);
        StaticUtilities.makeNumeric(yTextField);
    }

    public void bind(SettingsModelImpl injectedSettingsModel)
    {
        // Bind sliders to settings model
        xListener = (observable, oldValue, newValue) ->
            injectedSettingsModel.set("currentLightCalibration",
                new Vector2(newValue.floatValue(), injectedSettingsModel.get("currentLightCalibration", Vector2.class).y));
        xSlider.valueProperty().addListener(xListener);

        yListener = (observable, oldValue, newValue) ->
            injectedSettingsModel.set("currentLightCalibration",
                new Vector2(injectedSettingsModel.get("currentLightCalibration", Vector2.class).x, newValue.floatValue()));
        ySlider.valueProperty().addListener(yListener);

        settingsListener = (observable, oldValue, newValue) ->
        {
            xSlider.setValue(newValue.x);
            ySlider.setValue(newValue.y);
        };

        injectedSettingsModel.getObjectProperty("currentLightCalibration", Vector2.class).addListener(settingsListener);

        SafeDecimalNumberStringConverter converter = new SafeDecimalNumberStringConverter(0.0f);

        // Bind text fields to sliders and ensure that the range adapts to values entered in text fields.
        xTextField.textProperty().addListener((observable, oldValue, newValue) ->
        {
            double x = converter.fromString(newValue);
            if (x > 5)
            {
                // Adjust bounds first
                xSlider.setMin(-x);
                xSlider.setMax(x);
                xSlider.setValue(x);
            }
            else if (x < -5)
            {
                // Adjust bounds first
                xSlider.setMin(x);
                xSlider.setMax(-x);
                xSlider.setValue(x);
            }
            else
            {
                // Adjust value first in case it was previously out of standard bounds
                xSlider.setValue(x);
                xSlider.setMin(-5);
                xSlider.setMax(5);
            }
        });

        yTextField.textProperty().addListener( (observable, oldValue, newValue) ->
        {
            double y = converter.fromString(newValue);

            if (y > 5)
            {
                // Adjust bounds first
                ySlider.setMin(-y);
                ySlider.setMax(y);
                ySlider.setValue(y);
            }
            else if (y < -5)
            {
                // Adjust bounds first
                ySlider.setMin(y);
                ySlider.setMax(-y);
                ySlider.setValue(y);
            }
            else
            {
                // Adjust value first in case it was previously out of standard bounds
                ySlider.setValue(y);
                ySlider.setMin(-5);
                ySlider.setMax(5);
            }

        });

        xSlider.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            // Compare slider and textbox after both have been reconverted to a standardized string.
            // If they are equivalent, don't change the textbox.
            if (!converter.toString(newValue).equals(converter.toString(converter.fromString(xTextField.getText()))))
            {
                xTextField.setText(converter.toString(newValue));
            }
        });
        ySlider.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            // Compare slider and textbox after both have been reconverted to a standardized string.
            // If they are equivalent, don't change the textbox.
            if (!converter.toString(newValue).equals(converter.toString(converter.fromString(yTextField.getText()))))
            {
                yTextField.setText(converter.toString(newValue));
            }
        });

        // Set initial value to start out synchronized.
        Vector2 originalLightCalibration = injectedSettingsModel.get("currentLightCalibration", Vector2.class);

        // Set initial bounds based on original calibration.
        float xMax = Math.max(5, Math.max(originalLightCalibration.x, -originalLightCalibration.x));
        float yMax = Math.max(5, Math.max(originalLightCalibration.y, -originalLightCalibration.y));

        xSlider.setMax(xMax);
        xSlider.setMin(-xMax);
        xSlider.setValue(originalLightCalibration.x);

        ySlider.setMax(yMax);
        ySlider.setMin(-yMax);
        ySlider.setValue(originalLightCalibration.y);

        xTextField.setText(converter.toString(xSlider.getValue()));
        yTextField.setText(converter.toString(ySlider.getValue()));
    }

    public void unbind(SettingsModelImpl injectedSettingsModel)
    {
        injectedSettingsModel.getObjectProperty("currentLightCalibration", Vector2.class).removeListener(settingsListener);
    }

    public void apply()
    {
        // Close window and the onCloseRequest will take care of the rest.
        Window window = root.getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }
}
