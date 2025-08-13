/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.preferences;

import kintsugi3d.builder.core.ReadonlyLoadOptionsModel;
import kintsugi3d.builder.state.ReadonlySettingsModel;

import java.nio.file.Path;
import java.util.Iterator;

public class ReadOnlyMergedUserPreferencesModel implements ReadOnlyUserPreferencesModel
{
    ReadOnlyUserPreferencesModel base;
    ReadOnlyUserPreferencesModel override;

    public ReadOnlyMergedUserPreferencesModel(ReadOnlyUserPreferencesModel base, ReadOnlyUserPreferencesModel override)
    {
        this.base = base;
        this.override = override;
    }

    private static <T> T nullableOverride(T base, T override)
    {
        if (override == null)
        {
            return base;
        }

        return override;
    }

    @Override
    public ReadonlyLoadOptionsModel getReadOnlyLoadOptions()
    {
        return nullableOverride(base.getReadOnlyLoadOptions(), override.getReadOnlyLoadOptions());
    }

    @Override
    public ReadOnlyDirectoryPreferencesModel getReadOnlyDirectoryPreferences()
    {
        return new ReadOnlyDirectoryPreferencesModel()
        {
            final ReadOnlyDirectoryPreferencesModel baseDirs = base.getReadOnlyDirectoryPreferences();
            final ReadOnlyDirectoryPreferencesModel overrideDirs = override.getReadOnlyDirectoryPreferences();

            @Override
            public Path getPreviewImagesDirectory()
            {
                return nullableOverride(baseDirs.getPreviewImagesDirectory(), overrideDirs.getPreviewImagesDirectory());
            }

            @Override
            public Path getLogFileDirectory()
            {
                return nullableOverride(baseDirs.getLogFileDirectory(), overrideDirs.getLogFileDirectory());
            }

            @Override
            public Path getCacheDirectory()
            {
                return nullableOverride(baseDirs.getCacheDirectory(), overrideDirs.getCacheDirectory());
            }

            @Override
            public Path getMasksDirectory() {
                return nullableOverride(baseDirs.getMasksDirectory(), overrideDirs.getMasksDirectory());
            }

            @Override
            public Path getPreferencesFileLocation()
            {
                return nullableOverride(baseDirs.getPreferencesFileLocation(), overrideDirs.getPreferencesFileLocation());
            }
        };
    }

    @Override
    public ReadonlySettingsModel getReadOnlySettings()
    {
        return new ReadonlySettingsModel() //TODO
        {
            @Override
            public Object getObject(String name)
            {
                return null;
            }

            @Override
            public <T> T get(String name, Class<T> settingType)
            {
                return null;
            }

            @Override
            public Class<?> getType(String name)
            {
                return null;
            }

            @Override
            public boolean exists(String name)
            {
                return false;
            }

            @Override
            public boolean shouldSerialize(String name)
            {
                return false;
            }

            @Override
            public Iterator<Setting> iterator()
            {
                return null;
            }
        };
    }
}
