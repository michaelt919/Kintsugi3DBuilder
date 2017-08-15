package tetzlaff.ibr.javafx.controllers.scene.environment_map;


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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.converter.DoubleStringConverter;
import tetzlaff.ibr.javafx.util.SafeNumberStringConverter;
import tetzlaff.ibr.javafx.util.StaticUtilities;

public class SettingsEnvironmentSceneController implements Initializable{

    @FXML VBox root;

    @FXML CheckBox evUseImageCheckBox;
    @FXML CheckBox evUseColorCheckBox;
    @FXML CheckBox bpUseImageCheckBox;
    @FXML CheckBox bpUseColorCheckBox;
    @FXML TextField evColorIntensityTextField;
    @FXML Slider evColorIntensitySlider;
    @FXML TextField evRotationTextField;
    @FXML Slider evRotationSlider;
    @FXML ColorPicker evColorPicker;
    @FXML ColorPicker bpColorPicker;

    @FXML Text evFileNameText;
    @FXML Text bpFileNameText;
    @FXML ImageView evImageView;
    @FXML ImageView bpImageView;

    private DoubleProperty trueEnvColorIntes = new SimpleDoubleProperty();

    private final SafeNumberStringConverter n = new SafeNumberStringConverter(0);

    //Files
    private Property<File> localEnvImageFile = new SimpleObjectProperty<>();
    private Property<File> localBPImageFile = new SimpleObjectProperty<>();

    private final FileChooser envImageFileChooser = new FileChooser();
    private final FileChooser bpImageFileChooser = new FileChooser();

    public ChangeListener<EnvironmentSettings> changeListener =
            (observable, oldValue, newValue) -> {
                if (oldValue != null) unbind(oldValue);

                if (newValue != null){ bind(newValue); setDisabled(newValue.isLocked());}
                else setDisabled(true);
            };

    public void setDisabled(boolean value){
        root.setDisable(value);
    }

    private void bind(EnvironmentSettings envSetting){

        evUseImageCheckBox.selectedProperty().bindBidirectional(envSetting.envUseImageProperty());
        evUseColorCheckBox.selectedProperty().bindBidirectional(envSetting.envUseColorProperty());
        bpUseImageCheckBox.selectedProperty().bindBidirectional(envSetting.bpUseImageProperty());
        bpUseColorCheckBox.selectedProperty().bindBidirectional(envSetting.bpUseColorProperty());

//        evColorIntensitySlider.valueProperty().bindBidirectional(evSetting.evColorIntensityProperty());
        trueEnvColorIntes.bindBidirectional(envSetting.envColorIntensityProperty());

        evRotationSlider.valueProperty().bindBidirectional(envSetting.envRotationProperty());
        evColorIntensityTextField.textProperty().bindBidirectional(envSetting.envColorIntensityProperty(), n);
        evRotationTextField.textProperty().bindBidirectional(envSetting.envRotationProperty(), n);
        evColorPicker.valueProperty().bindBidirectional(envSetting.envColorProperty());
        bpColorPicker.valueProperty().bindBidirectional(envSetting.bpColorProperty());

        localEnvImageFile.bindBidirectional(envSetting.envImageFileProperty());
        localBPImageFile.bindBidirectional(envSetting.bpImageFileProperty());


        evUseImageCheckBox.disableProperty().bind(envSetting.firstEnvLoadedProperty().not());
    }

    private void unbind(EnvironmentSettings evSetting){

        evUseImageCheckBox.selectedProperty().unbindBidirectional(evSetting.envUseImageProperty());
        evUseColorCheckBox.selectedProperty().unbindBidirectional(evSetting.envUseColorProperty());
        bpUseImageCheckBox.selectedProperty().unbindBidirectional(evSetting.bpUseImageProperty());
        bpUseColorCheckBox.selectedProperty().unbindBidirectional(evSetting.bpUseColorProperty());

//        evColorIntensitySlider.valueProperty().unbindBidirectional(evSetting.evColorIntensityProperty());
        trueEnvColorIntes.unbindBidirectional(evSetting.envColorIntensityProperty());

        evRotationSlider.valueProperty().unbindBidirectional(evSetting.envRotationProperty());
        evColorIntensityTextField.textProperty().unbindBidirectional(evSetting.envColorIntensityProperty());
        evRotationTextField.textProperty().unbindBidirectional(evSetting.envRotationProperty());
        evColorPicker.valueProperty().unbindBidirectional(evSetting.envColorProperty());
        bpColorPicker.valueProperty().unbindBidirectional(evSetting.bpColorProperty());

        localEnvImageFile.unbindBidirectional(evSetting.envImageFileProperty());
        localBPImageFile.unbindBidirectional(evSetting.bpImageFileProperty());

    }



    @Override
    public void initialize(URL location, ResourceBundle resources) {

        StaticUtilities.wrap(-180, 180, evRotationTextField);
        StaticUtilities.bound(0, Double.MAX_VALUE, evColorIntensityTextField);

        evColorIntensitySlider.setLabelFormatter(new DoubleStringConverter(){
            @Override
            public String toString(Double value) {
                return super.toString(Math.pow(10,value));
            }
        });
        StaticUtilities.powerBind(evColorIntensitySlider.valueProperty(), trueEnvColorIntes);




        envImageFileChooser.setTitle("Pick file for environment map");
        bpImageFileChooser.setTitle("pick file for backplate");

        envImageFileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        bpImageFileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        envImageFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("HDR", "*.hdr"),
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("Other", "*.*")
        );

        bpImageFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("HDR", "*.hdr"),
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("Other", "*.*")
        );

        localEnvImageFile.addListener((observable, oldValue, newValue) -> {
            if(newValue != null){
                evFileNameText.setText(newValue.getName());

                evImageView.setImage(new Image(newValue.toURI().toString()));

            }
            else{
                evFileNameText.setText("Filename");

                evImageView.setImage(null);
            }
        });

        localBPImageFile.addListener((ob, o, n)->{
            if(n != null){
                bpFileNameText.setText(n.getName());

                bpImageView.setImage(new Image(n.toURI().toString()));

            }
            else{
                bpFileNameText.setText("Filename");

                bpImageView.setImage(null);
            }
        });

        setDisabled(true);

    }

    @FXML private void pickEnvImageFile(){
       File newFile = envImageFileChooser.showOpenDialog(root.getScene().getWindow());
        if(newFile != null) localEnvImageFile.setValue(newFile);
    }
    @FXML private void pickBPImageFile(){
        File newFile = bpImageFileChooser.showOpenDialog(root.getScene().getWindow());
        if(newFile != null) localBPImageFile.setValue(newFile);
    }
}

