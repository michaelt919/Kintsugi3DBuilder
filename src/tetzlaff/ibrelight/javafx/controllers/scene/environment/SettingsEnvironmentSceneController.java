package tetzlaff.ibrelight.javafx.controllers.scene.environment;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.converter.DoubleStringConverter;
import tetzlaff.ibrelight.javafx.internal.EnvironmentModelImpl;
import tetzlaff.ibrelight.javafx.util.ImageFactory;
import tetzlaff.ibrelight.javafx.util.SafeNumberStringConverter;
import tetzlaff.ibrelight.javafx.util.StaticUtilities;
import tetzlaff.util.AbstractImage;

public class SettingsEnvironmentSceneController implements Initializable
{
    @FXML private Button envRefreshButton;
    @FXML private Button bpRefreshButton;

    @FXML private VBox root;

    @FXML private CheckBox envUseImageCheckBox;
    @FXML private CheckBox envUseColorCheckBox;
    @FXML private CheckBox bpUseImageCheckBox;
    @FXML private CheckBox bpUseColorCheckBox;
    @FXML private TextField envIntensityTextField;
    @FXML private Slider envIntensitySlider;
    @FXML private TextField envRotationTextField;
    @FXML private Slider envRotationSlider;
    @FXML private TextField envFilteringBiasTextField;
    @FXML private ColorPicker envColorPicker;
    @FXML private ColorPicker bpColorPicker;
    @FXML private TextField backgroundIntensityTextField;
    @FXML private Slider backgroundIntensitySlider;

    @FXML private Label envFileNameText;
    @FXML private Label bpFileNameText;
    @FXML private ImageView envImageView;
    @FXML private ImageView bpImageView;

    private final DoubleProperty trueEnvColorIntensity = new SimpleDoubleProperty();
    private final DoubleProperty trueBackgroundIntensity = new SimpleDoubleProperty();

    private final SafeNumberStringConverter numberStringConverter = new SafeNumberStringConverter(0);

    //Files
    private final Property<File> localEnvImageFile = new SimpleObjectProperty<>();
    private final Property<File> localBPImageFile = new SimpleObjectProperty<>();

    private final FileChooser envImageFileChooser = new FileChooser();
    private final FileChooser bpImageFileChooser = new FileChooser();

    private EnvironmentModelImpl environmentMapModel;

    final ChangeListener<EnvironmentSetting> changeListener = (observable, oldValue, newValue) ->
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
        else
        {
            setDisabled(true);
        }
    };

    public void setDisabled(boolean value)
    {
        root.setDisable(value);
    }

    private void bind(EnvironmentSetting envSetting)
    {
        envUseImageCheckBox.selectedProperty().bindBidirectional(envSetting.envUseImageProperty());
        envUseColorCheckBox.selectedProperty().bindBidirectional(envSetting.envUseColorProperty());
        bpUseImageCheckBox.selectedProperty().bindBidirectional(envSetting.bpUseImageProperty());
        bpUseColorCheckBox.selectedProperty().bindBidirectional(envSetting.bpUseColorProperty());

        trueEnvColorIntensity.bindBidirectional(envSetting.envColorIntensityProperty());
        trueBackgroundIntensity.bindBidirectional(envSetting.backgroundIntensityProperty());

        envRotationSlider.valueProperty().bindBidirectional(envSetting.envRotationProperty());
        envIntensityTextField.textProperty().bindBidirectional(envSetting.envColorIntensityProperty(), numberStringConverter);
        envRotationTextField.textProperty().bindBidirectional(envSetting.envRotationProperty(), numberStringConverter);
        envFilteringBiasTextField.textProperty().bindBidirectional(envSetting.envFilteringBiasProperty(), numberStringConverter);
        envColorPicker.valueProperty().bindBidirectional(envSetting.envColorProperty());
        bpColorPicker.valueProperty().bindBidirectional(envSetting.bpColorProperty());
        backgroundIntensityTextField.textProperty().bindBidirectional(envSetting.backgroundIntensityProperty(), numberStringConverter);

        localEnvImageFile.bindBidirectional(envSetting.envImageFileProperty());
        localBPImageFile.bindBidirectional(envSetting.bpImageFileProperty());

        envUseImageCheckBox.disableProperty().bind(envSetting.envLoadedProperty().not());
        bpUseImageCheckBox.disableProperty().bind(envSetting.bpLoadedProperty().not());
    }

    private void unbind(EnvironmentSetting envSetting)
    {
        envUseImageCheckBox.selectedProperty().unbindBidirectional(envSetting.envUseImageProperty());
        envUseColorCheckBox.selectedProperty().unbindBidirectional(envSetting.envUseColorProperty());
        bpUseImageCheckBox.selectedProperty().unbindBidirectional(envSetting.bpUseImageProperty());
        bpUseColorCheckBox.selectedProperty().unbindBidirectional(envSetting.bpUseColorProperty());

        trueEnvColorIntensity.unbindBidirectional(envSetting.envColorIntensityProperty());
        trueBackgroundIntensity.unbindBidirectional(envSetting.backgroundIntensityProperty());

        envRotationSlider.valueProperty().unbindBidirectional(envSetting.envRotationProperty());
        envIntensityTextField.textProperty().unbindBidirectional(envSetting.envColorIntensityProperty());
        envRotationTextField.textProperty().unbindBidirectional(envSetting.envRotationProperty());
        envFilteringBiasTextField.textProperty().unbindBidirectional(envSetting.envFilteringBiasProperty());
        envColorPicker.valueProperty().unbindBidirectional(envSetting.envColorProperty());
        bpColorPicker.valueProperty().unbindBidirectional(envSetting.bpColorProperty());
        backgroundIntensityTextField.textProperty().unbindBidirectional(envSetting.backgroundIntensityProperty());

        localEnvImageFile.unbindBidirectional(envSetting.envImageFileProperty());
        localBPImageFile.unbindBidirectional(envSetting.bpImageFileProperty());
    }

    private static final DoubleStringConverter LOG_SCALE_CONVERTER = new DoubleStringConverter()
    {
        @Override
        public String toString(Double value)
        {
            return super.toString(Math.pow(10, value));
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        StaticUtilities.makeWrapAroundNumeric(-180, 180, envRotationTextField);
        StaticUtilities.makeClampedNumeric(0, Double.MAX_VALUE, envIntensityTextField);
        StaticUtilities.makeClampedNumeric(0, Double.MAX_VALUE, backgroundIntensityTextField);
        StaticUtilities.makeNumeric(envFilteringBiasTextField);

        envIntensitySlider.setLabelFormatter(LOG_SCALE_CONVERTER);
        StaticUtilities.bindLogScaleToLinear(envIntensitySlider.valueProperty(), trueEnvColorIntensity);

        backgroundIntensitySlider.setLabelFormatter(LOG_SCALE_CONVERTER);
        StaticUtilities.bindLogScaleToLinear(backgroundIntensitySlider.valueProperty(), trueBackgroundIntensity);

        envImageFileChooser.setTitle("Select an environment map");
        bpImageFileChooser.setTitle("Select a backplate image");

        File userHome = new File(System.getProperty("user.home"));

        envImageFileChooser.initialDirectoryProperty().set(userHome);
        bpImageFileChooser.initialDirectoryProperty().set(userHome);

        envImageFileChooser.getExtensionFilters().add(new ExtensionFilter("Radiance HDR environment maps", "*.hdr"));

        bpImageFileChooser.getExtensionFilters().add(
            new ExtensionFilter("Supported image formats", "*.png", "*.jpeg", "*.jpg", "*.bmp", "*.gif"));

        localEnvImageFile.addListener((observable, oldValue, newValue) ->
        {
            if (newValue != null)
            {
                envFileNameText.setVisible(true);
                envFileNameText.setText(newValue.getName());
            }
            else
            {
                envFileNameText.setVisible(false);

                envImageView.setImage(null);
            }
        });

        localBPImageFile.addListener((ob, o, n) ->
        {
            if (n != null)
            {
                bpFileNameText.setVisible(true);
                bpFileNameText.setText(n.getName());

                bpImageView.setImage(new Image(n.toURI().toString()));
            }
            else
            {
                bpFileNameText.setVisible(false);
                bpFileNameText.setText("Filename");

                bpImageView.setImage(null);
            }
        });

        setDisabled(true);
    }

    void updateEnvironmentMapImage(AbstractImage environmentMapImage)
    {
        if (environmentMapImage == null)
        {
            envImageView.setImage(null);
        }
        else
        {
            envImageView.setImage(ImageFactory.createFromAbstractImage(environmentMapImage));
        }
    }

    @FXML
    private void pickEnvImageFile()
    {
        File newFile = envImageFileChooser.showOpenDialog(root.getScene().getWindow());
        if (newFile != null)
        {
            localEnvImageFile.setValue(newFile);
            envUseImageCheckBox.setSelected(true);
            envImageFileChooser.initialDirectoryProperty().set(newFile.getParentFile());
            envImageFileChooser.initialFileNameProperty().set(newFile.getName());
        }
    }

    @FXML
    private void pickBPImageFile()
    {
        File newFile = bpImageFileChooser.showOpenDialog(root.getScene().getWindow());
        if (newFile != null)
        {
            localBPImageFile.setValue(newFile);
            bpUseImageCheckBox.setSelected(true);
            bpImageFileChooser.initialDirectoryProperty().set(newFile.getParentFile());
            bpImageFileChooser.initialFileNameProperty().set(newFile.getName());
        }
    }

    @FXML
    private void refreshEnvironment()
    {
        environmentMapModel.loadEnvironmentMap(localEnvImageFile.getValue());
    }

    @FXML
    private void refreshBackplate()
    {
        environmentMapModel.loadBackplate(localBPImageFile.getValue());
        bpImageView.setImage(new Image(localBPImageFile.getValue().toURI().toString()));
    }

    public void setEnvironmentMapModel(EnvironmentModelImpl environmentMapModel)
    {
        this.environmentMapModel = environmentMapModel;
    }
}
