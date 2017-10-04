package tetzlaff.ibrelight.javafx.controllers.menubar;//Created by alexk on 7/31/2017.

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import tetzlaff.ibrelight.javafx.internal.SettingsModelImpl;
import tetzlaff.ibrelight.javafx.util.SafeFloatStringConverter;
import tetzlaff.ibrelight.javafx.util.StaticUtilities;
import tetzlaff.util.ShadingParameterMode;

public class IBROptionsController implements Initializable
{
    @FXML private CheckBox occlusionCheckBox;
    @FXML private TextField gammaTextField;
    @FXML private TextField weightExponentTextField;
    @FXML private TextField isotropyFactorTextField;
    @FXML private TextField occlusionBiasTextField;
    @FXML private Slider gammaSlider;
    @FXML private Slider weightExponentSlider;
    @FXML private Slider isotropyFactorSlider;
    @FXML private Slider occlusionBiasSlider;
    @FXML private ChoiceBox<ShadingParameterMode> weightModeChoiceBox;
    @FXML private GridPane root;

    private SettingsModelImpl settingCache;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        weightModeChoiceBox.setConverter(new StringConverter<ShadingParameterMode>()
        {
            @Override
            public String toString(ShadingParameterMode object)
            {
                return object.name();
            }

            @Override
            public ShadingParameterMode fromString(String string)
            {
                return ShadingParameterMode.valueOf(string);
            }
        });
        weightModeChoiceBox.getItems().addAll(ShadingParameterMode.values());

        StaticUtilities.bound(1, 5, gammaTextField);
        StaticUtilities.bound(1, 100, weightExponentTextField);
        StaticUtilities.bound(0, 1, isotropyFactorTextField);
        StaticUtilities.bound(0, 0.1, occlusionBiasTextField);
    }

    public void bind(SettingsModelImpl ibrSettingsUIImpl)
    {
        occlusionCheckBox.selectedProperty().bindBidirectional(ibrSettingsUIImpl.occlusionProperty());
        weightModeChoiceBox.valueProperty().bindBidirectional(ibrSettingsUIImpl.weightModeProperty());

        gammaSlider.valueProperty().bindBidirectional(ibrSettingsUIImpl.gammaProperty());
        gammaTextField.textProperty().bindBidirectional(ibrSettingsUIImpl.gammaProperty(), new SafeFloatStringConverter(2.2f));

        weightExponentSlider.valueProperty().bindBidirectional(ibrSettingsUIImpl.weightExponentProperty());
        weightExponentTextField.textProperty().bindBidirectional(ibrSettingsUIImpl.weightExponentProperty(), new SafeFloatStringConverter(16f));

        isotropyFactorSlider.valueProperty().bindBidirectional(ibrSettingsUIImpl.isotropyFactorProperty());
        isotropyFactorTextField.textProperty().bindBidirectional(ibrSettingsUIImpl.isotropyFactorProperty(), new SafeFloatStringConverter(0f));

        occlusionBiasSlider.valueProperty().bindBidirectional(ibrSettingsUIImpl.occlusionBiasProperty());
        occlusionBiasTextField.textProperty().bindBidirectional(ibrSettingsUIImpl.occlusionBiasProperty(), new SafeFloatStringConverter(0.0025f));

        settingCache = ibrSettingsUIImpl;
        root.getScene().getWindow().setOnCloseRequest(param -> unbind());
    }

    private void unbind()
    {

        System.out.println("unbind");

        occlusionCheckBox.selectedProperty().unbindBidirectional(settingCache.occlusionProperty());

        weightModeChoiceBox.valueProperty().unbindBidirectional(settingCache.weightModeProperty());

        gammaSlider.valueProperty().unbindBidirectional(settingCache.gammaProperty());
        gammaTextField.textProperty().unbindBidirectional(settingCache.gammaProperty());

        weightExponentSlider.valueProperty().unbindBidirectional(settingCache.weightExponentProperty());
        weightExponentTextField.textProperty().unbindBidirectional(settingCache.weightExponentProperty());

        isotropyFactorSlider.valueProperty().unbindBidirectional(settingCache.isotropyFactorProperty());
        isotropyFactorTextField.textProperty().unbindBidirectional(settingCache.occlusionBiasProperty());

        occlusionBiasSlider.valueProperty().unbindBidirectional(settingCache.occlusionBiasProperty());
        occlusionBiasTextField.textProperty().unbindBidirectional(settingCache.occlusionBiasProperty());
    }
}