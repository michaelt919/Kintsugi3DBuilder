package tetzlaff.ibr.gui2.controllers.menu_bar;//Created by alexk on 7/31/2017.

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import tetzlaff.util.ShadingParameterMode;

import java.net.URL;
import java.util.ResourceBundle;

public class IBROptionsController implements Initializable{
    @FXML private CheckBox iBRenderingCheckBox;
    @FXML private CheckBox relightingCheckBox;
    @FXML private CheckBox texturesCheckBox;
    @FXML private CheckBox shadowsCheckBox;
    @FXML private CheckBox visibleLightsCheckBox;
    @FXML private CheckBox fresnelCheckBox;
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

        iBRenderingCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.iBR);
        relightingCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.relighting);
        texturesCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.textures);
        shadowsCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.shadows);
        visibleLightsCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.visibleLights);
        fresnelCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.fresnel);
        occlusionCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.occlusion);
        geometricAttenuationCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.pBRGeometricAttenuation);

        weightModeChoiceBox.valueProperty().bindBidirectional(ibrSettingsUIImpl.weightMode);

        settingCash = ibrSettingsUIImpl;
        root.getScene().getWindow().setOnCloseRequest(param->unbind());
    }

    private void unbind(){

        iBRenderingCheckBox.selectedProperty().unbindBidirectional(settingCash.iBR);
        relightingCheckBox.selectedProperty().unbindBidirectional(settingCash.relighting);
        texturesCheckBox.selectedProperty().unbindBidirectional(settingCash.textures);
        shadowsCheckBox.selectedProperty().unbindBidirectional(settingCash.shadows);
        visibleLightsCheckBox.selectedProperty().unbindBidirectional(settingCash.visibleLights);
        fresnelCheckBox.selectedProperty().unbindBidirectional(settingCash.fresnel);
        occlusionCheckBox.selectedProperty().unbindBidirectional(settingCash.occlusion);
        geometricAttenuationCheckBox.selectedProperty().unbindBidirectional(settingCash.pBRGeometricAttenuation);

        weightModeChoiceBox.valueProperty().unbindBidirectional(settingCash.weightMode);

    }
}