/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.tools;

import kintsugi3d.builder.state.SettingsModel;

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
        public ToggleSettingTool create()
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
