/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

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
