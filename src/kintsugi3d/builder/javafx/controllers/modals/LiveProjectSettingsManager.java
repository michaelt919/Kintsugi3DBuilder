package kintsugi3d.builder.javafx.controllers.modals;

import kintsugi3d.builder.javafx.internal.ObservableGeneralSettingsModel;

public class LiveProjectSettingsManager extends ProjectSettingsManager
{
    private final ObservableGeneralSettingsModel revertSettingsModel = ProjectSettingsManager.getDefaultSettingsModel();

    @Override
    public void trackSetting(String settingName)
    {
        // Local model automatically updates when the UI is changed
        // Add a listener to automatically push to the project settings model.
        super.trackSetting(settingName);
        getLocalSettingsModel().getObservable(settingName).addListener(obs ->
            getProjectSettingsModel().copyFrom(getLocalSettingsModel(), settingName));
    }

    @Override
    public void refresh()
    {
        super.refresh();

        // Remember what to set back to if the user cancels
        this.revertSettingsModel.copyFrom(getProjectSettingsModel());
    }

    public void cancel()
    {
        // Revert back to the settings when the window was opened.
        getProjectSettingsModel().copyFrom(revertSettingsModel, getTrackedSettings());
    }
}
