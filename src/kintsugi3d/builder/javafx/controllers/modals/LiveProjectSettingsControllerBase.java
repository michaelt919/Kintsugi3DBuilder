package kintsugi3d.builder.javafx.controllers.modals;

import kintsugi3d.builder.javafx.internal.ObservableGeneralSettingsModel;

public abstract class LiveProjectSettingsControllerBase extends ProjectSettingsControllerBase
{
    private final ObservableGeneralSettingsModel revertSettingsModel = getDefaultSettingsModel();

    @Override
    protected void trackSetting(String settingName)
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

    @Override
    public boolean cancel()
    {
        if (super.cancel())
        {
            // Revert back to the settings when the window was opened.
            getProjectSettingsModel().copyFrom(revertSettingsModel, getTrackedSettings());
            return true;
        }
        else
        {
            return false;
        }
    }
}
