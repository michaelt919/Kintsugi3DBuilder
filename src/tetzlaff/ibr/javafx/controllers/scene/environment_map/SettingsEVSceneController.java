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

public class SettingsEVSceneController implements Initializable{
// Boolean evUseImage
// Boolean evUseColor
// Boolean bpUseImage
// Boolean bpUseColor
// Boolean imagePathsRelative
// String evImagePath
// String bpImagePath
// Double evColorIntensity
// Double evRotation
// Color evColor
// Color bpColor
// String name

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

    private DoubleProperty trueEVColorIntes = new SimpleDoubleProperty();

    private final SafeNumberStringConverter n = new SafeNumberStringConverter(0);

    public ChangeListener<EVSetting> changeListener =
            (observable, oldValue, newValue) -> {
                if (oldValue != null) unbind(oldValue);

                if (newValue != null){ bind(newValue); setDisabled(newValue.isLocked());}
                else setDisabled(true);
            };

    public void setDisabled(boolean value){
        root.setDisable(value);
    }

    private void bind(EVSetting evSetting){

        evUseImageCheckBox.selectedProperty().bindBidirectional(evSetting.evUseImageProperty());
        evUseColorCheckBox.selectedProperty().bindBidirectional(evSetting.evUseColorProperty());
        bpUseImageCheckBox.selectedProperty().bindBidirectional(evSetting.bpUseImageProperty());
        bpUseColorCheckBox.selectedProperty().bindBidirectional(evSetting.bpUseColorProperty());

//        evColorIntensitySlider.valueProperty().bindBidirectional(evSetting.evColorIntensityProperty());
        trueEVColorIntes.bindBidirectional(evSetting.evColorIntensityProperty());

        evRotationSlider.valueProperty().bindBidirectional(evSetting.evRotationProperty());
        evColorIntensityTextField.textProperty().bindBidirectional(evSetting.evColorIntensityProperty(), n);
        evRotationTextField.textProperty().bindBidirectional(evSetting.evRotationProperty(), n);
        evColorPicker.valueProperty().bindBidirectional(evSetting.evColorProperty());
        bpColorPicker.valueProperty().bindBidirectional(evSetting.bpColorProperty());

        localEVImageFile.bindBidirectional(evSetting.evImageFileProperty());
        localBPImageFile.bindBidirectional(evSetting.bpImageFileProperty());


        evUseImageCheckBox.disableProperty().bind(evSetting.firstEVLoadedProperty().not());
    }

    private void unbind(EVSetting evSetting){

        evUseImageCheckBox.selectedProperty().unbindBidirectional(evSetting.evUseImageProperty());
        evUseColorCheckBox.selectedProperty().unbindBidirectional(evSetting.evUseColorProperty());
        bpUseImageCheckBox.selectedProperty().unbindBidirectional(evSetting.bpUseImageProperty());
        bpUseColorCheckBox.selectedProperty().unbindBidirectional(evSetting.bpUseColorProperty());

//        evColorIntensitySlider.valueProperty().unbindBidirectional(evSetting.evColorIntensityProperty());
        trueEVColorIntes.unbindBidirectional(evSetting.evColorIntensityProperty());

        evRotationSlider.valueProperty().unbindBidirectional(evSetting.evRotationProperty());
        evColorIntensityTextField.textProperty().unbindBidirectional(evSetting.evColorIntensityProperty());
        evRotationTextField.textProperty().unbindBidirectional(evSetting.evRotationProperty());
        evColorPicker.valueProperty().unbindBidirectional(evSetting.evColorProperty());
        bpColorPicker.valueProperty().unbindBidirectional(evSetting.bpColorProperty());

        localEVImageFile.unbindBidirectional(evSetting.evImageFileProperty());
        localBPImageFile.unbindBidirectional(evSetting.bpImageFileProperty());

    }

    //Files
    private Property<File> localEVImageFile = new SimpleObjectProperty<>();
    private Property<File> localBPImageFile = new SimpleObjectProperty<>();

    private final FileChooser evImageFileChooser = new FileChooser();
    private final FileChooser bpImageFileChooser = new FileChooser();



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
        StaticUtilities.powerBind(evColorIntensitySlider.valueProperty(), trueEVColorIntes);




        evImageFileChooser.setTitle("Pick file for environment map");
        bpImageFileChooser.setTitle("pick file for backplate");

        evImageFileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        bpImageFileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        evImageFileChooser.getExtensionFilters().addAll(
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

        localEVImageFile.addListener((observable, oldValue, newValue) -> {
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

    @FXML private void pickEVImageFile(){
       File newFile = evImageFileChooser.showOpenDialog(root.getScene().getWindow());
        if(newFile != null) localEVImageFile.setValue(newFile);
    }
    @FXML private void pickBPImageFile(){
        File newFile = bpImageFileChooser.showOpenDialog(root.getScene().getWindow());
        if(newFile != null) localBPImageFile.setValue(newFile);
    }
}

