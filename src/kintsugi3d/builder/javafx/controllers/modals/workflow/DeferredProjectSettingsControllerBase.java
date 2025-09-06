package kintsugi3d.builder.javafx.controllers.modals.workflow;

import javafx.fxml.FXML;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.IOModel;
import kintsugi3d.builder.javafx.controllers.paged.NonDataPageControllerBase;
import kintsugi3d.builder.javafx.internal.ObservableGeneralSettingsModel;
import kintsugi3d.builder.state.DefaultSettings;

/**
 * Base class for controllers that allow the user to alter settings but the effects of the settings are only
 * apparent upon some discrete event (rather than being updated in real-time).
 */
public abstract class DeferredProjectSettingsControllerBase extends NonDataPageControllerBase
{
    private final ObservableGeneralSettingsModel localSettingsModel = getDefaultSettingsModel();

    private static ObservableGeneralSettingsModel getDefaultSettingsModel()
    {
        ObservableGeneralSettingsModel settingsModel = new ObservableGeneralSettingsModel();
        DefaultSettings.applyProjectDefaults(settingsModel);
        return settingsModel;
    }

    @Override
    public void refresh()
    {
        // Populate local model with the current project settings.
        IOModel ioModel = Global.state().getIOModel();
        if (ioModel.hasValidHandler())
        {
            localSettingsModel.copyFrom(ioModel.getLoadedViewSet().getProjectSettings());
        }
    }

    @FXML
    public boolean cancel()
    {
        return true;
    }

    protected ObservableGeneralSettingsModel getLocalSettingsModel()
    {
        return localSettingsModel;
    }

    protected boolean applySettings()
    {
        IOModel ioModel = Global.state().getIOModel();
        if (!ioModel.hasValidHandler())
        {
            error("Failed to apply settings", "No project is loaded.");
            return false;
        }

        // Apply settings so they're seen by the SpecularFitRequest and also remembered for later.
        ioModel.getLoadedViewSet().getProjectSettings().copyFrom(localSettingsModel);
        return true;
    }
}
