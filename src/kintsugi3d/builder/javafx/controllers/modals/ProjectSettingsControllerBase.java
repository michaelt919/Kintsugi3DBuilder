package kintsugi3d.builder.javafx.controllers.modals;

import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import kintsugi3d.builder.javafx.controllers.paged.NonDataPageControllerBase;
import kintsugi3d.builder.javafx.internal.ReadonlyObservableGeneralSettingsModel;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.builder.state.GeneralSettingsModel;

import java.util.Set;
import java.util.function.Function;

/**
 * Base class for controllers that allow the user to alter settings but the effects of the settings are only
 * apparent upon some discrete event (rather than being updated in real-time).
 * Keeps track of which settings have been bound to allow for them to be conveniently applied or reset to defaults.
 */
public abstract class ProjectSettingsControllerBase extends NonDataPageControllerBase
{
    private final ProjectSettingsManager projectSettingsManager;

    protected ProjectSettingsControllerBase()
    {
        this.projectSettingsManager = new ProjectSettingsManager();
    }

    protected ProjectSettingsControllerBase(ProjectSettingsManager projectSettingsManager)
    {
        this.projectSettingsManager = projectSettingsManager;
    }

    protected ReadonlyObservableGeneralSettingsModel getLocalSettingsModel()
    {
        return projectSettingsManager.getLocalSettingsModel();
    }

    protected GeneralSettingsModel getProjectSettingsModel()
    {
        return projectSettingsManager.getProjectSettingsModel();
    }

    protected Set<String> getTrackedSettings()
    {
        return projectSettingsManager.getTrackedSettings();
    }

    @Override
    public void refresh()
    {
        // Populate local model with the current project settings.
        projectSettingsManager.refresh();
    }

    @FXML
    public boolean cancel()
    {
        return StaticUtilities.confirmCancel();
    }

    /**
     * Apply settings that have been bound / tracked on this controller to the project.
     * @return
     */
    @FXML
    public void applySettings()
    {
        projectSettingsManager.applySettings();
    }

    /**
     * Locally reset settings that have been bound / tracked on this controller to their default values.
     * To have these defaults applied to the project, applyBoundSettings() will also need to be called.
     */
    @FXML
    public void resetSettingsToDefaults()
    {
        projectSettingsManager.resetSettingsToDefaults();
    }

    protected void trackSetting(String settingName)
    {
        projectSettingsManager.trackSetting(settingName);
    }

    protected void bindFloatSetting(TextField textField, String settingName, float minValue, float maxValue)
    {
        projectSettingsManager.bindFloatSetting(textField, settingName, minValue, maxValue);
    }

    protected void bindNormalizedSetting(TextField textField, String settingName)
    {
        projectSettingsManager.bindNormalizedSetting(textField, settingName);
    }

    protected void bindIntegerSetting(TextField textField, String settingName, int minValue, int maxValue)
    {
        projectSettingsManager.bindIntegerSetting(textField, settingName, minValue, maxValue);
    }

    protected void bindNumericSetting(Slider slider, String settingName)
    {
        projectSettingsManager.bindNumericSetting(slider, settingName);
    }

    protected void bindBooleanSetting(CheckBox checkBox, String settingName)
    {
        projectSettingsManager.bindBooleanSetting(checkBox, settingName);
    }

    protected void bindBooleanSetting(BooleanProperty property, String settingName)
    {
        projectSettingsManager.bindBooleanSetting(property, settingName);
    }

    protected <T> void bindNumericComboBox(ComboBox<T> comboBox, String settingName,
        Function<Number, T> choiceConstructor, Function<T, Number> extractNumeric)
    {
        // Manually bind resolution both ways as a combo box.
        projectSettingsManager.bindNumericComboBox(comboBox, settingName, choiceConstructor, extractNumeric);
    }

    protected <T> void bindTextComboBox(ComboBox<T> comboBox, String settingName,
        Function<String, T> choiceConstructor, Function<T, String> extractText)
    {
        // Manually bind resolution both ways as a combo box.
        projectSettingsManager.bindTextComboBox(comboBox, settingName, choiceConstructor, extractText);
    }

    protected void bindTextComboBox(ComboBox<String> comboBox, String settingName)
    {
        projectSettingsManager.bindTextComboBox(comboBox, settingName);
    }
}
