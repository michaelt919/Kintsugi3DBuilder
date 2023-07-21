package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tetzlaff.ibrelight.util.Launch4jConfiguration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class JvmSettingsController implements Initializable
{
    private static final Logger log = LoggerFactory.getLogger(JvmSettingsController.class);

    private static final int MIN_VALUE = 1;
    private static final int MAX_VALUE = 1048576;

    @FXML private CheckBox maxMemCheckbox;
    @FXML private Spinner<Integer> maxMemSpinner;
    @FXML private Button okButton;
    @FXML private Button applyButton;
    @FXML private Button closeButton;
    @FXML private AnchorPane root;

    private Launch4jConfiguration configuration;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        try
        {
            configuration = Launch4jConfiguration.read();
        }
        catch (IOException e)
        {
            log.error("Failed to read jvm configuration:", e);
            log.error("Using default configuration");
            configuration = Launch4jConfiguration.empty();
        }

        maxMemCheckbox.setSelected(configuration.isEnableMaxMemory());
        maxMemSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_VALUE, MAX_VALUE, configuration.getMaxMemoryMb(), 1));
    }

    public void button_OK(ActionEvent actionEvent)
    {
        button_Apply(actionEvent);
        button_Close(actionEvent);
    }

    public void button_Apply(ActionEvent actionEvent)
    {
        configuration.setEnableMaxMemory(maxMemCheckbox.isSelected());
        configuration.setMaxMemoryMb((Integer)maxMemSpinner.getValue());

        try
        {
            configuration.write();
        }
        catch (IOException e)
        {
            log.error("An error occurred saving jvm settings:", e);
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("IBRelight");
        alert.setHeaderText("Restart Required");
        alert.setContentText("A restart of IBRelight is needed for changes to take effect.");
        alert.show();
    }

    public void button_Close(ActionEvent actionEvent)
    {
        root.getScene().getWindow().hide();
    }
}
