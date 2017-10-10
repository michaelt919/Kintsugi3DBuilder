package tetzlaff.ibrelight.tools;

import tetzlaff.models.SettingsModel;

public class ToggleSettingTool implements KeyPressTool
{
    private static class Builder extends ToolBuilderBase<ToggleSettingTool>
    {
        private final String settingName;

        Builder(String settingName)
        {
            this.settingName = settingName;
        }

        @Override
        public ToggleSettingTool build()
        {
            return new ToggleSettingTool(settingName, getSettingsModel());
        }
    }

    static ToolBuilder<ToggleSettingTool> getBuilder(String settingName)
    {
        return new Builder(settingName);
    }

    private final String settingName;
    private final SettingsModel settingsModel;

    public ToggleSettingTool(String settingName, SettingsModel settingsModel)
    {
        this.settingName = settingName;
        this.settingsModel = settingsModel;
    }

    @Override
    public void keyPressed()
    {
        settingsModel.set(settingName, !settingsModel.getBoolean(settingName));
    }
}
