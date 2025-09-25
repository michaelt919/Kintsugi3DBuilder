package kintsugi3d.builder.rendering;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.core.UserCancellationException;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.builder.state.cards.TabsManager;

class BackendProgressMonitor implements ProgressMonitor
{
    private final ProjectInstance<?> instance;
    private final ProgressMonitor base;

    public BackendProgressMonitor(ProjectInstance<?> instance, ProgressMonitor base)
    {
        this.instance = instance;
        this.base = base;
    }

    @Override
    public void allowUserCancellation() throws UserCancellationException
    {
        if (base != null)
        {
            base.allowUserCancellation();
        }
    }

    @Override
    public void cancelComplete(UserCancellationException e)
    {
        Global.state().getTabModels().clearTabs(); // Loading cancelled; clear the tabs of the sidebar

        if (base != null)
        {
            base.cancelComplete(e);
        }
    }

    @Override
    public void start()
    {
        if (base != null)
        {
            base.start();
        }
    }

    @Override
    public void setProcessName(String processName)
    {
        if (base != null)
        {
            base.setProcessName(processName);
        }
    }

    @Override
    public void setStageCount(int count)
    {
        if (base != null)
        {
            // Add one for the preview image generation step already completed.
            base.setStageCount(count + 1);
        }
    }

    @Override
    public void setStage(int stage, String message)
    {
        if (base != null)
        {
            // Add one for the preview image generation step already completed.
            base.setStage(stage + 1, message);
        }
    }

    @Override
    public void advanceStage(String message)
    {
        if (base != null)
        {
            // Add one for the preview image generation step already completed.
            base.advanceStage(message);
        }
    }

    @Override
    public void setMaxProgress(double maxProgress)
    {
        if (base != null)
        {
            base.setMaxProgress(maxProgress);
        }
    }

    @Override
    public void setProgress(double progress, String message)
    {
        if (base != null)
        {
            base.setProgress(progress, message);
        }
    }

    @Override
    public void complete()
    {
        instance.getResources().calibrateLightIntensities();
        instance.reloadShaders();

        Global.state().getProjectModel().setProjectLoaded(true);
        Global.state().getProjectModel().setProjectProcessed(isProcessed());
        Global.state().getProjectModel().setModelSize(instance.getActiveGeometry().getBoundingBoxSize());

        if (isProcessed())
        {
            BasisWeightResources<?> basisWeightResources =
                instance.getResources().getSpecularMaterialResources().getBasisWeightResources();

            Global.state().getProjectModel().setProcessedTextureResolution(basisWeightResources.weightMaps.getWidth());
        }
        else
        {
            Global.state().getProjectModel().setProcessedTextureResolution(0);
        }

        // Refresh tabs for materials
        new TabsManager(instance).refreshTab("Materials");

        if (base != null)
        {
            base.complete();
        }
    }

    private boolean isProcessed()
    {
        return instance.getResources().getSpecularMaterialResources().getBasisWeightResources() != null;
    }

    @Override
    public void fail(Throwable e)
    {
        if (base != null)
        {
            base.fail(e);
        }
    }

    @Override
    public void warn(Throwable e)
    {
        if (base != null)
        {
            base.warn(e);
        }
    }

    @Override
    public boolean isConflictingProcess()
    {
        if (base == null)
        {
            return false;
        }
        return base.isConflictingProcess();
    }
}
