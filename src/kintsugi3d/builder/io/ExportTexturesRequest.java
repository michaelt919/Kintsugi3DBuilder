package kintsugi3d.builder.io;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ObservableProjectGraphicsRequest;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.state.GeneralSettingsModel;
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
