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

package kintsugi3d.builder.io;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ObservableProjectGraphicsRequest;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import kintsugi3d.builder.util.Kintsugi3DViewerLauncher;
import kintsugi3d.gl.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ExportTexturesRequest implements ObservableProjectGraphicsRequest
{
    private static final Logger LOG = LoggerFactory.getLogger(ExportTexturesRequest.class);

    private final File exportLocationFile;
    private final ExportSettings settings;
    private final Runnable callback;

    public ExportTexturesRequest(File exportLocationFile, ExportSettings settings, Runnable callback)
    {
        this.exportLocationFile = exportLocationFile;
        this.settings = settings;
        this.callback = callback;
    }

    public ExportTexturesRequest(File exportLocationFile, Runnable callback)
    {
        this(exportLocationFile, getExportSettingsFromProject(), callback);
    }

    private static ExportSettings getExportSettingsFromProject()
    {
        GeneralSettingsModel projectSettings = Global.state().getIOModel()
            .validateHandler()
            .getLoadedViewSet().getProjectSettings();

        ExportSettings exportSettings = new ExportSettings();

        // Settings that are not managed by project
        exportSettings.setShouldSaveTextures(true);
        exportSettings.setShouldAppendModelNameToTextures(true); // Give textures better filenames for export

        // Settings affected by modal and stored in project
        exportSettings.setShouldGenerateLowResTextures(projectSettings.getBoolean("exportLODEnabled"));
        exportSettings.setMinimumTextureResolution(projectSettings.getInt("minimumLODSize"));
        exportSettings.setShouldOpenViewerOnceComplete(projectSettings.getBoolean("openViewerOnExportComplete"));
        exportSettings.setTextureFormat(projectSettings.get("textureFormat", String.class));

        return exportSettings;
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(
        ProjectInstance<ContextType> renderable, ProgressMonitor monitor)
    {
        if (settings.shouldSaveModel())
        {
            // Includes textures is shouldSaveTextures is true
            renderable.saveGLTF(exportLocationFile.getParentFile(), exportLocationFile.getName(), settings, this::onSaveComplete);
        }
        else if (settings.shouldSaveTextures())
        {
            renderable.getResources().getSpecularMaterialResources()
                .saveEssential(settings.getTextureFormat(), exportLocationFile.getParentFile());
        }
    }

    private void onSaveComplete()
    {
        if (settings.shouldOpenViewerOnceComplete())
        {
            try
            {
                Kintsugi3DViewerLauncher.launchViewer(exportLocationFile);
            }
            catch (IOException e)
            {
                LOG.error("Error launching Kintsugi 3D Viewer", e);
            }
            callback.run();
        }
    }
}
