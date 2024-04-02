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

        // Bind text fields to sliders
        xTextField.textProperty().bindBidirectional(xSlider.valueProperty(), new SafeDecimalNumberStringConverter(0.0f));
        yTextField.textProperty().bindBidirectional(ySlider.valueProperty(), new SafeDecimalNumberStringConverter(0.0f));

        // Set initial value to start out synchronized.
        Vector2 originalLightCalibration = injectedSettingsModel.get("currentLightCalibration", Vector2.class);
        xSlider.setValue(originalLightCalibration.x);
        ySlider.setValue(originalLightCalibration.y);
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
