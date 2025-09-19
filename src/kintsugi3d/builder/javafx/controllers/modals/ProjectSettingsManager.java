package kintsugi3d.builder.javafx.controllers.modals;

import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.javafx.internal.ObservableGeneralSettingsModel;
import kintsugi3d.builder.javafx.internal.ReadonlyObservableGeneralSettingsModel;
import kintsugi3d.builder.javafx.util.SafeFloatStringConverter;
import kintsugi3d.builder.javafx.util.SafeNumberStringConverter;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.builder.state.DefaultSettings;
import kintsugi3d.builder.state.GeneralSettingsModel;
import kintsugi3d.builder.state.SimpleGeneralSettingsModel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class ProjectSettingsManager
{
    public GeneralSettingsModel projectSettingsModel;
    public final ObservableGeneralSettingsModel localSettingsModel = getDefaultSettingsModel();
    public final Set<String> trackedSettings = new HashSet<String>();

    public ProjectSettingsManager()
    {
    }

    public static ObservableGeneralSettingsModel getDefaultSettingsModel()
    {
        ObservableGeneralSettingsModel settingsModel = new ObservableGeneralSettingsModel();
        DefaultSettings.applyProjectDefaults(settingsModel);
        return settingsModel;
    }

    public ReadonlyObservableGeneralSettingsModel getLocalSettingsModel()
    {
        return localSettingsModel;
    }

    public GeneralSettingsModel getProjectSettingsModel()
    {
        return projectSettingsModel;
    }

    public Set<String> getTrackedSettings()
    {
        return Collections.unmodifiableSet(trackedSettings);
    }

    public void refresh()
    {
        if (Global.state().getIOModel().hasValidHandler())
        {
            this.projectSettingsModel = Global.state().getIOModel().getLoadedViewSet().getProjectSettings();
        }
        else
        {
            this.projectSettingsModel = new SimpleGeneralSettingsModel();
            DefaultSettings.applyProjectDefaults(projectSettingsModel);
        }

        // Populate local model with the current project settings.
        localSettingsModel.copyFrom(this.projectSettingsModel);
    }

    /**
     * Apply settings that have been bound / tracked on this controller to the project.
     *
     * @return
     */
    @FXML
    public void applySettings()
    {
        this.projectSettingsModel.copyFrom(localSettingsModel, trackedSettings);
    }

    /**
     * Locally reset settings that have been bound / tracked on this controller to their default values.
     * To have these defaults applied to the project, applyBoundSettings() will also need to be called.
     */
    @FXML
    public void resetSettingsToDefaults()
    {
        GeneralSettingsModel defaults = getDefaultSettingsModel();
        localSettingsModel.copyFrom(defaults, trackedSettings);
    }

    public void trackSetting(String settingName)
    {
        trackedSettings.add(settingName);
    }

    public void bindFloatSetting(TextField textField, String settingName, float minValue, float maxValue)
    {
        trackSetting(settingName);
        StaticUtilities.makeClampedNumeric(minValue, maxValue, textField);
        SafeFloatStringConverter converter = new SafeFloatStringConverter(localSettingsModel.getFloat(settingName));
        textField.setText(converter.toString(localSettingsModel.getFloat(settingName)));
        textField.textProperty().bindBidirectional(localSettingsModel.getNumericProperty(settingName), converter);
    }

    public void bindNormalizedSetting(TextField textField, String settingName)
    {
        bindFloatSetting(textField, settingName, 0, 1);
    }

    public void bindIntegerSetting(TextField textField, String settingName, int minValue, int maxValue)
    {
        trackSetting(settingName);
        StaticUtilities.makeClampedInteger(minValue, maxValue, textField);
        SafeNumberStringConverter converter = new SafeNumberStringConverter(localSettingsModel.getInt(settingName));
        textField.setText(converter.toString(localSettingsModel.getInt(settingName)));
        textField.textProperty().bindBidirectional(localSettingsModel.getNumericProperty(settingName), converter);
    }

    public void bindNumericSetting(Slider slider, String settingName)
    {
        trackSetting(settingName);
        slider.setValue(localSettingsModel.getDouble(settingName));
        slider.valueProperty().bindBidirectional(localSettingsModel.getNumericProperty(settingName));
    }

    public void bindBooleanSetting(CheckBox checkBox, String settingName)
    {
        trackSetting(settingName);
        checkBox.setSelected(localSettingsModel.getBoolean(settingName));
        checkBox.selectedProperty().bindBidirectional(localSettingsModel.getBooleanProperty(settingName));
    }

    public void bindBooleanSetting(BooleanProperty property, String settingName)
    {
        trackSetting(settingName);
        property.set(localSettingsModel.getBoolean(settingName));
        property.bindBidirectional(localSettingsModel.getBooleanProperty(settingName));
    }

    public <T> void bindNumericComboBox(ComboBox<T> comboBox, String settingName,
        Function<Number, T> choiceConstructor, Function<T, Number> extractNumeric)
    {
        trackSetting(settingName);

        // Manually bind resolution both ways as a combo box.
        comboBox.valueProperty().addListener(
            (obs, oldValue, newValue) ->
                localSettingsModel.set(settingName, extractNumeric.apply(newValue)));
        localSettingsModel.getNumericProperty(settingName).addListener(
            (obs, oldValue, newValue) ->
                comboBox.setValue(choiceConstructor.apply(newValue)));
        comboBox.setValue(choiceConstructor.apply(localSettingsModel.get(settingName, Number.class)));
    }

    public <T> void bindTextComboBox(ComboBox<T> comboBox, String settingName,
        Function<String, T> choiceConstructor, Function<T, String> extractText)
    {
        trackSetting(settingName);

        // Manually bind resolution both ways as a combo box.
        comboBox.valueProperty().addListener(
            (obs, oldValue, newValue) ->
                localSettingsModel.set(settingName, extractText.apply(newValue)));
        localSettingsModel.getObjectProperty(settingName, String.class).addListener(
            (obs, oldValue, newValue) ->
                comboBox.setValue(choiceConstructor.apply(newValue)));
        comboBox.setValue(choiceConstructor.apply(settingName));
    }

    public void bindTextComboBox(ComboBox<String> comboBox, String settingName)
    {
        bindTextComboBox(comboBox, settingName, Function.identity(), Function.identity());
    }
}