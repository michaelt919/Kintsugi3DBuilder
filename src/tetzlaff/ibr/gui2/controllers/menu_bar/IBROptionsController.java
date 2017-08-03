package tetzlaff.ibr.gui2.controllers.menu_bar;//Created by alexk on 7/31/2017.

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import tetzlaff.ibr.util.Flag;
import tetzlaff.ibr.util.StaticHouse;
import tetzlaff.util.SafeFloatStringConverter;
import tetzlaff.util.SafeNumberStringConverter;
import tetzlaff.util.ShadingParameterMode;

import java.net.URL;
import java.util.ResourceBundle;

public class IBROptionsController implements Initializable{
    @FXML private CheckBox texturesCheckBox;
    @FXML private CheckBox occlusionCheckBox;
    @FXML private CheckBox geometricAttenuationCheckBox;
    @FXML private TextField gamaTextField;
    @FXML private TextField weightExponentTextField;
    @FXML private TextField isotropyFactorTextField;
    @FXML private TextField occlusionBiasTextField;
    @FXML private Slider gamaSlider;
    @FXML private Slider weightExponentSlider;
    @FXML private Slider isotropyFactorSlider;
    @FXML private Slider occlusionBiasSlider;
    @FXML private ChoiceBox<ShadingParameterMode> weightModeChoiceBox;
    @FXML private GridPane root;

    private IBRSettingsUIImpl settingCash;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        weightModeChoiceBox.setConverter(new StringConverter<ShadingParameterMode>() {
            @Override
            public String toString(ShadingParameterMode object) {
                return object.name();
            }

            @Override
            public ShadingParameterMode fromString(String string) {
                return ShadingParameterMode.valueOf(string);
            }
        });
        weightModeChoiceBox.getItems().addAll(ShadingParameterMode.values());


        StaticHouse.bound(1,5, gamaTextField);
        StaticHouse.bound(1,100, weightExponentTextField);
        StaticHouse.bound(0,1, isotropyFactorTextField);
        StaticHouse.bound(0, 0.1, occlusionBiasTextField);

    }


    public void bind(IBRSettingsUIImpl ibrSettingsUIImpl){

        texturesCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.texturesProperty());
        occlusionCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.occlusionProperty());
        geometricAttenuationCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.pBRGeometricAttenuationProperty());
        weightModeChoiceBox.valueProperty().bindBidirectional(ibrSettingsUIImpl.weightModeProperty());

        gamaSlider.valueProperty().bindBidirectional(ibrSettingsUIImpl.gammaProperty());
        gamaTextField.textProperty().bindBidirectional(ibrSettingsUIImpl.gammaProperty(), new SafeFloatStringConverter(2.2f));

        weightExponentSlider.valueProperty().bindBidirectional(ibrSettingsUIImpl.weightExponentProperty());
        weightExponentTextField.textProperty().bindBidirectional(ibrSettingsUIImpl.weightExponentProperty(), new SafeFloatStringConverter(16f));

        isotropyFactorSlider.valueProperty().bindBidirectional(ibrSettingsUIImpl.isotropyFactorProperty());
        isotropyFactorTextField.textProperty().bindBidirectional(ibrSettingsUIImpl.isotropyFactorProperty(), new SafeFloatStringConverter(0f));

        occlusionBiasSlider.valueProperty().bindBidirectional(ibrSettingsUIImpl.occlusionBiasProperty());
        occlusionBiasTextField.textProperty().bindBidirectional(ibrSettingsUIImpl.occlusionBiasProperty(), new SafeFloatStringConverter(0.0025f));


        settingCash = ibrSettingsUIImpl;
        root.getScene().getWindow().setOnCloseRequest(param->unbind());
    }


    private void unbind(){

        System.out.println("unbind");

        texturesCheckBox.selectedProperty().unbindBidirectional(settingCash.texturesProperty());
        occlusionCheckBox.selectedProperty().unbindBidirectional(settingCash.occlusionProperty());
        geometricAttenuationCheckBox.selectedProperty().unbindBidirectional(settingCash.pBRGeometricAttenuationProperty());

        weightModeChoiceBox.valueProperty().unbindBidirectional(settingCash.weightModeProperty());

        gamaSlider.valueProperty().unbindBidirectional(settingCash.gammaProperty());
        gamaTextField.textProperty().unbindBidirectional(settingCash.gammaProperty());

        weightExponentSlider.valueProperty().unbindBidirectional(settingCash.weightExponentProperty());
        weightExponentTextField.textProperty().unbindBidirectional(settingCash.weightExponentProperty());

        isotropyFactorSlider.valueProperty().unbindBidirectional(settingCash.isotropyFactorProperty());
        isotropyFactorTextField.textProperty().unbindBidirectional(settingCash.occlusionBiasProperty());

        occlusionBiasSlider.valueProperty().unbindBidirectional(settingCash.occlusionBiasProperty());
        occlusionBiasTextField.textProperty().unbindBidirectional(settingCash.occlusionBiasProperty());

    }
}