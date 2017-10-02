package tetzlaff.ibr.javafx.controllers.scene.environment;

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
import tetzlaff.ibr.javafx.util.SafeNumberStringConverter;
import tetzlaff.ibr.javafx.util.StaticUtilities;

public class SettingsEnvironmentSceneController implements Initializable
{

    @FXML VBox root;

    @FXML CheckBox envUseImageCheckBox;
    @FXML CheckBox envUseColorCheckBox;
    @FXML CheckBox bpUseImageCheckBox;
    @FXML CheckBox bpUseColorCheckBox;
    @FXML TextField envIntensityTextField;
    @FXML Slider envIntensitySlider;
    @FXML TextField envRotationTextField;
    @FXML Slider envRotationSlider;
    @FXML ColorPicker envColorPicker;
    @FXML ColorPicker bpColorPicker;

    @FXML Label envFileNameText;
    @FXML Label bpFileNameText;
    @FXML ImageView envImageView;
    @FXML ImageView bpImageView;

    private final DoubleProperty trueEnvColorIntes = new SimpleDoubleProperty();

    private final SafeNumberStringConverter n = new SafeNumberStringConverter(0);

    //Files
    private final Property<File> localEnvImageFile = new SimpleObjectProperty<>();
    private final Property<File> localBPImageFile = new SimpleObjectProperty<>();

    private final FileChooser envImageFileChooser = new FileChooser();
    private final FileChooser bpImageFileChooser = new FileChooser();

    ChangeListener<EnvironmentSetting> changeListener =
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

//        evColorIntensitySlider.valueProperty().bindBidirectional(evSetting.evColorIntensityProperty());
        trueEnvColorIntes.bindBidirectional(envSetting.envColorIntensityProperty());

        envRotationSlider.valueProperty().bindBidirectional(envSetting.envRotationProperty());
        envIntensityTextField.textProperty().bindBidirectional(envSetting.envColorIntensityProperty(), n);
        envRotationTextField.textProperty().bindBidirectional(envSetting.envRotationProperty(), n);
        envColorPicker.valueProperty().bindBidirectional(envSetting.envColorProperty());
        bpColorPicker.valueProperty().bindBidirectional(envSetting.bpColorProperty());

        localEnvImageFile.bindBidirectional(envSetting.envImageFileProperty());
        localBPImageFile.bindBidirectional(envSetting.bpImageFileProperty());

        envUseImageCheckBox.disableProperty().bind(envSetting.firstEnvLoadedProperty().not());
    }

    private void unbind(EnvironmentSetting evSetting)
    {

        envUseImageCheckBox.selectedProperty().unbindBidirectional(evSetting.envUseImageProperty());
        envUseColorCheckBox.selectedProperty().unbindBidirectional(evSetting.envUseColorProperty());
        bpUseImageCheckBox.selectedProperty().unbindBidirectional(evSetting.bpUseImageProperty());
        bpUseColorCheckBox.selectedProperty().unbindBidirectional(evSetting.bpUseColorProperty());

//        evColorIntensitySlider.valueProperty().unbindBidirectional(evSetting.evColorIntensityProperty());
        trueEnvColorIntes.unbindBidirectional(evSetting.envColorIntensityProperty());

        envRotationSlider.valueProperty().unbindBidirectional(evSetting.envRotationProperty());
        envIntensityTextField.textProperty().unbindBidirectional(evSetting.envColorIntensityProperty());
        envRotationTextField.textProperty().unbindBidirectional(evSetting.envRotationProperty());
        envColorPicker.valueProperty().unbindBidirectional(evSetting.envColorProperty());
        bpColorPicker.valueProperty().unbindBidirectional(evSetting.bpColorProperty());

        localEnvImageFile.unbindBidirectional(evSetting.envImageFileProperty());
        localBPImageFile.unbindBidirectional(evSetting.bpImageFileProperty());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {

        StaticUtilities.wrap(-180, 180, envRotationTextField);
        StaticUtilities.bound(0, Double.MAX_VALUE, envIntensityTextField);

        envIntensitySlider.setLabelFormatter(new DoubleStringConverter()
        {
            @Override
            public String toString(Double value)
            {
                return super.toString(Math.pow(10, value));
            }
        });
        StaticUtilities.powerBind(envIntensitySlider.valueProperty(), trueEnvColorIntes);

        envImageFileChooser.setTitle("Pick file for environment map");
        bpImageFileChooser.setTitle("pick file for backplate");

        envImageFileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        bpImageFileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        envImageFileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("HDR", "*.hdr"),
            new ExtensionFilter("PNG", "*.png"),
            new ExtensionFilter("JPG", "*.jpg"),
            new ExtensionFilter("Other", "*.*")
        );

        bpImageFileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("HDR", "*.hdr"),
            new ExtensionFilter("PNG", "*.png"),
            new ExtensionFilter("JPG", "*.jpg"),
            new ExtensionFilter("Other", "*.*")
        );

        localEnvImageFile.addListener((observable, oldValue, newValue) ->
        {
            if (newValue != null)
            {
                envFileNameText.setVisible(true);
                envFileNameText.setText(newValue.getName());

                envImageView.setImage(new Image(newValue.toURI().toString()));
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

    @FXML
    private void pickEnvImageFile()
    {
        File newFile = envImageFileChooser.showOpenDialog(root.getScene().getWindow());
        if (newFile != null)
        {
            localEnvImageFile.setValue(newFile);
            envUseImageCheckBox.setSelected(true);
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
        }
    }
}

