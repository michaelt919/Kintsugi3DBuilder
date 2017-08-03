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

    }

    private void setUpSlider(Slider slider, float valueStart){
//        slider.set
    }

    private void preBindSlidersSetup(float gamaStart, float weightStart, float isoStart, float occStart){




    }


    public void bind(IBRSettingsUIImpl ibrSettingsUIImpl){

        texturesCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.texturesProperty());
        occlusionCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.occlusionProperty());
        geometricAttenuationCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.pBRGeometricAttenuationProperty());

        weightModeChoiceBox.valueProperty().bindBidirectional(ibrSettingsUIImpl.weightModeProperty());

        settingCash = ibrSettingsUIImpl;
        root.getScene().getWindow().setOnCloseRequest(param->unbind());
    }


    private void unbind(){

        System.out.println("unbind");

        texturesCheckBox.selectedProperty().unbindBidirectional(settingCash.texturesProperty());
        occlusionCheckBox.selectedProperty().unbindBidirectional(settingCash.occlusionProperty());
        geometricAttenuationCheckBox.selectedProperty().unbindBidirectional(settingCash.pBRGeometricAttenuationProperty());

        weightModeChoiceBox.valueProperty().unbindBidirectional(settingCash.weightModeProperty());

    }
}