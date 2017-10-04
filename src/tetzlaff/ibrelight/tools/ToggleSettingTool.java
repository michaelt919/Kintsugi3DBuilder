package tetzlaff.ibrelight.tools;

import java.util.function.Consumer;

import tetzlaff.ibrelight.core.SettingsModel;

public class ToggleSettingTool implements KeyPressTool
{
    private static class Builder extends ToolBuilderBase<ToggleSettingTool>
    {
        private final Consumer<SettingsModel> toggleFunction;

        Builder(Consumer<SettingsModel> toggleFunction)
        {
            this.toggleFunction = toggleFunction;
        }

        @Override
        public ToggleSettingTool build()
        {
            return new ToggleSettingTool(toggleFunction, getSettingsModel());
        }
    }

    static ToolBuilder<ToggleSettingTool> getBuilder(Consumer<SettingsModel> toggleFunction)
    {
        return new Builder(toggleFunction);
    }

    private final Consumer<SettingsModel> toggleFunction;
    private final SettingsModel settingsModel;

    public ToggleSettingTool(Consumer<SettingsModel> toggleFunction, SettingsModel settingsModel)
    {
        this.toggleFunction = toggleFunction;
        this.settingsModel = settingsModel;
    }

    @Override
    public void keyPressed()
    {
        toggleFunction.accept(settingsModel);
    }
}
